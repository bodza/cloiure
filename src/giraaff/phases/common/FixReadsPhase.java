package giraaff.phases.common;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.TriState;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.BlockMap;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeMap;
import giraaff.graph.NodeStack;
import giraaff.graph.Position;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BinaryOpLogicNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.UnaryOpLogicNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.BinaryNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.UnaryNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.extended.IntegerSwitchNode;
import giraaff.nodes.memory.FixedAccessNode;
import giraaff.nodes.memory.FloatingAccessNode;
import giraaff.nodes.memory.FloatingReadNode;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryPhiNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.BasePhase;
import giraaff.phases.Phase;
import giraaff.phases.graph.ScheduledNodeIterator;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.LowTierContext;
import giraaff.phases.tiers.PhaseContext;

///
// This phase lowers {@link FloatingReadNode FloatingReadNodes} into corresponding fixed reads.
///
// @class FixReadsPhase
public final class FixReadsPhase extends BasePhase<LowTierContext>
{
    // @field
    protected boolean ___replaceInputsWithConstants;
    // @field
    protected Phase ___schedulePhase;

    // @class FixReadsPhase.FixReadsClosure
    private static final class FixReadsClosure extends ScheduledNodeIterator
    {
        @Override
        protected void processNode(Node __node)
        {
            if (__node instanceof AbstractMergeNode)
            {
                AbstractMergeNode __mergeNode = (AbstractMergeNode) __node;
                for (MemoryPhiNode __memoryPhi : __mergeNode.memoryPhis().snapshot())
                {
                    // Memory phi nodes are no longer necessary at this point.
                    __memoryPhi.replaceAtUsages(null);
                    __memoryPhi.safeDelete();
                }
            }
            else if (__node instanceof FloatingAccessNode)
            {
                FloatingAccessNode __floatingAccessNode = (FloatingAccessNode) __node;
                __floatingAccessNode.setLastLocationAccess(null);
                FixedAccessNode __fixedAccess = __floatingAccessNode.asFixedNode();
                replaceCurrent(__fixedAccess);
            }
            else if (__node instanceof PiNode)
            {
                PiNode __piNode = (PiNode) __node;
                if (__piNode.stamp(NodeView.DEFAULT).isCompatible(__piNode.getOriginalNode().stamp(NodeView.DEFAULT)))
                {
                    // Pi nodes are no longer necessary at this point.
                    __piNode.replaceAndDelete(__piNode.getOriginalNode());
                }
            }
            else if (__node instanceof MemoryAccess)
            {
                MemoryAccess __memoryAccess = (MemoryAccess) __node;
                __memoryAccess.setLastLocationAccess(null);
            }
        }
    }

    // @class FixReadsPhase.RawConditionalEliminationVisitor
    protected static final class RawConditionalEliminationVisitor implements ControlFlowGraph.RecursiveVisitor<Integer>
    {
        // @field
        protected final NodeMap<FixReadsPhase.StampElement> ___stampMap;
        // @field
        protected final NodeStack ___undoOperations;
        // @field
        private final StructuredGraph.ScheduleResult ___schedule;
        // @field
        private final StructuredGraph ___graph;
        // @field
        private final MetaAccessProvider ___metaAccess;
        // @field
        private final boolean ___replaceConstantInputs;
        // @field
        private final BlockMap<Integer> ___blockActionStart;
        // @field
        private final EconomicMap<MergeNode, EconomicMap<ValueNode, Stamp>> ___endMaps;

        // @cons FixReadsPhase.RawConditionalEliminationVisitor
        protected RawConditionalEliminationVisitor(StructuredGraph __graph, StructuredGraph.ScheduleResult __schedule, MetaAccessProvider __metaAccess, boolean __replaceInputsWithConstants)
        {
            super();
            this.___graph = __graph;
            this.___schedule = __schedule;
            this.___metaAccess = __metaAccess;
            this.___blockActionStart = new BlockMap<>(__schedule.getCFG());
            this.___endMaps = EconomicMap.create();
            this.___stampMap = __graph.createNodeMap();
            this.___undoOperations = new NodeStack();
            this.___replaceConstantInputs = __replaceInputsWithConstants && GraalOptions.replaceInputsWithConstantsBasedOnStamps;
        }

        protected void replaceInput(Position __p, Node __oldInput, Node __newConstantInput)
        {
            __p.set(__oldInput, __newConstantInput);
        }

        protected int replaceConstantInputs(Node __node)
        {
            int __replacements = 0;
            // Check if we can replace any of the inputs with a constant.
            for (Position __p : __node.inputPositions())
            {
                Node __input = __p.get(__node);
                if (__p.getInputType() == InputType.Value)
                {
                    if (__input instanceof ValueNode)
                    {
                        ValueNode __valueNode = (ValueNode) __input;
                        if (__valueNode instanceof ConstantNode)
                        {
                            // Input already is a constant.
                        }
                        else
                        {
                            Stamp __bestStamp = getBestStamp(__valueNode);
                            Constant __constant = __bestStamp.asConstant();
                            if (__constant != null)
                            {
                                ConstantNode __stampConstant = ConstantNode.forConstant(__bestStamp, __constant, this.___metaAccess, this.___graph);
                                replaceInput(__p, __node, __stampConstant);
                                __replacements++;
                            }
                        }
                    }
                }
            }
            return __replacements;
        }

        protected void processNode(Node __node)
        {
            if (this.___replaceConstantInputs)
            {
                replaceConstantInputs(__node);
            }

            if (__node instanceof MergeNode)
            {
                registerCombinedStamps((MergeNode) __node);
            }

            if (__node instanceof AbstractBeginNode)
            {
                processAbstractBegin((AbstractBeginNode) __node);
            }
            else if (__node instanceof IfNode)
            {
                processIf((IfNode) __node);
            }
            else if (__node instanceof IntegerSwitchNode)
            {
                processIntegerSwitch((IntegerSwitchNode) __node);
            }
            else if (__node instanceof BinaryNode)
            {
                processBinary((BinaryNode) __node);
            }
            else if (__node instanceof ConditionalNode)
            {
                processConditional((ConditionalNode) __node);
            }
            else if (__node instanceof UnaryNode)
            {
                processUnary((UnaryNode) __node);
            }
            else if (__node instanceof EndNode)
            {
                processEnd((EndNode) __node);
            }
        }

        protected void registerCombinedStamps(MergeNode __node)
        {
            EconomicMap<ValueNode, Stamp> __endMap = this.___endMaps.get(__node);
            MapCursor<ValueNode, Stamp> __entries = __endMap.getEntries();
            while (__entries.advance())
            {
                ValueNode __value = __entries.getKey();
                if (__value.isDeleted())
                {
                    // nodes from this map can be deleted when a loop dies
                    continue;
                }
                registerNewValueStamp(__value, __entries.getValue());
            }
        }

        protected void processEnd(EndNode __node)
        {
            AbstractMergeNode __abstractMerge = __node.merge();
            if (__abstractMerge instanceof MergeNode)
            {
                MergeNode __merge = (MergeNode) __abstractMerge;

                NodeMap<Block> __blockToNodeMap = this.___schedule.getNodeToBlockMap();
                Block __mergeBlock = __blockToNodeMap.get(__merge);
                Block __mergeBlockDominator = __mergeBlock.getDominator();
                Block __currentBlock = __blockToNodeMap.get(__node);

                EconomicMap<ValueNode, Stamp> __currentEndMap = this.___endMaps.get(__merge);

                if (__currentEndMap == null || !__currentEndMap.isEmpty())
                {
                    EconomicMap<ValueNode, Stamp> __endMap = EconomicMap.create();

                    // process phis
                    for (ValuePhiNode __phi : __merge.valuePhis())
                    {
                        if (__currentEndMap == null || __currentEndMap.containsKey(__phi))
                        {
                            ValueNode __valueAt = __phi.valueAt(__node);
                            Stamp __bestStamp = getBestStamp(__valueAt);

                            if (__currentEndMap != null)
                            {
                                __bestStamp = __bestStamp.meet(__currentEndMap.get(__phi));
                            }

                            if (!__bestStamp.equals(__phi.stamp(NodeView.DEFAULT)))
                            {
                                __endMap.put(__phi, __bestStamp);
                            }
                        }
                    }

                    int __lastMark = this.___undoOperations.size();
                    while (__currentBlock != __mergeBlockDominator)
                    {
                        int __mark = this.___blockActionStart.get(__currentBlock);
                        for (int __i = __lastMark - 1; __i >= __mark; --__i)
                        {
                            ValueNode __nodeWithNewStamp = (ValueNode) this.___undoOperations.get(__i);

                            if (__nodeWithNewStamp.isDeleted() || __nodeWithNewStamp instanceof LogicNode || __nodeWithNewStamp instanceof ConstantNode || __blockToNodeMap.isNew(__nodeWithNewStamp))
                            {
                                continue;
                            }

                            Block __block = getBlock(__nodeWithNewStamp, __blockToNodeMap);
                            if (__block == null || __block.getId() <= __mergeBlockDominator.getId())
                            {
                                // Node with new stamp in path to the merge block dominator and that
                                // at the same time was defined at least in the merge block dominator
                                // (i.e. therefore can be used after the merge.)

                                Stamp __bestStamp = getBestStamp(__nodeWithNewStamp);

                                if (__currentEndMap != null)
                                {
                                    Stamp __otherEndsStamp = __currentEndMap.get(__nodeWithNewStamp);
                                    if (__otherEndsStamp == null)
                                    {
                                        // No stamp registered in one of the previously processed ends => skip.
                                        continue;
                                    }
                                    __bestStamp = __bestStamp.meet(__otherEndsStamp);
                                }

                                if (__nodeWithNewStamp.stamp(NodeView.DEFAULT).tryImproveWith(__bestStamp) == null)
                                {
                                    // No point in registering the stamp.
                                }
                                else
                                {
                                    __endMap.put(__nodeWithNewStamp, __bestStamp);
                                }
                            }
                        }
                        __currentBlock = __currentBlock.getDominator();
                    }

                    this.___endMaps.put(__merge, __endMap);
                }
            }
        }

        private static Block getBlock(ValueNode __node, NodeMap<Block> __blockToNodeMap)
        {
            if (__node instanceof PhiNode)
            {
                PhiNode __phiNode = (PhiNode) __node;
                return __blockToNodeMap.get(__phiNode.merge());
            }
            return __blockToNodeMap.get(__node);
        }

        protected void processUnary(UnaryNode __node)
        {
            Stamp __newStamp = __node.foldStamp(getBestStamp(__node.getValue()));
            if (!checkReplaceWithConstant(__newStamp, __node))
            {
                registerNewValueStamp(__node, __newStamp);
            }
        }

        protected boolean checkReplaceWithConstant(Stamp __newStamp, ValueNode __node)
        {
            Constant __constant = __newStamp.asConstant();
            if (__constant != null && !(__node instanceof ConstantNode))
            {
                ConstantNode __stampConstant = ConstantNode.forConstant(__newStamp, __constant, this.___metaAccess, this.___graph);
                __node.replaceAtUsages(InputType.Value, __stampConstant);
                GraphUtil.tryKillUnused(__node);
                return true;
            }
            return false;
        }

        protected void processBinary(BinaryNode __node)
        {
            Stamp __xStamp = getBestStamp(__node.getX());
            Stamp __yStamp = getBestStamp(__node.getY());
            Stamp __newStamp = __node.foldStamp(__xStamp, __yStamp);
            if (!checkReplaceWithConstant(__newStamp, __node))
            {
                registerNewValueStamp(__node, __newStamp);
            }
        }

        protected void processIntegerSwitch(IntegerSwitchNode __node)
        {
            Stamp __bestStamp = getBestStamp(__node.value());
            __node.tryRemoveUnreachableKeys(null, __bestStamp);
        }

        protected void processIf(IfNode __node)
        {
            TriState __result = tryProveCondition(__node.condition());
            if (__result != TriState.UNKNOWN)
            {
                boolean __isTrue = (__result == TriState.TRUE);
                AbstractBeginNode __survivingSuccessor = __node.getSuccessor(__isTrue);
                __survivingSuccessor.replaceAtUsages(null);
                __survivingSuccessor.replaceAtPredecessor(null);
                __node.replaceAtPredecessor(__survivingSuccessor);
                GraphUtil.killCFG(__node);
            }
        }

        protected void processConditional(ConditionalNode __node)
        {
            TriState __result = tryProveCondition(__node.condition());
            if (__result != TriState.UNKNOWN)
            {
                boolean __isTrue = (__result == TriState.TRUE);
                __node.replaceAndDelete(__isTrue ? __node.trueValue() : __node.falseValue());
            }
            else
            {
                Stamp __trueStamp = getBestStamp(__node.trueValue());
                Stamp __falseStamp = getBestStamp(__node.falseValue());
                registerNewStamp(__node, __trueStamp.meet(__falseStamp));
            }
        }

        protected TriState tryProveCondition(LogicNode __condition)
        {
            Stamp __conditionStamp = this.getBestStamp(__condition);
            if (__conditionStamp == StampFactory.tautology())
            {
                return TriState.TRUE;
            }
            else if (__conditionStamp == StampFactory.contradiction())
            {
                return TriState.FALSE;
            }

            if (__condition instanceof UnaryOpLogicNode)
            {
                UnaryOpLogicNode __unaryOpLogicNode = (UnaryOpLogicNode) __condition;
                return __unaryOpLogicNode.tryFold(this.getBestStamp(__unaryOpLogicNode.getValue()));
            }
            else if (__condition instanceof BinaryOpLogicNode)
            {
                BinaryOpLogicNode __binaryOpLogicNode = (BinaryOpLogicNode) __condition;
                return __binaryOpLogicNode.tryFold(this.getBestStamp(__binaryOpLogicNode.getX()), this.getBestStamp(__binaryOpLogicNode.getY()));
            }

            return TriState.UNKNOWN;
        }

        protected void processAbstractBegin(AbstractBeginNode __beginNode)
        {
            Node __predecessor = __beginNode.predecessor();
            if (__predecessor instanceof IfNode)
            {
                IfNode __ifNode = (IfNode) __predecessor;
                boolean __negated = (__ifNode.falseSuccessor() == __beginNode);
                LogicNode __condition = __ifNode.condition();
                registerNewCondition(__condition, __negated);
            }
            else if (__predecessor instanceof IntegerSwitchNode)
            {
                IntegerSwitchNode __integerSwitchNode = (IntegerSwitchNode) __predecessor;
                registerIntegerSwitch(__beginNode, __integerSwitchNode);
            }
        }

        private void registerIntegerSwitch(AbstractBeginNode __beginNode, IntegerSwitchNode __integerSwitchNode)
        {
            registerNewValueStamp(__integerSwitchNode.value(), __integerSwitchNode.getValueStampForSuccessor(__beginNode));
        }

        protected void registerNewCondition(LogicNode __condition, boolean __negated)
        {
            if (__condition instanceof UnaryOpLogicNode)
            {
                UnaryOpLogicNode __unaryLogicNode = (UnaryOpLogicNode) __condition;
                ValueNode __value = __unaryLogicNode.getValue();
                Stamp __newStamp = __unaryLogicNode.getSucceedingStampForValue(__negated);
                registerNewValueStamp(__value, __newStamp);
            }
            else if (__condition instanceof BinaryOpLogicNode)
            {
                BinaryOpLogicNode __binaryOpLogicNode = (BinaryOpLogicNode) __condition;
                ValueNode __x = __binaryOpLogicNode.getX();
                ValueNode __y = __binaryOpLogicNode.getY();
                Stamp __xStamp = getBestStamp(__x);
                Stamp __yStamp = getBestStamp(__y);
                registerNewValueStamp(__x, __binaryOpLogicNode.getSucceedingStampForX(__negated, __xStamp, __yStamp));
                registerNewValueStamp(__y, __binaryOpLogicNode.getSucceedingStampForY(__negated, __xStamp, __yStamp));
            }
            registerCondition(__condition, __negated);
        }

        protected void registerCondition(LogicNode __condition, boolean __negated)
        {
            registerNewStamp(__condition, __negated ? StampFactory.contradiction() : StampFactory.tautology());
        }

        protected boolean registerNewValueStamp(ValueNode __value, Stamp __newStamp)
        {
            if (__newStamp != null && !__value.isConstant())
            {
                Stamp __currentStamp = getBestStamp(__value);
                Stamp __betterStamp = __currentStamp.tryImproveWith(__newStamp);
                if (__betterStamp != null)
                {
                    registerNewStamp(__value, __betterStamp);
                    return true;
                }
            }
            return false;
        }

        protected void registerNewStamp(ValueNode __value, Stamp __newStamp)
        {
            ValueNode __originalNode = __value;
            this.___stampMap.setAndGrow(__originalNode, new FixReadsPhase.StampElement(__newStamp, this.___stampMap.getAndGrow(__originalNode)));
            this.___undoOperations.push(__originalNode);
        }

        protected Stamp getBestStamp(ValueNode __value)
        {
            ValueNode __originalNode = __value;
            FixReadsPhase.StampElement __currentStamp = this.___stampMap.getAndGrow(__originalNode);
            if (__currentStamp == null)
            {
                return __value.stamp(NodeView.DEFAULT);
            }
            return __currentStamp.getStamp();
        }

        @Override
        public Integer enter(Block __b)
        {
            int __mark = this.___undoOperations.size();
            this.___blockActionStart.put(__b, __mark);
            for (Node __n : this.___schedule.getBlockToNodesMap().get(__b))
            {
                if (__n.isAlive())
                {
                    processNode(__n);
                }
            }
            return __mark;
        }

        @Override
        public void exit(Block __b, Integer __state)
        {
            int __mark = __state;
            while (this.___undoOperations.size() > __mark)
            {
                Node __node = this.___undoOperations.pop();
                if (__node.isAlive())
                {
                    this.___stampMap.set(__node, this.___stampMap.get(__node).getParent());
                }
            }
        }
    }

    // @cons FixReadsPhase
    public FixReadsPhase(boolean __replaceInputsWithConstants, Phase __schedulePhase)
    {
        super();
        this.___replaceInputsWithConstants = __replaceInputsWithConstants;
        this.___schedulePhase = __schedulePhase;
    }

    @Override
    protected void run(StructuredGraph __graph, LowTierContext __context)
    {
        this.___schedulePhase.apply(__graph);
        StructuredGraph.ScheduleResult __schedule = __graph.getLastSchedule();
        FixReadsPhase.FixReadsClosure __fixReadsClosure = new FixReadsPhase.FixReadsClosure();
        for (Block __block : __schedule.getCFG().getBlocks())
        {
            __fixReadsClosure.processNodes(__block, __schedule);
        }
        if (GraalOptions.rawConditionalElimination)
        {
            __schedule.getCFG().visitDominatorTree(createVisitor(__graph, __schedule, __context), false);
        }
        __graph.setAfterFixReadPhase(true);
    }

    // @class FixReadsPhase.RawCEPhase
    public static final class RawCEPhase extends BasePhase<LowTierContext>
    {
        // @field
        private final boolean ___replaceInputsWithConstants;

        // @cons FixReadsPhase.RawCEPhase
        public RawCEPhase(boolean __replaceInputsWithConstants)
        {
            super();
            this.___replaceInputsWithConstants = __replaceInputsWithConstants;
        }

        @Override
        protected CharSequence getName()
        {
            return "RawCEPhase";
        }

        @Override
        protected void run(StructuredGraph __graph, LowTierContext __context)
        {
            if (GraalOptions.rawConditionalElimination)
            {
                SchedulePhase __schedulePhase = new SchedulePhase(SchedulePhase.SchedulingStrategy.LATEST, true);
                __schedulePhase.apply(__graph);
                StructuredGraph.ScheduleResult __schedule = __graph.getLastSchedule();
                __schedule.getCFG().visitDominatorTree(new FixReadsPhase.RawConditionalEliminationVisitor(__graph, __schedule, __context.getMetaAccess(), this.___replaceInputsWithConstants), false);
            }
        }
    }

    protected ControlFlowGraph.RecursiveVisitor<?> createVisitor(StructuredGraph __graph, StructuredGraph.ScheduleResult __schedule, PhaseContext __context)
    {
        return new FixReadsPhase.RawConditionalEliminationVisitor(__graph, __schedule, __context.getMetaAccess(), this.___replaceInputsWithConstants);
    }

    // @class FixReadsPhase.StampElement
    protected static final class StampElement
    {
        // @field
        private final Stamp ___stamp;
        // @field
        private final FixReadsPhase.StampElement ___parent;

        // @cons FixReadsPhase.StampElement
        public StampElement(Stamp __stamp, FixReadsPhase.StampElement __parent)
        {
            super();
            this.___stamp = __stamp;
            this.___parent = __parent;
        }

        public FixReadsPhase.StampElement getParent()
        {
            return this.___parent;
        }

        public Stamp getStamp()
        {
            return this.___stamp;
        }
    }

    public void setReplaceInputsWithConstants(boolean __replaceInputsWithConstants)
    {
        this.___replaceInputsWithConstants = __replaceInputsWithConstants;
    }
}
