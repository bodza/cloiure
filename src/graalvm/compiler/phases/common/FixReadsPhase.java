package graalvm.compiler.phases.common;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.TriState;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.core.common.type.FloatStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeMap;
import graalvm.compiler.graph.NodeStack;
import graalvm.compiler.graph.Position;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.BinaryOpLogicNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.EndNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.MergeNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.UnaryOpLogicNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.ValuePhiNode;
import graalvm.compiler.nodes.calc.BinaryNode;
import graalvm.compiler.nodes.calc.ConditionalNode;
import graalvm.compiler.nodes.calc.UnaryNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.cfg.ControlFlowGraph.RecursiveVisitor;
import graalvm.compiler.nodes.extended.IntegerSwitchNode;
import graalvm.compiler.nodes.memory.FixedAccessNode;
import graalvm.compiler.nodes.memory.FloatingAccessNode;
import graalvm.compiler.nodes.memory.FloatingReadNode;
import graalvm.compiler.nodes.memory.MemoryAccess;
import graalvm.compiler.nodes.memory.MemoryPhiNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.Phase;
import graalvm.compiler.phases.graph.ScheduledNodeIterator;
import graalvm.compiler.phases.schedule.SchedulePhase;
import graalvm.compiler.phases.schedule.SchedulePhase.SchedulingStrategy;
import graalvm.compiler.phases.tiers.LowTierContext;
import graalvm.compiler.phases.tiers.PhaseContext;

/**
 * This phase lowers {@link FloatingReadNode FloatingReadNodes} into corresponding fixed reads.
 */
public class FixReadsPhase extends BasePhase<LowTierContext>
{
    protected boolean replaceInputsWithConstants;
    protected Phase schedulePhase;

    private static class FixReadsClosure extends ScheduledNodeIterator
    {
        @Override
        protected void processNode(Node node)
        {
            if (node instanceof AbstractMergeNode)
            {
                AbstractMergeNode mergeNode = (AbstractMergeNode) node;
                for (MemoryPhiNode memoryPhi : mergeNode.memoryPhis().snapshot())
                {
                    // Memory phi nodes are no longer necessary at this point.
                    memoryPhi.replaceAtUsages(null);
                    memoryPhi.safeDelete();
                }
            }
            else if (node instanceof FloatingAccessNode)
            {
                FloatingAccessNode floatingAccessNode = (FloatingAccessNode) node;
                floatingAccessNode.setLastLocationAccess(null);
                FixedAccessNode fixedAccess = floatingAccessNode.asFixedNode();
                replaceCurrent(fixedAccess);
            }
            else if (node instanceof PiNode)
            {
                PiNode piNode = (PiNode) node;
                if (piNode.stamp(NodeView.DEFAULT).isCompatible(piNode.getOriginalNode().stamp(NodeView.DEFAULT)))
                {
                    // Pi nodes are no longer necessary at this point.
                    piNode.replaceAndDelete(piNode.getOriginalNode());
                }
            }
            else if (node instanceof MemoryAccess)
            {
                MemoryAccess memoryAccess = (MemoryAccess) node;
                memoryAccess.setLastLocationAccess(null);
            }
        }
    }

    protected static class RawConditionalEliminationVisitor implements RecursiveVisitor<Integer>
    {
        protected final NodeMap<StampElement> stampMap;
        protected final NodeStack undoOperations;
        private final ScheduleResult schedule;
        private final StructuredGraph graph;
        private final MetaAccessProvider metaAccess;
        private final boolean replaceConstantInputs;
        private final BlockMap<Integer> blockActionStart;
        private final EconomicMap<MergeNode, EconomicMap<ValueNode, Stamp>> endMaps;

        protected RawConditionalEliminationVisitor(StructuredGraph graph, ScheduleResult schedule, MetaAccessProvider metaAccess, boolean replaceInputsWithConstants)
        {
            this.graph = graph;
            this.schedule = schedule;
            this.metaAccess = metaAccess;
            blockActionStart = new BlockMap<>(schedule.getCFG());
            endMaps = EconomicMap.create();
            stampMap = graph.createNodeMap();
            undoOperations = new NodeStack();
            replaceConstantInputs = replaceInputsWithConstants && GraalOptions.ReplaceInputsWithConstantsBasedOnStamps.getValue(graph.getOptions());
        }

        protected void replaceInput(Position p, Node oldInput, Node newConstantInput)
        {
            p.set(oldInput, newConstantInput);
        }

        protected int replaceConstantInputs(Node node)
        {
            int replacements = 0;
            // Check if we can replace any of the inputs with a constant.
            for (Position p : node.inputPositions())
            {
                Node input = p.get(node);
                if (p.getInputType() == InputType.Value)
                {
                    if (input instanceof ValueNode)
                    {
                        ValueNode valueNode = (ValueNode) input;
                        if (valueNode instanceof ConstantNode)
                        {
                            // Input already is a constant.
                        }
                        else
                        {
                            Stamp bestStamp = getBestStamp(valueNode);
                            Constant constant = bestStamp.asConstant();
                            if (constant != null)
                            {
                                if (bestStamp instanceof FloatStamp)
                                {
                                    FloatStamp floatStamp = (FloatStamp) bestStamp;
                                    if (floatStamp.contains(0.0d))
                                    {
                                        // Could also be -0.0d.
                                        continue;
                                    }
                                }
                                ConstantNode stampConstant = ConstantNode.forConstant(bestStamp, constant, metaAccess, graph);
                                replaceInput(p, node, stampConstant);
                                replacements++;
                            }
                        }
                    }
                }
            }
            return replacements;
        }

        protected void processNode(Node node)
        {
            if (replaceConstantInputs)
            {
                replaceConstantInputs(node);
            }

            if (node instanceof MergeNode)
            {
                registerCombinedStamps((MergeNode) node);
            }

            if (node instanceof AbstractBeginNode)
            {
                processAbstractBegin((AbstractBeginNode) node);
            }
            else if (node instanceof IfNode)
            {
                processIf((IfNode) node);
            }
            else if (node instanceof IntegerSwitchNode)
            {
                processIntegerSwitch((IntegerSwitchNode) node);
            }
            else if (node instanceof BinaryNode)
            {
                processBinary((BinaryNode) node);
            }
            else if (node instanceof ConditionalNode)
            {
                processConditional((ConditionalNode) node);
            }
            else if (node instanceof UnaryNode)
            {
                processUnary((UnaryNode) node);
            }
            else if (node instanceof EndNode)
            {
                processEnd((EndNode) node);
            }
        }

        protected void registerCombinedStamps(MergeNode node)
        {
            EconomicMap<ValueNode, Stamp> endMap = endMaps.get(node);
            MapCursor<ValueNode, Stamp> entries = endMap.getEntries();
            while (entries.advance())
            {
                ValueNode value = entries.getKey();
                if (value.isDeleted())
                {
                    // nodes from this map can be deleted when a loop dies
                    continue;
                }
                registerNewValueStamp(value, entries.getValue());
            }
        }

        protected void processEnd(EndNode node)
        {
            AbstractMergeNode abstractMerge = node.merge();
            if (abstractMerge instanceof MergeNode)
            {
                MergeNode merge = (MergeNode) abstractMerge;

                NodeMap<Block> blockToNodeMap = this.schedule.getNodeToBlockMap();
                Block mergeBlock = blockToNodeMap.get(merge);
                Block mergeBlockDominator = mergeBlock.getDominator();
                Block currentBlock = blockToNodeMap.get(node);

                EconomicMap<ValueNode, Stamp> currentEndMap = endMaps.get(merge);

                if (currentEndMap == null || !currentEndMap.isEmpty())
                {
                    EconomicMap<ValueNode, Stamp> endMap = EconomicMap.create();

                    // Process phis
                    for (ValuePhiNode phi : merge.valuePhis())
                    {
                        if (currentEndMap == null || currentEndMap.containsKey(phi))
                        {
                            ValueNode valueAt = phi.valueAt(node);
                            Stamp bestStamp = getBestStamp(valueAt);

                            if (currentEndMap != null)
                            {
                                bestStamp = bestStamp.meet(currentEndMap.get(phi));
                            }

                            if (!bestStamp.equals(phi.stamp(NodeView.DEFAULT)))
                            {
                                endMap.put(phi, bestStamp);
                            }
                        }
                    }

                    int lastMark = undoOperations.size();
                    while (currentBlock != mergeBlockDominator)
                    {
                        int mark = blockActionStart.get(currentBlock);
                        for (int i = lastMark - 1; i >= mark; --i)
                        {
                            ValueNode nodeWithNewStamp = (ValueNode) undoOperations.get(i);

                            if (nodeWithNewStamp.isDeleted() || nodeWithNewStamp instanceof LogicNode || nodeWithNewStamp instanceof ConstantNode || blockToNodeMap.isNew(nodeWithNewStamp))
                            {
                                continue;
                            }

                            Block block = getBlock(nodeWithNewStamp, blockToNodeMap);
                            if (block == null || block.getId() <= mergeBlockDominator.getId())
                            {
                                // Node with new stamp in path to the merge block dominator and that
                                // at the same time was defined at least in the merge block
                                // dominator (i.e., therefore can be used after the merge.)

                                Stamp bestStamp = getBestStamp(nodeWithNewStamp);

                                if (currentEndMap != null)
                                {
                                    Stamp otherEndsStamp = currentEndMap.get(nodeWithNewStamp);
                                    if (otherEndsStamp == null)
                                    {
                                        // No stamp registered in one of the previously processed
                                        // ends => skip.
                                        continue;
                                    }
                                    bestStamp = bestStamp.meet(otherEndsStamp);
                                }

                                if (nodeWithNewStamp.stamp(NodeView.DEFAULT).tryImproveWith(bestStamp) == null)
                                {
                                    // No point in registering the stamp.
                                }
                                else
                                {
                                    endMap.put(nodeWithNewStamp, bestStamp);
                                }
                            }
                        }
                        currentBlock = currentBlock.getDominator();
                    }

                    endMaps.put(merge, endMap);
                }
            }
        }

        private static Block getBlock(ValueNode node, NodeMap<Block> blockToNodeMap)
        {
            if (node instanceof PhiNode)
            {
                PhiNode phiNode = (PhiNode) node;
                return blockToNodeMap.get(phiNode.merge());
            }
            return blockToNodeMap.get(node);
        }

        protected void processUnary(UnaryNode node)
        {
            Stamp newStamp = node.foldStamp(getBestStamp(node.getValue()));
            if (!checkReplaceWithConstant(newStamp, node))
            {
                registerNewValueStamp(node, newStamp);
            }
        }

        protected boolean checkReplaceWithConstant(Stamp newStamp, ValueNode node)
        {
            Constant constant = newStamp.asConstant();
            if (constant != null && !(node instanceof ConstantNode))
            {
                ConstantNode stampConstant = ConstantNode.forConstant(newStamp, constant, metaAccess, graph);
                node.replaceAtUsages(InputType.Value, stampConstant);
                GraphUtil.tryKillUnused(node);
                return true;
            }
            return false;
        }

        protected void processBinary(BinaryNode node)
        {
            Stamp xStamp = getBestStamp(node.getX());
            Stamp yStamp = getBestStamp(node.getY());
            Stamp newStamp = node.foldStamp(xStamp, yStamp);
            if (!checkReplaceWithConstant(newStamp, node))
            {
                registerNewValueStamp(node, newStamp);
            }
        }

        protected void processIntegerSwitch(IntegerSwitchNode node)
        {
            Stamp bestStamp = getBestStamp(node.value());
            node.tryRemoveUnreachableKeys(null, bestStamp);
        }

        protected void processIf(IfNode node)
        {
            TriState result = tryProveCondition(node.condition());
            if (result != TriState.UNKNOWN)
            {
                boolean isTrue = (result == TriState.TRUE);
                AbstractBeginNode survivingSuccessor = node.getSuccessor(isTrue);
                survivingSuccessor.replaceAtUsages(null);
                survivingSuccessor.replaceAtPredecessor(null);
                node.replaceAtPredecessor(survivingSuccessor);
                GraphUtil.killCFG(node);
            }
        }

        protected void processConditional(ConditionalNode node)
        {
            TriState result = tryProveCondition(node.condition());
            if (result != TriState.UNKNOWN)
            {
                boolean isTrue = (result == TriState.TRUE);
                node.replaceAndDelete(isTrue ? node.trueValue() : node.falseValue());
            }
            else
            {
                Stamp trueStamp = getBestStamp(node.trueValue());
                Stamp falseStamp = getBestStamp(node.falseValue());
                registerNewStamp(node, trueStamp.meet(falseStamp));
            }
        }

        protected TriState tryProveCondition(LogicNode condition)
        {
            Stamp conditionStamp = this.getBestStamp(condition);
            if (conditionStamp == StampFactory.tautology())
            {
                return TriState.TRUE;
            }
            else if (conditionStamp == StampFactory.contradiction())
            {
                return TriState.FALSE;
            }

            if (condition instanceof UnaryOpLogicNode)
            {
                UnaryOpLogicNode unaryOpLogicNode = (UnaryOpLogicNode) condition;
                return unaryOpLogicNode.tryFold(this.getBestStamp(unaryOpLogicNode.getValue()));
            }
            else if (condition instanceof BinaryOpLogicNode)
            {
                BinaryOpLogicNode binaryOpLogicNode = (BinaryOpLogicNode) condition;
                return binaryOpLogicNode.tryFold(this.getBestStamp(binaryOpLogicNode.getX()), this.getBestStamp(binaryOpLogicNode.getY()));
            }

            return TriState.UNKNOWN;
        }

        protected void processAbstractBegin(AbstractBeginNode beginNode)
        {
            Node predecessor = beginNode.predecessor();
            if (predecessor instanceof IfNode)
            {
                IfNode ifNode = (IfNode) predecessor;
                boolean negated = (ifNode.falseSuccessor() == beginNode);
                LogicNode condition = ifNode.condition();
                registerNewCondition(condition, negated);
            }
            else if (predecessor instanceof IntegerSwitchNode)
            {
                IntegerSwitchNode integerSwitchNode = (IntegerSwitchNode) predecessor;
                registerIntegerSwitch(beginNode, integerSwitchNode);
            }
        }

        private void registerIntegerSwitch(AbstractBeginNode beginNode, IntegerSwitchNode integerSwitchNode)
        {
            registerNewValueStamp(integerSwitchNode.value(), integerSwitchNode.getValueStampForSuccessor(beginNode));
        }

        protected void registerNewCondition(LogicNode condition, boolean negated)
        {
            if (condition instanceof UnaryOpLogicNode)
            {
                UnaryOpLogicNode unaryLogicNode = (UnaryOpLogicNode) condition;
                ValueNode value = unaryLogicNode.getValue();
                Stamp newStamp = unaryLogicNode.getSucceedingStampForValue(negated);
                registerNewValueStamp(value, newStamp);
            }
            else if (condition instanceof BinaryOpLogicNode)
            {
                BinaryOpLogicNode binaryOpLogicNode = (BinaryOpLogicNode) condition;
                ValueNode x = binaryOpLogicNode.getX();
                ValueNode y = binaryOpLogicNode.getY();
                Stamp xStamp = getBestStamp(x);
                Stamp yStamp = getBestStamp(y);
                registerNewValueStamp(x, binaryOpLogicNode.getSucceedingStampForX(negated, xStamp, yStamp));
                registerNewValueStamp(y, binaryOpLogicNode.getSucceedingStampForY(negated, xStamp, yStamp));
            }
            registerCondition(condition, negated);
        }

        protected void registerCondition(LogicNode condition, boolean negated)
        {
            registerNewStamp(condition, negated ? StampFactory.contradiction() : StampFactory.tautology());
        }

        protected boolean registerNewValueStamp(ValueNode value, Stamp newStamp)
        {
            if (newStamp != null && !value.isConstant())
            {
                Stamp currentStamp = getBestStamp(value);
                Stamp betterStamp = currentStamp.tryImproveWith(newStamp);
                if (betterStamp != null)
                {
                    registerNewStamp(value, betterStamp);
                    return true;
                }
            }
            return false;
        }

        protected void registerNewStamp(ValueNode value, Stamp newStamp)
        {
            ValueNode originalNode = value;
            stampMap.setAndGrow(originalNode, new StampElement(newStamp, stampMap.getAndGrow(originalNode)));
            undoOperations.push(originalNode);
        }

        protected Stamp getBestStamp(ValueNode value)
        {
            ValueNode originalNode = value;
            StampElement currentStamp = stampMap.getAndGrow(originalNode);
            if (currentStamp == null)
            {
                return value.stamp(NodeView.DEFAULT);
            }
            return currentStamp.getStamp();
        }

        @Override
        public Integer enter(Block b)
        {
            int mark = undoOperations.size();
            blockActionStart.put(b, mark);
            for (Node n : schedule.getBlockToNodesMap().get(b))
            {
                if (n.isAlive())
                {
                    processNode(n);
                }
            }
            return mark;
        }

        @Override
        public void exit(Block b, Integer state)
        {
            int mark = state;
            while (undoOperations.size() > mark)
            {
                Node node = undoOperations.pop();
                if (node.isAlive())
                {
                    stampMap.set(node, stampMap.get(node).getParent());
                }
            }
        }
    }

    public FixReadsPhase(boolean replaceInputsWithConstants, Phase schedulePhase)
    {
        this.replaceInputsWithConstants = replaceInputsWithConstants;
        this.schedulePhase = schedulePhase;
    }

    @Override
    protected void run(StructuredGraph graph, LowTierContext context)
    {
        schedulePhase.apply(graph);
        ScheduleResult schedule = graph.getLastSchedule();
        FixReadsClosure fixReadsClosure = new FixReadsClosure();
        for (Block block : schedule.getCFG().getBlocks())
        {
            fixReadsClosure.processNodes(block, schedule);
        }
        if (GraalOptions.RawConditionalElimination.getValue(graph.getOptions()))
        {
            schedule.getCFG().visitDominatorTree(createVisitor(graph, schedule, context), false);
        }
        graph.setAfterFixReadPhase(true);
    }

    public static class RawCEPhase extends BasePhase<LowTierContext>
    {
        private final boolean replaceInputsWithConstants;

        public RawCEPhase(boolean replaceInputsWithConstants)
        {
            this.replaceInputsWithConstants = replaceInputsWithConstants;
        }

        @Override
        protected CharSequence getName()
        {
            return "RawCEPhase";
        }

        @Override
        protected void run(StructuredGraph graph, LowTierContext context)
        {
            if (GraalOptions.RawConditionalElimination.getValue(graph.getOptions()))
            {
                SchedulePhase schedulePhase = new SchedulePhase(SchedulingStrategy.LATEST, true);
                schedulePhase.apply(graph);
                ScheduleResult schedule = graph.getLastSchedule();
                schedule.getCFG().visitDominatorTree(new RawConditionalEliminationVisitor(graph, schedule, context.getMetaAccess(), replaceInputsWithConstants), false);
            }
        }
    }

    protected ControlFlowGraph.RecursiveVisitor<?> createVisitor(StructuredGraph graph, ScheduleResult schedule, PhaseContext context)
    {
        return new RawConditionalEliminationVisitor(graph, schedule, context.getMetaAccess(), replaceInputsWithConstants);
    }

    protected static final class StampElement
    {
        private final Stamp stamp;
        private final StampElement parent;

        public StampElement(Stamp stamp, StampElement parent)
        {
            this.stamp = stamp;
            this.parent = parent;
        }

        public StampElement getParent()
        {
            return parent;
        }

        public Stamp getStamp()
        {
            return stamp;
        }

        @Override
        public String toString()
        {
            StringBuilder result = new StringBuilder();
            result.append(stamp);
            if (this.parent != null)
            {
                result.append(" (");
                result.append(this.parent.toString());
                result.append(")");
            }
            return result.toString();
        }
    }

    public void setReplaceInputsWithConstants(boolean replaceInputsWithConstants)
    {
        this.replaceInputsWithConstants = replaceInputsWithConstants;
    }
}
