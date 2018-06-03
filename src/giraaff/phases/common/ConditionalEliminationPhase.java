package giraaff.phases.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.TriState;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;
import org.graalvm.collections.Pair;

import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.core.common.cfg.BlockMap;
import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.And;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Or;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeMap;
import giraaff.graph.NodeStack;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BinaryOpLogicNode;
import giraaff.nodes.ConditionAnchorNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.DeoptimizingGuard;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.ShortCircuitOrNode;
import giraaff.nodes.StaticDeoptimizingNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.UnaryOpLogicNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.AndNode;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.nodes.calc.BinaryNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.UnaryNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.extended.IntegerSwitchNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.extended.ValueAnchorNode;
import giraaff.nodes.java.TypeSwitchNode;
import giraaff.nodes.spi.NodeWithState;
import giraaff.nodes.spi.StampInverter;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.BasePhase;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.schedule.SchedulePhase.SchedulingStrategy;
import giraaff.phases.tiers.PhaseContext;

// @class ConditionalEliminationPhase
public final class ConditionalEliminationPhase extends BasePhase<PhaseContext>
{
    // @field
    private final boolean ___fullSchedule;
    // @field
    private final boolean ___moveGuards;

    // @cons
    public ConditionalEliminationPhase(boolean __fullSchedule)
    {
        this(__fullSchedule, true);
    }

    // @cons
    public ConditionalEliminationPhase(boolean __fullSchedule, boolean __moveGuards)
    {
        super();
        this.___fullSchedule = __fullSchedule;
        this.___moveGuards = __moveGuards;
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        BlockMap<List<Node>> __blockToNodes = null;
        NodeMap<Block> __nodeToBlock = null;
        ControlFlowGraph __cfg = ControlFlowGraph.compute(__graph, true, true, true, true);
        if (this.___fullSchedule)
        {
            if (this.___moveGuards)
            {
                __cfg.visitDominatorTree(new MoveGuardsUpwards(), __graph.hasValueProxies());
            }
            SchedulePhase.run(__graph, SchedulingStrategy.EARLIEST_WITH_GUARD_ORDER, __cfg);
            ScheduleResult __r = __graph.getLastSchedule();
            __blockToNodes = __r.getBlockToNodesMap();
            __nodeToBlock = __r.getNodeToBlockMap();
        }
        else
        {
            __nodeToBlock = __cfg.getNodeToBlock();
            __blockToNodes = getBlockToNodes(__cfg);
        }
        ControlFlowGraph.RecursiveVisitor<?> __visitor = createVisitor(__graph, __cfg, __blockToNodes, __nodeToBlock, __context);
        __cfg.visitDominatorTree(__visitor, __graph.hasValueProxies());
    }

    protected BlockMap<List<Node>> getBlockToNodes(@SuppressWarnings("unused") ControlFlowGraph __cfg)
    {
        return null;
    }

    protected ControlFlowGraph.RecursiveVisitor<?> createVisitor(StructuredGraph __graph, @SuppressWarnings("unused") ControlFlowGraph __cfg, BlockMap<List<Node>> __blockToNodes, NodeMap<Block> __nodeToBlock, PhaseContext __context)
    {
        return new Instance(__graph, __blockToNodes, __nodeToBlock, __context);
    }

    // @class ConditionalEliminationPhase.MoveGuardsUpwards
    public static final class MoveGuardsUpwards implements ControlFlowGraph.RecursiveVisitor<Block>
    {
        // @field
        Block ___anchorBlock;

        @Override
        public Block enter(Block __b)
        {
            Block __oldAnchorBlock = this.___anchorBlock;
            if (__b.getDominator() == null || __b.getDominator().getPostdominator() != __b)
            {
                // New anchor.
                this.___anchorBlock = __b;
            }

            AbstractBeginNode __beginNode = __b.getBeginNode();
            if (__beginNode instanceof AbstractMergeNode && this.___anchorBlock != __b)
            {
                AbstractMergeNode __mergeNode = (AbstractMergeNode) __beginNode;
                for (GuardNode __guard : __mergeNode.guards().snapshot())
                {
                    GuardNode __newlyCreatedGuard = new GuardNode(__guard.getCondition(), this.___anchorBlock.getBeginNode(), __guard.getReason(), __guard.getAction(), __guard.isNegated(), __guard.getSpeculation());
                    GuardNode __newGuard = __mergeNode.graph().unique(__newlyCreatedGuard);
                    __guard.replaceAndDelete(__newGuard);
                }
            }

            FixedNode __endNode = __b.getEndNode();
            if (__endNode instanceof IfNode)
            {
                IfNode __node = (IfNode) __endNode;

                // Check if we can move guards upwards.
                AbstractBeginNode __trueSuccessor = __node.trueSuccessor();
                EconomicMap<LogicNode, GuardNode> __trueGuards = EconomicMap.create(Equivalence.IDENTITY);
                for (GuardNode __guard : __trueSuccessor.guards())
                {
                    LogicNode __condition = __guard.getCondition();
                    if (__condition.hasMoreThanOneUsage())
                    {
                        __trueGuards.put(__condition, __guard);
                    }
                }

                if (!__trueGuards.isEmpty())
                {
                    for (GuardNode __guard : __node.falseSuccessor().guards().snapshot())
                    {
                        GuardNode __otherGuard = __trueGuards.get(__guard.getCondition());
                        if (__otherGuard != null && __guard.isNegated() == __otherGuard.isNegated())
                        {
                            JavaConstant __speculation = __otherGuard.getSpeculation();
                            if (__speculation == null)
                            {
                                __speculation = __guard.getSpeculation();
                            }
                            else if (__guard.getSpeculation() != null && __guard.getSpeculation() != __speculation)
                            {
                                // Cannot optimize due to different speculations.
                                continue;
                            }
                            GuardNode __newlyCreatedGuard = new GuardNode(__guard.getCondition(), this.___anchorBlock.getBeginNode(), __guard.getReason(), __guard.getAction(), __guard.isNegated(), __speculation);
                            GuardNode __newGuard = __node.graph().unique(__newlyCreatedGuard);
                            if (__otherGuard.isAlive())
                            {
                                __otherGuard.replaceAndDelete(__newGuard);
                            }
                            __guard.replaceAndDelete(__newGuard);
                        }
                    }
                }
            }
            return __oldAnchorBlock;
        }

        @Override
        public void exit(Block __b, Block __value)
        {
            this.___anchorBlock = __value;
        }
    }

    // @class ConditionalEliminationPhase.PhiInfoElement
    private static final class PhiInfoElement
    {
        // @field
        private EconomicMap<EndNode, InfoElement> ___infoElements;

        public void set(EndNode __end, InfoElement __infoElement)
        {
            if (this.___infoElements == null)
            {
                this.___infoElements = EconomicMap.create(Equivalence.IDENTITY);
            }
            this.___infoElements.put(__end, __infoElement);
        }

        public InfoElement get(EndNode __end)
        {
            if (this.___infoElements == null)
            {
                return null;
            }
            return this.___infoElements.get(__end);
        }
    }

    // @class ConditionalEliminationPhase.Instance
    public static final class Instance implements ControlFlowGraph.RecursiveVisitor<Integer>
    {
        // @field
        protected final NodeMap<InfoElement> ___map;
        // @field
        protected final BlockMap<List<Node>> ___blockToNodes;
        // @field
        protected final NodeMap<Block> ___nodeToBlock;
        // @field
        protected final CanonicalizerTool ___tool;
        // @field
        protected final NodeStack ___undoOperations;
        // @field
        protected final StructuredGraph ___graph;
        // @field
        protected final EconomicMap<MergeNode, EconomicMap<ValuePhiNode, PhiInfoElement>> ___mergeMaps;

        ///
        // Tests which may be eliminated because post dominating tests to prove a broader condition.
        ///
        // @field
        private Deque<DeoptimizingGuard> ___pendingTests;

        // @cons
        public Instance(StructuredGraph __graph, BlockMap<List<Node>> __blockToNodes, NodeMap<Block> __nodeToBlock, PhaseContext __context)
        {
            super();
            this.___graph = __graph;
            this.___blockToNodes = __blockToNodes;
            this.___nodeToBlock = __nodeToBlock;
            this.___undoOperations = new NodeStack();
            this.___map = __graph.createNodeMap();
            this.___pendingTests = new ArrayDeque<>();
            this.___tool = GraphUtil.getDefaultSimplifier(__context.getMetaAccess(), __context.getConstantReflection(), __context.getConstantFieldProvider(), false, __graph.getAssumptions(), __context.getLowerer());
            this.___mergeMaps = EconomicMap.create();
        }

        protected void processConditionAnchor(ConditionAnchorNode __node)
        {
            tryProveCondition(__node.condition(), (__guard, __result, __guardedValueStamp, __newInput) ->
            {
                if (__result != __node.isNegated())
                {
                    __node.replaceAtUsages(__guard.asNode());
                    GraphUtil.unlinkFixedNode(__node);
                    GraphUtil.killWithUnusedFloatingInputs(__node);
                }
                else
                {
                    ValueAnchorNode __valueAnchor = __node.graph().add(new ValueAnchorNode(null));
                    __node.replaceAtUsages(__valueAnchor);
                    __node.graph().replaceFixedWithFixed(__node, __valueAnchor);
                }
                return true;
            });
        }

        protected void processGuard(GuardNode __node)
        {
            if (!tryProveGuardCondition(__node, __node.getCondition(), (__guard, __result, __guardedValueStamp, __newInput) ->
            {
                if (__result != __node.isNegated())
                {
                    __node.replaceAndDelete(__guard.asNode());
                }
                else
                {
                    DeoptimizeNode __deopt = __node.graph().add(new DeoptimizeNode(__node.getAction(), __node.getReason(), __node.getSpeculation()));
                    AbstractBeginNode __beginNode = (AbstractBeginNode) __node.getAnchor();
                    FixedNode __next = __beginNode.next();
                    __beginNode.setNext(__deopt);
                    GraphUtil.killCFG(__next);
                }
                return true;
            }))
            {
                registerNewCondition(__node.getCondition(), __node.isNegated(), __node);
            }
        }

        protected void processFixedGuard(FixedGuardNode __node)
        {
            if (!tryProveGuardCondition(__node, __node.condition(), (__guard, __result, __guardedValueStamp, __newInput) ->
            {
                if (__result != __node.isNegated())
                {
                    __node.replaceAtUsages(__guard.asNode());
                    GraphUtil.unlinkFixedNode(__node);
                    GraphUtil.killWithUnusedFloatingInputs(__node);
                }
                else
                {
                    DeoptimizeNode __deopt = __node.graph().add(new DeoptimizeNode(__node.getAction(), __node.getReason(), __node.getSpeculation()));
                    __deopt.setStateBefore(__node.stateBefore());
                    __node.replaceAtPredecessor(__deopt);
                    GraphUtil.killCFG(__node);
                }
                return true;
            }))
            {
                registerNewCondition(__node.condition(), __node.isNegated(), __node);
            }
        }

        protected void processIf(IfNode __node)
        {
            tryProveCondition(__node.condition(), (__guard, __result, __guardedValueStamp, __newInput) ->
            {
                AbstractBeginNode __survivingSuccessor = __node.getSuccessor(__result);
                __survivingSuccessor.replaceAtUsages(InputType.Guard, __guard.asNode());
                __survivingSuccessor.replaceAtPredecessor(null);
                __node.replaceAtPredecessor(__survivingSuccessor);
                GraphUtil.killCFG(__node);
                return true;
            });
        }

        @Override
        public Integer enter(Block __block)
        {
            int __mark = this.___undoOperations.size();
            // For now conservatively collect guards only within the same block.
            this.___pendingTests.clear();
            processNodes(__block);
            return __mark;
        }

        protected void processNodes(Block __block)
        {
            if (this.___blockToNodes != null)
            {
                for (Node __n : this.___blockToNodes.get(__block))
                {
                    if (__n.isAlive())
                    {
                        processNode(__n);
                    }
                }
            }
            else
            {
                processBlock(__block);
            }
        }

        private void processBlock(Block __block)
        {
            FixedNode __n = __block.getBeginNode();
            FixedNode __endNode = __block.getEndNode();
            while (__n != __endNode)
            {
                if (__n.isDeleted() || __endNode.isDeleted())
                {
                    // This branch was deleted!
                    return;
                }
                FixedNode __next = ((FixedWithNextNode) __n).next();
                processNode(__n);
                __n = __next;
            }
            if (__endNode.isAlive())
            {
                processNode(__endNode);
            }
        }

        protected void processNode(Node __node)
        {
            if (__node instanceof NodeWithState && !(__node instanceof GuardingNode))
            {
                this.___pendingTests.clear();
            }

            if (__node instanceof MergeNode)
            {
                introducePisForPhis((MergeNode) __node);
            }

            if (__node instanceof AbstractBeginNode)
            {
                if (__node instanceof LoopExitNode && this.___graph.hasValueProxies())
                {
                    // Condition must not be used down this path.
                    return;
                }
                processAbstractBegin((AbstractBeginNode) __node);
            }
            else if (__node instanceof FixedGuardNode)
            {
                processFixedGuard((FixedGuardNode) __node);
            }
            else if (__node instanceof GuardNode)
            {
                processGuard((GuardNode) __node);
            }
            else if (__node instanceof ConditionAnchorNode)
            {
                processConditionAnchor((ConditionAnchorNode) __node);
            }
            else if (__node instanceof IfNode)
            {
                processIf((IfNode) __node);
            }
            else if (__node instanceof EndNode)
            {
                processEnd((EndNode) __node);
            }
        }

        protected void introducePisForPhis(MergeNode __merge)
        {
            EconomicMap<ValuePhiNode, PhiInfoElement> __mergeMap = this.___mergeMaps.get(__merge);
            if (__mergeMap != null)
            {
                MapCursor<ValuePhiNode, PhiInfoElement> __entries = __mergeMap.getEntries();
                while (__entries.advance())
                {
                    ValuePhiNode __phi = __entries.getKey();
                    // Phi might have been killed already via a conditional elimination in another branch.
                    if (__phi.isDeleted())
                    {
                        continue;
                    }
                    PhiInfoElement __phiInfoElements = __entries.getValue();
                    Stamp __bestPossibleStamp = null;
                    for (int __i = 0; __i < __phi.valueCount(); ++__i)
                    {
                        ValueNode __valueAt = __phi.valueAt(__i);
                        Stamp __curBestStamp = __valueAt.stamp(NodeView.DEFAULT);
                        InfoElement __infoElement = __phiInfoElements.get(__merge.forwardEndAt(__i));
                        if (__infoElement != null)
                        {
                            __curBestStamp = __curBestStamp.join(__infoElement.getStamp());
                        }

                        if (__bestPossibleStamp == null)
                        {
                            __bestPossibleStamp = __curBestStamp;
                        }
                        else
                        {
                            __bestPossibleStamp = __bestPossibleStamp.meet(__curBestStamp);
                        }
                    }

                    Stamp __oldStamp = __phi.stamp(NodeView.DEFAULT);
                    if (__oldStamp.tryImproveWith(__bestPossibleStamp) != null)
                    {
                        // Need to be careful to not run into stamp update cycles with the iterative canonicalization.
                        boolean __allow = false;
                        if (__bestPossibleStamp instanceof ObjectStamp)
                        {
                            // Always allow object stamps.
                            __allow = true;
                        }
                        else if (__bestPossibleStamp instanceof IntegerStamp)
                        {
                            IntegerStamp __integerStamp = (IntegerStamp) __bestPossibleStamp;
                            IntegerStamp __oldIntegerStamp = (IntegerStamp) __oldStamp;
                            if (__integerStamp.isPositive() != __oldIntegerStamp.isPositive())
                            {
                                __allow = true;
                            }
                            else if (__integerStamp.isNegative() != __oldIntegerStamp.isNegative())
                            {
                                __allow = true;
                            }
                            else if (__integerStamp.isStrictlyPositive() != __oldIntegerStamp.isStrictlyPositive())
                            {
                                __allow = true;
                            }
                            else if (__integerStamp.isStrictlyNegative() != __oldIntegerStamp.isStrictlyNegative())
                            {
                                __allow = true;
                            }
                            else if (__integerStamp.asConstant() != null)
                            {
                                __allow = true;
                            }
                            else if (__oldStamp.isUnrestricted())
                            {
                                __allow = true;
                            }
                        }
                        else
                        {
                            __allow = (__bestPossibleStamp.asConstant() != null);
                        }

                        if (__allow)
                        {
                            ValuePhiNode __newPhi = this.___graph.addWithoutUnique(new ValuePhiNode(__bestPossibleStamp, __merge));
                            for (int __i = 0; __i < __phi.valueCount(); ++__i)
                            {
                                ValueNode __valueAt = __phi.valueAt(__i);
                                if (__bestPossibleStamp.meet(__valueAt.stamp(NodeView.DEFAULT)).equals(__bestPossibleStamp))
                                {
                                    // Pi not required here.
                                }
                                else
                                {
                                    InfoElement __infoElement = __phiInfoElements.get(__merge.forwardEndAt(__i));
                                    Stamp __curBestStamp = __infoElement.getStamp();
                                    ValueNode __input = __infoElement.getProxifiedInput();
                                    if (__input == null)
                                    {
                                        __input = __valueAt;
                                    }
                                    ValueNode __valueNode = this.___graph.maybeAddOrUnique(PiNode.create(__input, __curBestStamp, (ValueNode) __infoElement.___guard));
                                    __valueAt = __valueNode;
                                }
                                __newPhi.addInput(__valueAt);
                            }
                            __phi.replaceAtUsagesAndDelete(__newPhi);
                        }
                    }
                }
            }
        }

        protected void processEnd(EndNode __end)
        {
            AbstractMergeNode __abstractMerge = __end.merge();
            if (__abstractMerge instanceof MergeNode)
            {
                MergeNode __merge = (MergeNode) __abstractMerge;

                EconomicMap<ValuePhiNode, PhiInfoElement> __mergeMap = this.___mergeMaps.get(__merge);
                for (ValuePhiNode __phi : __merge.valuePhis())
                {
                    ValueNode __valueAt = __phi.valueAt(__end);
                    InfoElement __infoElement = this.getInfoElements(__valueAt);
                    while (__infoElement != null)
                    {
                        Stamp __newStamp = __infoElement.getStamp();
                        if (__phi.stamp(NodeView.DEFAULT).tryImproveWith(__newStamp) != null)
                        {
                            if (__mergeMap == null)
                            {
                                __mergeMap = EconomicMap.create();
                                this.___mergeMaps.put(__merge, __mergeMap);
                            }

                            PhiInfoElement __phiInfoElement = __mergeMap.get(__phi);
                            if (__phiInfoElement == null)
                            {
                                __phiInfoElement = new PhiInfoElement();
                                __mergeMap.put(__phi, __phiInfoElement);
                            }

                            __phiInfoElement.set(__end, __infoElement);
                            break;
                        }
                        __infoElement = nextElement(__infoElement);
                    }
                }
            }
        }

        protected void registerNewCondition(LogicNode __condition, boolean __negated, GuardingNode __guard)
        {
            if (__condition instanceof UnaryOpLogicNode)
            {
                UnaryOpLogicNode __unaryLogicNode = (UnaryOpLogicNode) __condition;
                ValueNode __value = __unaryLogicNode.getValue();
                if (maybeMultipleUsages(__value))
                {
                    // getSucceedingStampForValue doesn't take the (potentially a Pi Node) input
                    // stamp into account, so it can be safely propagated.
                    Stamp __newStamp = __unaryLogicNode.getSucceedingStampForValue(__negated);
                    registerNewStamp(__value, __newStamp, __guard, true);
                }
            }
            else if (__condition instanceof BinaryOpLogicNode)
            {
                BinaryOpLogicNode __binaryOpLogicNode = (BinaryOpLogicNode) __condition;
                ValueNode __x = __binaryOpLogicNode.getX();
                ValueNode __y = __binaryOpLogicNode.getY();
                if (!__x.isConstant() && maybeMultipleUsages(__x))
                {
                    Stamp __newStampX = __binaryOpLogicNode.getSucceedingStampForX(__negated, getSafeStamp(__x), getOtherSafeStamp(__y));
                    registerNewStamp(__x, __newStampX, __guard);
                }

                if (!__y.isConstant() && maybeMultipleUsages(__y))
                {
                    Stamp __newStampY = __binaryOpLogicNode.getSucceedingStampForY(__negated, getOtherSafeStamp(__x), getSafeStamp(__y));
                    registerNewStamp(__y, __newStampY, __guard);
                }

                if (__condition instanceof IntegerEqualsNode && __guard instanceof DeoptimizingGuard && !__negated)
                {
                    if (__y.isConstant() && __x instanceof AndNode)
                    {
                        AndNode __and = (AndNode) __x;
                        ValueNode __andX = __and.getX();
                        if (__and.getY() == __y && maybeMultipleUsages(__andX))
                        {
                            // This 'and' proves something about some of the bits in and.getX().
                            // It's equivalent to or'ing in the mask value since those values are known to be set.
                            BinaryOp<Or> __op = ArithmeticOpTable.forStamp(__x.stamp(NodeView.DEFAULT)).getOr();
                            IntegerStamp __newStampX = (IntegerStamp) __op.foldStamp(getSafeStamp(__andX), getOtherSafeStamp(__y));
                            registerNewStamp(__andX, __newStampX, __guard);
                        }
                    }
                }
            }
            if (__guard instanceof DeoptimizingGuard)
            {
                this.___pendingTests.push((DeoptimizingGuard) __guard);
            }
            registerCondition(__condition, __negated, __guard);
        }

        Pair<InfoElement, Stamp> recursiveFoldStamp(Node __node)
        {
            if (__node instanceof UnaryNode)
            {
                UnaryNode __unary = (UnaryNode) __node;
                ValueNode __value = __unary.getValue();
                InfoElement __infoElement = getInfoElements(__value);
                while (__infoElement != null)
                {
                    Stamp __result = __unary.foldStamp(__infoElement.getStamp());
                    if (__result != null)
                    {
                        return Pair.create(__infoElement, __result);
                    }
                    __infoElement = nextElement(__infoElement);
                }
            }
            else if (__node instanceof BinaryNode)
            {
                BinaryNode __binary = (BinaryNode) __node;
                ValueNode __y = __binary.getY();
                ValueNode __x = __binary.getX();
                if (__y.isConstant())
                {
                    InfoElement __infoElement = getInfoElements(__x);
                    while (__infoElement != null)
                    {
                        Stamp __result = __binary.foldStamp(__infoElement.___stamp, __y.stamp(NodeView.DEFAULT));
                        if (__result != null)
                        {
                            return Pair.create(__infoElement, __result);
                        }
                        __infoElement = nextElement(__infoElement);
                    }
                }
            }
            return null;
        }

        ///
        // Get the stamp that may be used for the value for which we are registering the condition.
        // We may directly use the stamp here without restriction, because any later lookup of the
        // registered info elements is in the same chain of pi nodes.
        ///
        private static Stamp getSafeStamp(ValueNode __x)
        {
            return __x.stamp(NodeView.DEFAULT);
        }

        ///
        // We can only use the stamp of a second value involved in the condition if we are sure that
        // we are not implicitly creating a dependency on a pi node that is responsible for that
        // stamp. For now, we are conservatively only using the stamps of constants. Under certain
        // circumstances, we may also be able to use the stamp of the value after skipping pi nodes
        // (e.g. the stamp of a parameter after inlining, or the stamp of a fixed node that can
        // never be replaced with a pi node via canonicalization).
        ///
        private static Stamp getOtherSafeStamp(ValueNode __x)
        {
            if (__x.isConstant() || __x.graph().isAfterFixedReadPhase())
            {
                return __x.stamp(NodeView.DEFAULT);
            }
            return __x.stamp(NodeView.DEFAULT).unrestricted();
        }

        ///
        // Recursively try to fold stamps within this expression using information from
        // {@link #getInfoElements(ValueNode)}. It's only safe to use constants and one
        // {@link InfoElement} otherwise more than one guard would be required.
        //
        // @return the pair of the @{link InfoElement} used and the stamp produced for the whole expression
        ///
        Pair<InfoElement, Stamp> recursiveFoldStampFromInfo(Node __node)
        {
            return recursiveFoldStamp(__node);
        }

        ///
        // Look for a preceding guard whose condition is implied by {@code thisGuard}. If we find
        // one, try to move this guard just above that preceding guard so that we can fold it:
        //
        // <pre>
        //     guard(C1); // preceding guard
        //     ...
        //     guard(C2); // thisGuard
        // </pre>
        //
        // If C2 => C1, transform to:
        //
        // <pre>
        //     guard(C2);
        //     ...
        // </pre>
        ///
        protected boolean foldPendingTest(DeoptimizingGuard __thisGuard, ValueNode __original, Stamp __newStamp, GuardRewirer __rewireGuardFunction)
        {
            for (DeoptimizingGuard __pendingGuard : this.___pendingTests)
            {
                LogicNode __pendingCondition = __pendingGuard.getCondition();
                TriState __result = TriState.UNKNOWN;
                if (__pendingCondition instanceof UnaryOpLogicNode)
                {
                    UnaryOpLogicNode __unaryLogicNode = (UnaryOpLogicNode) __pendingCondition;
                    if (__unaryLogicNode.getValue() == __original)
                    {
                        __result = __unaryLogicNode.tryFold(__newStamp);
                    }
                }
                else if (__pendingCondition instanceof BinaryOpLogicNode)
                {
                    BinaryOpLogicNode __binaryOpLogicNode = (BinaryOpLogicNode) __pendingCondition;
                    ValueNode __x = __binaryOpLogicNode.getX();
                    ValueNode __y = __binaryOpLogicNode.getY();
                    if (__x == __original)
                    {
                        __result = __binaryOpLogicNode.tryFold(__newStamp, getOtherSafeStamp(__y));
                    }
                    else if (__y == __original)
                    {
                        __result = __binaryOpLogicNode.tryFold(getOtherSafeStamp(__x), __newStamp);
                    }
                    else if (__binaryOpLogicNode instanceof IntegerEqualsNode && __y.isConstant() && __x instanceof AndNode)
                    {
                        AndNode __and = (AndNode) __x;
                        if (__and.getY() == __y && __and.getX() == __original)
                        {
                            BinaryOp<And> __andOp = ArithmeticOpTable.forStamp(__newStamp).getAnd();
                            __result = __binaryOpLogicNode.tryFold(__andOp.foldStamp(__newStamp, getOtherSafeStamp(__y)), getOtherSafeStamp(__y));
                        }
                    }
                }
                if (__result.isKnown())
                {
                    // The test case be folded using the information available but the test can only
                    // be moved up if we're sure there's no schedule dependence.
                    if (canScheduleAbove(__thisGuard.getCondition(), __pendingGuard.asNode(), __original) && foldGuard(__thisGuard, __pendingGuard, __result.toBoolean(), __newStamp, __rewireGuardFunction))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean canScheduleAbove(Node __n, Node __target, ValueNode __knownToBeAbove)
        {
            Block __targetBlock = this.___nodeToBlock.get(__target);
            Block __testBlock = this.___nodeToBlock.get(__n);
            if (__targetBlock != null && __testBlock != null)
            {
                if (__targetBlock == __testBlock)
                {
                    for (Node __fixed : this.___blockToNodes.get(__targetBlock))
                    {
                        if (__fixed == __n)
                        {
                            return true;
                        }
                        else if (__fixed == __target)
                        {
                            break;
                        }
                    }
                }
                else if (AbstractControlFlowGraph.dominates(__testBlock, __targetBlock))
                {
                    return true;
                }
            }
            InputFilter __v = new InputFilter(__knownToBeAbove);
            __n.applyInputs(__v);
            return __v.___ok;
        }

        protected boolean foldGuard(DeoptimizingGuard __thisGuard, DeoptimizingGuard __otherGuard, boolean __outcome, Stamp __guardedValueStamp, GuardRewirer __rewireGuardFunction)
        {
            DeoptimizationAction __action = StaticDeoptimizingNode.mergeActions(__otherGuard.getAction(), __thisGuard.getAction());
            if (__action != null && __otherGuard.getSpeculation() == __thisGuard.getSpeculation())
            {
                LogicNode __condition = (LogicNode) __thisGuard.getCondition().copyWithInputs();
                // We have ...; guard(C1); guard(C2);...
                //
                // Where the first guard is 'otherGuard' and the second one 'thisGuard'.
                //
                // Depending on 'outcome', we have C2 => C1 or C2 => !C1.
                //
                // - If C2 => C1, 'mustDeopt' below is false and we transform to ...; guard(C2); ...
                //
                // - If C2 => !C1, 'mustDeopt' is true and we transform to ..; guard(C1); deopt;

                // for the second case, the action of the deopt is copied from there:
                __thisGuard.setAction(__action);
                GuardRewirer __rewirer = (__guard, __result, __innerGuardedValueStamp, __newInput) ->
                {
                    // 'result' is 'outcome', 'guard' is 'otherGuard'
                    boolean __mustDeopt = __result == __otherGuard.isNegated();
                    if (__rewireGuardFunction.rewire(__guard, __mustDeopt == __thisGuard.isNegated(), __innerGuardedValueStamp, __newInput))
                    {
                        if (!__mustDeopt)
                        {
                            __otherGuard.setCondition(__condition, __thisGuard.isNegated());
                            __otherGuard.setAction(__action);
                            __otherGuard.setReason(__thisGuard.getReason());
                        }
                        return true;
                    }
                    __condition.safeDelete();
                    return false;
                };
                // move the later test up
                return rewireGuards(__otherGuard, __outcome, null, __guardedValueStamp, __rewirer);
            }
            return false;
        }

        protected void registerCondition(LogicNode __condition, boolean __negated, GuardingNode __guard)
        {
            if (__condition.hasMoreThanOneUsage())
            {
                registerNewStamp(__condition, __negated ? StampFactory.contradiction() : StampFactory.tautology(), __guard);
            }
        }

        protected InfoElement getInfoElements(ValueNode __proxiedValue)
        {
            ValueNode __value = GraphUtil.skipPi(__proxiedValue);
            if (__value == null)
            {
                return null;
            }
            return this.___map.getAndGrow(__value);
        }

        protected boolean rewireGuards(GuardingNode __guard, boolean __result, ValueNode __proxifiedInput, Stamp __guardedValueStamp, GuardRewirer __rewireGuardFunction)
        {
            return __rewireGuardFunction.rewire(__guard, __result, __guardedValueStamp, __proxifiedInput);
        }

        protected boolean tryProveCondition(LogicNode __node, GuardRewirer __rewireGuardFunction)
        {
            return tryProveGuardCondition(null, __node, __rewireGuardFunction);
        }

        private InfoElement nextElement(InfoElement __current)
        {
            InfoElement __parent = __current.getParent();
            if (__parent != null)
            {
                return __parent;
            }
            else
            {
                ValueNode __proxifiedInput = __current.getProxifiedInput();
                if (__proxifiedInput instanceof PiNode)
                {
                    PiNode __piNode = (PiNode) __proxifiedInput;
                    return getInfoElements(__piNode.getOriginalNode());
                }
            }
            return null;
        }

        protected boolean tryProveGuardCondition(DeoptimizingGuard __thisGuard, LogicNode __node, GuardRewirer __rewireGuardFunction)
        {
            InfoElement __infoElement = getInfoElements(__node);
            while (__infoElement != null)
            {
                Stamp __stamp = __infoElement.getStamp();
                JavaConstant __constant = (JavaConstant) __stamp.asConstant();
                if (__constant != null)
                {
                    // No proxified input and stamp required.
                    return rewireGuards(__infoElement.getGuard(), __constant.asBoolean(), null, null, __rewireGuardFunction);
                }
                __infoElement = nextElement(__infoElement);
            }

            if (__node instanceof UnaryOpLogicNode)
            {
                UnaryOpLogicNode __unaryLogicNode = (UnaryOpLogicNode) __node;
                ValueNode __value = __unaryLogicNode.getValue();
                __infoElement = getInfoElements(__value);
                while (__infoElement != null)
                {
                    Stamp __stamp = __infoElement.getStamp();
                    TriState __result = __unaryLogicNode.tryFold(__stamp);
                    if (__result.isKnown())
                    {
                        return rewireGuards(__infoElement.getGuard(), __result.toBoolean(), __infoElement.getProxifiedInput(), __infoElement.getStamp(), __rewireGuardFunction);
                    }
                    __infoElement = nextElement(__infoElement);
                }
                Pair<InfoElement, Stamp> __foldResult = recursiveFoldStampFromInfo(__value);
                if (__foldResult != null)
                {
                    TriState __result = __unaryLogicNode.tryFold(__foldResult.getRight());
                    if (__result.isKnown())
                    {
                        return rewireGuards(__foldResult.getLeft().getGuard(), __result.toBoolean(), __foldResult.getLeft().getProxifiedInput(), __foldResult.getRight(), __rewireGuardFunction);
                    }
                }
                if (__thisGuard != null)
                {
                    Stamp __newStamp = __unaryLogicNode.getSucceedingStampForValue(__thisGuard.isNegated());
                    if (__newStamp != null && foldPendingTest(__thisGuard, __value, __newStamp, __rewireGuardFunction))
                    {
                        return true;
                    }
                }
            }
            else if (__node instanceof BinaryOpLogicNode)
            {
                BinaryOpLogicNode __binaryOpLogicNode = (BinaryOpLogicNode) __node;
                ValueNode __x = __binaryOpLogicNode.getX();
                ValueNode __y = __binaryOpLogicNode.getY();
                __infoElement = getInfoElements(__x);
                while (__infoElement != null)
                {
                    TriState __result = __binaryOpLogicNode.tryFold(__infoElement.getStamp(), __y.stamp(NodeView.DEFAULT));
                    if (__result.isKnown())
                    {
                        return rewireGuards(__infoElement.getGuard(), __result.toBoolean(), __infoElement.getProxifiedInput(), __infoElement.getStamp(), __rewireGuardFunction);
                    }
                    __infoElement = nextElement(__infoElement);
                }

                if (__y.isConstant())
                {
                    Pair<InfoElement, Stamp> __foldResult = recursiveFoldStampFromInfo(__x);
                    if (__foldResult != null)
                    {
                        TriState __result = __binaryOpLogicNode.tryFold(__foldResult.getRight(), __y.stamp(NodeView.DEFAULT));
                        if (__result.isKnown())
                        {
                            return rewireGuards(__foldResult.getLeft().getGuard(), __result.toBoolean(), __foldResult.getLeft().getProxifiedInput(), __foldResult.getRight(), __rewireGuardFunction);
                        }
                    }
                }
                else
                {
                    __infoElement = getInfoElements(__y);
                    while (__infoElement != null)
                    {
                        TriState __result = __binaryOpLogicNode.tryFold(__x.stamp(NodeView.DEFAULT), __infoElement.getStamp());
                        if (__result.isKnown())
                        {
                            return rewireGuards(__infoElement.getGuard(), __result.toBoolean(), __infoElement.getProxifiedInput(), __infoElement.getStamp(), __rewireGuardFunction);
                        }
                        __infoElement = nextElement(__infoElement);
                    }
                }

                // For complex expressions involving constants, see if it's possible to fold the
                // tests by using stamps one level up in the expression. For instance, (x + n < y)
                // might fold if something is known about x and all other values are constants. The
                // reason for the constant restriction is that if more than 1 real value is involved
                // the code might need to adopt multiple guards to have proper dependences.
                if (__x instanceof BinaryArithmeticNode<?> && __y.isConstant())
                {
                    BinaryArithmeticNode<?> __binary = (BinaryArithmeticNode<?>) __x;
                    if (__binary.getY().isConstant())
                    {
                        __infoElement = getInfoElements(__binary.getX());
                        while (__infoElement != null)
                        {
                            Stamp __newStampX = __binary.foldStamp(__infoElement.getStamp(), __binary.getY().stamp(NodeView.DEFAULT));
                            TriState __result = __binaryOpLogicNode.tryFold(__newStampX, __y.stamp(NodeView.DEFAULT));
                            if (__result.isKnown())
                            {
                                return rewireGuards(__infoElement.getGuard(), __result.toBoolean(), __infoElement.getProxifiedInput(), __newStampX, __rewireGuardFunction);
                            }
                            __infoElement = nextElement(__infoElement);
                        }
                    }
                }

                if (__thisGuard != null && __binaryOpLogicNode instanceof IntegerEqualsNode && !__thisGuard.isNegated())
                {
                    if (__y.isConstant() && __x instanceof AndNode)
                    {
                        AndNode __and = (AndNode) __x;
                        if (__and.getY() == __y)
                        {
                            // This 'and' proves something about some of the bits in and.getX().
                            // It's equivalent to or'ing in the mask value since those values are known to be set.
                            BinaryOp<Or> __op = ArithmeticOpTable.forStamp(__x.stamp(NodeView.DEFAULT)).getOr();
                            IntegerStamp __newStampX = (IntegerStamp) __op.foldStamp(getSafeStamp(__and.getX()), getOtherSafeStamp(__y));
                            if (foldPendingTest(__thisGuard, __and.getX(), __newStampX, __rewireGuardFunction))
                            {
                                return true;
                            }
                        }
                    }
                }

                if (__thisGuard != null)
                {
                    if (!__x.isConstant())
                    {
                        Stamp __newStampX = __binaryOpLogicNode.getSucceedingStampForX(__thisGuard.isNegated(), getSafeStamp(__x), getOtherSafeStamp(__y));
                        if (__newStampX != null && foldPendingTest(__thisGuard, __x, __newStampX, __rewireGuardFunction))
                        {
                            return true;
                        }
                    }
                    if (!__y.isConstant())
                    {
                        Stamp __newStampY = __binaryOpLogicNode.getSucceedingStampForY(__thisGuard.isNegated(), getOtherSafeStamp(__x), getSafeStamp(__y));
                        if (__newStampY != null && foldPendingTest(__thisGuard, __y, __newStampY, __rewireGuardFunction))
                        {
                            return true;
                        }
                    }
                }
            }
            else if (__node instanceof ShortCircuitOrNode)
            {
                final ShortCircuitOrNode __shortCircuitOrNode = (ShortCircuitOrNode) __node;
                return tryProveCondition(__shortCircuitOrNode.getX(), (__guard, __result, __guardedValueStamp, __newInput) ->
                {
                    if (__result == !__shortCircuitOrNode.isXNegated())
                    {
                        return rewireGuards(__guard, true, __newInput, __guardedValueStamp, __rewireGuardFunction);
                    }
                    else
                    {
                        return tryProveCondition(__shortCircuitOrNode.getY(), (__innerGuard, __innerResult, __innerGuardedValueStamp, __innerNewInput) ->
                        {
                            ValueNode __proxifiedInput = __newInput;
                            if (__proxifiedInput == null)
                            {
                                __proxifiedInput = __innerNewInput;
                            }
                            else if (__innerNewInput != null)
                            {
                                if (__innerNewInput != __newInput)
                                {
                                    // Cannot canonicalize due to different proxied inputs.
                                    return false;
                                }
                            }
                            // Can only canonicalize if the guards are equal.
                            if (__innerGuard == __guard)
                            {
                                return rewireGuards(__guard, __innerResult ^ __shortCircuitOrNode.isYNegated(), __proxifiedInput, __guardedValueStamp, __rewireGuardFunction);
                            }
                            return false;
                        });
                    }
                });
            }

            return false;
        }

        protected void registerNewStamp(ValueNode __maybeProxiedValue, Stamp __newStamp, GuardingNode __guard)
        {
            registerNewStamp(__maybeProxiedValue, __newStamp, __guard, false);
        }

        protected void registerNewStamp(ValueNode __maybeProxiedValue, Stamp __newStamp, GuardingNode __guard, boolean __propagateThroughPis)
        {
            if (__newStamp == null || __newStamp.isUnrestricted())
            {
                return;
            }

            ValueNode __value = __maybeProxiedValue;
            Stamp __stamp = __newStamp;

            while (__stamp != null && __value != null)
            {
                ValueNode __proxiedValue = null;
                if (__value instanceof PiNode)
                {
                    __proxiedValue = __value;
                }
                this.___map.setAndGrow(__value, new InfoElement(__stamp, __guard, __proxiedValue, this.___map.getAndGrow(__value)));
                this.___undoOperations.push(__value);
                if (__propagateThroughPis && __value instanceof PiNode)
                {
                    PiNode __piNode = (PiNode) __value;
                    __value = __piNode.getOriginalNode();
                }
                else if (__value instanceof StampInverter)
                {
                    StampInverter __stampInverter = (StampInverter) __value;
                    __value = __stampInverter.getValue();
                    __stamp = __stampInverter.invertStamp(__stamp);
                }
                else
                {
                    break;
                }
            }
        }

        protected void processAbstractBegin(AbstractBeginNode __beginNode)
        {
            Node __predecessor = __beginNode.predecessor();
            if (__predecessor instanceof IfNode)
            {
                IfNode __ifNode = (IfNode) __predecessor;
                boolean __negated = (__ifNode.falseSuccessor() == __beginNode);
                LogicNode __condition = __ifNode.condition();
                registerNewCondition(__condition, __negated, __beginNode);
            }
            else if (__predecessor instanceof TypeSwitchNode)
            {
                TypeSwitchNode __typeSwitch = (TypeSwitchNode) __predecessor;
                processTypeSwitch(__beginNode, __typeSwitch);
            }
            else if (__predecessor instanceof IntegerSwitchNode)
            {
                IntegerSwitchNode __integerSwitchNode = (IntegerSwitchNode) __predecessor;
                processIntegerSwitch(__beginNode, __integerSwitchNode);
            }
        }

        private static boolean maybeMultipleUsages(ValueNode __value)
        {
            if (__value.hasMoreThanOneUsage())
            {
                return true;
            }
            else
            {
                return __value instanceof ProxyNode || __value instanceof PiNode || __value instanceof StampInverter;
            }
        }

        protected void processIntegerSwitch(AbstractBeginNode __beginNode, IntegerSwitchNode __integerSwitchNode)
        {
            ValueNode __value = __integerSwitchNode.value();
            if (maybeMultipleUsages(__value))
            {
                Stamp __stamp = __integerSwitchNode.getValueStampForSuccessor(__beginNode);
                if (__stamp != null)
                {
                    registerNewStamp(__value, __stamp, __beginNode);
                }
            }
        }

        protected void processTypeSwitch(AbstractBeginNode __beginNode, TypeSwitchNode __typeSwitch)
        {
            ValueNode __hub = __typeSwitch.value();
            if (__hub instanceof LoadHubNode)
            {
                LoadHubNode __loadHub = (LoadHubNode) __hub;
                ValueNode __value = __loadHub.getValue();
                if (maybeMultipleUsages(__value))
                {
                    Stamp __stamp = __typeSwitch.getValueStampForSuccessor(__beginNode);
                    if (__stamp != null)
                    {
                        registerNewStamp(__value, __stamp, __beginNode);
                    }
                }
            }
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
                    this.___map.set(__node, this.___map.get(__node).getParent());
                }
            }
        }
    }

    @FunctionalInterface
    // @iface ConditionalEliminationPhase.InfoElementProvider
    protected interface InfoElementProvider
    {
        Iterable<InfoElement> getInfoElements(ValueNode __value);
    }

    ///
    // Checks for safe nodes when moving pending tests up.
    ///
    // @class ConditionalEliminationPhase.InputFilter
    static final class InputFilter extends Node.EdgeVisitor
    {
        // @field
        boolean ___ok;
        // @field
        private ValueNode ___value;

        // @cons
        InputFilter(ValueNode __value)
        {
            super();
            this.___value = __value;
            this.___ok = true;
        }

        @Override
        public Node apply(Node __node, Node __curNode)
        {
            if (!this.___ok)
            {
                // abort the recursion
                return __curNode;
            }
            if (!(__curNode instanceof ValueNode))
            {
                this.___ok = false;
                return __curNode;
            }
            ValueNode __curValue = (ValueNode) __curNode;
            if (__curValue.isConstant() || __curValue == this.___value || __curValue instanceof ParameterNode)
            {
                return __curNode;
            }
            if (__curValue instanceof BinaryNode || __curValue instanceof UnaryNode)
            {
                __curValue.applyInputs(this);
            }
            else
            {
                this.___ok = false;
            }
            return __curNode;
        }
    }

    @FunctionalInterface
    // @iface ConditionalEliminationPhase.GuardRewirer
    protected interface GuardRewirer
    {
        ///
        // Called if the condition could be proven to have a constant value ({@code result}) under
        // {@code guard}.
        //
        // @param guard the guard whose result is proven
        // @param result the known result of the guard
        // @param newInput new input to pi nodes depending on the new guard
        // @return whether the transformation could be applied
        ///
        boolean rewire(GuardingNode __guard, boolean __result, Stamp __guardedValueStamp, ValueNode __newInput);
    }

    // @class ConditionalEliminationPhase.InfoElement
    protected static final class InfoElement
    {
        // @field
        private final Stamp ___stamp;
        // @field
        private final GuardingNode ___guard;
        // @field
        private final ValueNode ___proxifiedInput;
        // @field
        private final InfoElement ___parent;

        // @cons
        public InfoElement(Stamp __stamp, GuardingNode __guard, ValueNode __proxifiedInput, InfoElement __parent)
        {
            super();
            this.___stamp = __stamp;
            this.___guard = __guard;
            this.___proxifiedInput = __proxifiedInput;
            this.___parent = __parent;
        }

        public InfoElement getParent()
        {
            return this.___parent;
        }

        public Stamp getStamp()
        {
            return this.___stamp;
        }

        public GuardingNode getGuard()
        {
            return this.___guard;
        }

        public ValueNode getProxifiedInput()
        {
            return this.___proxifiedInput;
        }
    }
}
