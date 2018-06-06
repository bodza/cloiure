package giraaff.phases.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.extended.AnchoringNode;
import giraaff.nodes.extended.GuardedNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Replacements;
import giraaff.nodes.spi.StampProvider;
import giraaff.phases.BasePhase;
import giraaff.phases.Phase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.PhaseContext;
import giraaff.util.GraalError;

///
// Processes all {@link Lowerable} nodes to do their lowering.
///
// @class LoweringPhase
public final class LoweringPhase extends BasePhase<PhaseContext>
{
    // @class LoweringPhase.DummyGuardHandle
    static final class DummyGuardHandle extends ValueNode implements GuardedNode
    {
        // @def
        public static final NodeClass<LoweringPhase.DummyGuardHandle> TYPE = NodeClass.create(LoweringPhase.DummyGuardHandle.class);

        @Node.Input(InputType.Guard)
        // @field
        GuardingNode ___guard;

        // @cons LoweringPhase.DummyGuardHandle
        protected DummyGuardHandle(GuardingNode __guard)
        {
            super(TYPE, StampFactory.forVoid());
            this.___guard = __guard;
        }

        @Override
        public GuardingNode getGuard()
        {
            return this.___guard;
        }

        @Override
        public void setGuard(GuardingNode __guard)
        {
            updateUsagesInterface(this.___guard, __guard);
            this.___guard = __guard;
        }

        @Override
        public ValueNode asNode()
        {
            return this;
        }
    }

    // @class LoweringPhase.LoweringToolImpl
    // @closure
    final class LoweringToolImpl implements LoweringTool
    {
        // @field
        private final PhaseContext ___context;
        // @field
        private final NodeBitMap ___activeGuards;
        // @field
        private AnchoringNode ___guardAnchor;
        // @field
        private FixedWithNextNode ___lastFixedNode;

        // @cons LoweringPhase.LoweringToolImpl
        LoweringToolImpl(PhaseContext __context, AnchoringNode __guardAnchor, NodeBitMap __activeGuards, FixedWithNextNode __lastFixedNode)
        {
            super();
            this.___context = __context;
            this.___guardAnchor = __guardAnchor;
            this.___activeGuards = __activeGuards;
            this.___lastFixedNode = __lastFixedNode;
        }

        @Override
        public LoweringTool.LoweringStage getLoweringStage()
        {
            return LoweringPhase.this.___loweringStage;
        }

        @Override
        public ConstantReflectionProvider getConstantReflection()
        {
            return this.___context.getConstantReflection();
        }

        @Override
        public ConstantFieldProvider getConstantFieldProvider()
        {
            return this.___context.getConstantFieldProvider();
        }

        @Override
        public MetaAccessProvider getMetaAccess()
        {
            return this.___context.getMetaAccess();
        }

        @Override
        public LoweringProvider getLowerer()
        {
            return this.___context.getLowerer();
        }

        @Override
        public Replacements getReplacements()
        {
            return this.___context.getReplacements();
        }

        @Override
        public AnchoringNode getCurrentGuardAnchor()
        {
            return this.___guardAnchor;
        }

        @Override
        public GuardingNode createGuard(FixedNode __before, LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action)
        {
            return createGuard(__before, __condition, __deoptReason, __action, JavaConstant.NULL_POINTER, false);
        }

        @Override
        public StampProvider getStampProvider()
        {
            return this.___context.getStampProvider();
        }

        @Override
        public GuardingNode createGuard(FixedNode __before, LogicNode __condition, DeoptimizationReason __deoptReason, DeoptimizationAction __action, JavaConstant __speculation, boolean __negated)
        {
            StructuredGraph __graph = __before.graph();
            if (GraalOptions.optEliminateGuards)
            {
                for (Node __usage : __condition.usages())
                {
                    if (!this.___activeGuards.isNew(__usage) && this.___activeGuards.isMarked(__usage) && ((GuardNode) __usage).isNegated() == __negated)
                    {
                        return (GuardNode) __usage;
                    }
                }
            }
            if (!__condition.graph().getGuardsStage().allowsFloatingGuards())
            {
                FixedGuardNode __fixedGuard = __graph.add(new FixedGuardNode(__condition, __deoptReason, __action, __speculation, __negated));
                __graph.addBeforeFixed(__before, __fixedGuard);
                LoweringPhase.DummyGuardHandle __handle = __graph.add(new LoweringPhase.DummyGuardHandle(__fixedGuard));
                __fixedGuard.lower(this);
                GuardingNode __result = __handle.getGuard();
                __handle.safeDelete();
                return __result;
            }
            else
            {
                GuardNode __newGuard = __graph.unique(new GuardNode(__condition, this.___guardAnchor, __deoptReason, __action, __negated, __speculation));
                if (GraalOptions.optEliminateGuards)
                {
                    this.___activeGuards.markAndGrow(__newGuard);
                }
                return __newGuard;
            }
        }

        @Override
        public FixedWithNextNode lastFixedNode()
        {
            return this.___lastFixedNode;
        }

        private void setLastFixedNode(FixedWithNextNode __n)
        {
            this.___lastFixedNode = __n;
        }
    }

    // @field
    private final CanonicalizerPhase ___canonicalizer;
    // @field
    private final LoweringTool.LoweringStage ___loweringStage;

    // @cons LoweringPhase
    public LoweringPhase(CanonicalizerPhase __canonicalizer, LoweringTool.LoweringStage __loweringStage)
    {
        super();
        this.___canonicalizer = __canonicalizer;
        this.___loweringStage = __loweringStage;
    }

    ///
    // Checks that second lowering of a given graph did not introduce any new nodes.
    //
    // @param graph a graph that was just {@linkplain #lower lowered}
    ///
    private boolean checkPostLowering(StructuredGraph __graph, PhaseContext __context)
    {
        lower(__graph, __context, LoweringPhase.LoweringMode.VERIFY_LOWERING);
        return true;
    }

    @Override
    protected void run(final StructuredGraph __graph, PhaseContext __context)
    {
        lower(__graph, __context, LoweringPhase.LoweringMode.LOWERING);
    }

    private void lower(StructuredGraph __graph, PhaseContext __context, LoweringPhase.LoweringMode __mode)
    {
        IncrementalCanonicalizerPhase<PhaseContext> __incrementalCanonicalizer = new IncrementalCanonicalizerPhase<>(this.___canonicalizer);
        __incrementalCanonicalizer.appendPhase(new LoweringPhase.Round(__context, __mode));
        __incrementalCanonicalizer.apply(__graph, __context);
    }

    // @enum LoweringPhase.LoweringMode
    private enum LoweringMode
    {
        LOWERING,
        VERIFY_LOWERING
    }

    // @class LoweringPhase.Round
    // @closure
    private final class Round extends Phase
    {
        // @field
        private final PhaseContext ___context;
        // @field
        private final LoweringPhase.LoweringMode ___mode;
        // @field
        private StructuredGraph.ScheduleResult ___schedule;
        // @field
        private final SchedulePhase ___schedulePhase;

        // @cons LoweringPhase.Round
        private Round(PhaseContext __context, LoweringPhase.LoweringMode __mode)
        {
            super();
            this.___context = __context;
            this.___mode = __mode;

            // In VERIFY_LOWERING, we want to verify whether the lowering itself changes the graph.
            // Make sure we're not detecting spurious changes because the SchedulePhase modifies the graph.
            boolean __immutableSchedule = __mode == LoweringPhase.LoweringMode.VERIFY_LOWERING;

            this.___schedulePhase = new SchedulePhase(__immutableSchedule);
        }

        @Override
        protected CharSequence getName()
        {
            switch (this.___mode)
            {
                case LOWERING:
                    return "LoweringRound";
                case VERIFY_LOWERING:
                    return "VerifyLoweringRound";
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }

        @Override
        public void run(StructuredGraph __graph)
        {
            this.___schedulePhase.apply(__graph);
            this.___schedule = __graph.getLastSchedule();
            this.___schedule.getCFG().computePostdominators();
            Block __startBlock = this.___schedule.getCFG().getStartBlock();
            LoweringPhase.Round.ProcessFrame __rootFrame = new LoweringPhase.Round.ProcessFrame(__startBlock, __graph.createNodeBitMap(), __startBlock.getBeginNode(), null);
            LoweringPhase.processBlock(__rootFrame);
        }

        // @class LoweringPhase.Round.ProcessFrame
        // @closure
        private final class ProcessFrame extends LoweringPhase.Frame<LoweringPhase.Round.ProcessFrame>
        {
            // @field
            private final NodeBitMap ___activeGuards;
            // @field
            private AnchoringNode ___anchor;

            // @cons LoweringPhase.Round.ProcessFrame
            ProcessFrame(Block __block, NodeBitMap __activeGuards, AnchoringNode __anchor, LoweringPhase.Round.ProcessFrame __parent)
            {
                super(__block, __parent);
                this.___activeGuards = __activeGuards;
                this.___anchor = __anchor;
            }

            @Override
            public void preprocess()
            {
                this.___anchor = LoweringPhase.Round.this.process(this.___block, this.___activeGuards, this.___anchor);
            }

            @Override
            public LoweringPhase.Round.ProcessFrame enter(Block __b)
            {
                return new LoweringPhase.Round.ProcessFrame(__b, this.___activeGuards, __b.getBeginNode(), this);
            }

            @Override
            public LoweringPhase.Frame<?> enterAlwaysReached(Block __b)
            {
                AnchoringNode __newAnchor = this.___anchor;
                if (this.___parent != null && __b.getLoop() != this.___parent.___block.getLoop() && !__b.isLoopHeader())
                {
                    // We are exiting a loop => cannot reuse the anchor without inserting loop proxies.
                    __newAnchor = __b.getBeginNode();
                }
                return new LoweringPhase.Round.ProcessFrame(__b, this.___activeGuards, __newAnchor, this);
            }

            @Override
            public void postprocess()
            {
                if (this.___anchor == this.___block.getBeginNode() && GraalOptions.optEliminateGuards)
                {
                    for (GuardNode __guard : this.___anchor.asNode().usages().filter(GuardNode.class))
                    {
                        if (this.___activeGuards.isMarkedAndGrow(__guard))
                        {
                            this.___activeGuards.clear(__guard);
                        }
                    }
                }
            }
        }

        private AnchoringNode process(final Block __b, final NodeBitMap __activeGuards, final AnchoringNode __startAnchor)
        {
            final LoweringPhase.LoweringToolImpl __loweringTool = new LoweringPhase.LoweringToolImpl(this.___context, __startAnchor, __activeGuards, __b.getBeginNode());

            // Lower the instructions of this block.
            List<Node> __nodes = this.___schedule.nodesFor(__b);
            for (Node __node : __nodes)
            {
                if (__node.isDeleted())
                {
                    // This case can happen when previous lowerings deleted nodes.
                    continue;
                }

                // Cache the next node to be able to reconstruct the previous of the next node after lowering.
                FixedNode __nextNode = null;
                if (__node instanceof FixedWithNextNode)
                {
                    __nextNode = ((FixedWithNextNode) __node).next();
                }
                else
                {
                    __nextNode = __loweringTool.lastFixedNode().next();
                }

                if (__node instanceof Lowerable)
                {
                    ((Lowerable) __node).lower(__loweringTool);
                    if (__loweringTool.___guardAnchor.asNode().isDeleted())
                    {
                        // TODO nextNode could be deleted but this is not currently supported
                        __loweringTool.___guardAnchor = AbstractBeginNode.prevBegin(__nextNode);
                    }
                }

                if (!__nextNode.isAlive())
                {
                    // can happen when the rest of the block is killed by lowering (e.g. by an unconditional deopt)
                    break;
                }
                else
                {
                    Node __nextLastFixed = __nextNode.predecessor();
                    if (!(__nextLastFixed instanceof FixedWithNextNode))
                    {
                        // Insert begin node, to have a valid last fixed for next lowerable node.
                        // This is about lowering a FixedWithNextNode to a control split while this
                        // FixedWithNextNode is followed by some kind of BeginNode.
                        // For example the when a FixedGuard followed by a loop exit is lowered to a
                        // control-split + deopt.
                        AbstractBeginNode __begin = __node.graph().add(new BeginNode());
                        __nextLastFixed.replaceFirstSuccessor(__nextNode, __begin);
                        __begin.setNext(__nextNode);
                        __nextLastFixed = __begin;
                    }
                    __loweringTool.setLastFixedNode((FixedWithNextNode) __nextLastFixed);
                }
            }
            return __loweringTool.getCurrentGuardAnchor();
        }

        ///
        // Gets all usages of a floating, lowerable node that are unscheduled.
        //
        // Given that the lowering of such nodes may introduce fixed nodes, they must be lowered in
        // the context of a usage that dominates all other usages. The fixed nodes resulting from
        // lowering are attached to the fixed node context of the dominating usage. This ensures the
        // post-lowering graph still has a valid schedule.
        //
        // @param node a {@link Lowerable} node
        ///
        private Collection<Node> getUnscheduledUsages(Node __node)
        {
            List<Node> __unscheduledUsages = new ArrayList<>();
            if (__node instanceof FloatingNode)
            {
                for (Node __usage : __node.usages())
                {
                    if (__usage instanceof ValueNode && !(__usage instanceof PhiNode) && !(__usage instanceof ProxyNode))
                    {
                        if (this.___schedule.getCFG().getNodeToBlock().isNew(__usage) || this.___schedule.getCFG().blockFor(__usage) == null)
                        {
                            __unscheduledUsages.add(__usage);
                        }
                    }
                }
            }
            return __unscheduledUsages;
        }
    }

    // @enum LoweringPhase.ProcessBlockState
    enum ProcessBlockState
    {
        ST_ENTER,
        ST_PROCESS,
        ST_ENTER_ALWAYS_REACHED,
        ST_LEAVE,
        ST_PROCESS_ALWAYS_REACHED;
    }

    ///
    // This state-machine resembles the following recursion:
    //
    // <pre>
    // void processBlock(Block block) {
    //     preprocess();
    //     // Process always reached block first.
    //     Block alwaysReachedBlock = block.getPostdominator();
    //     if (alwaysReachedBlock != null && alwaysReachedBlock.getDominator() == block) {
    //         processBlock(alwaysReachedBlock);
    //     }
    //
    //     // Now go for the other dominators.
    //     for (Block dominated : block.getDominated()) {
    //         if (dominated != alwaysReachedBlock) {
    //             assert dominated.getDominator() == block;
    //             processBlock(dominated);
    //         }
    //     }
    //     postprocess();
    // }
    // </pre>
    //
    // This is necessary, as the recursive implementation quickly exceed the stack depth on SPARC.
    //
    // @param rootFrame contains the starting block.
    ///
    public static void processBlock(final LoweringPhase.Frame<?> __rootFrame)
    {
        LoweringPhase.ProcessBlockState __state = LoweringPhase.ProcessBlockState.ST_PROCESS;
        LoweringPhase.Frame<?> __f = __rootFrame;
        while (__f != null)
        {
            LoweringPhase.ProcessBlockState __nextState;
            if (__state == LoweringPhase.ProcessBlockState.ST_PROCESS || __state == LoweringPhase.ProcessBlockState.ST_PROCESS_ALWAYS_REACHED)
            {
                __f.preprocess();
                __nextState = __state == LoweringPhase.ProcessBlockState.ST_PROCESS_ALWAYS_REACHED ? LoweringPhase.ProcessBlockState.ST_ENTER : LoweringPhase.ProcessBlockState.ST_ENTER_ALWAYS_REACHED;
            }
            else if (__state == LoweringPhase.ProcessBlockState.ST_ENTER_ALWAYS_REACHED)
            {
                if (__f.___alwaysReachedBlock != null && __f.___alwaysReachedBlock.getDominator() == __f.___block)
                {
                    __f = __f.enterAlwaysReached(__f.___alwaysReachedBlock);
                    __nextState = LoweringPhase.ProcessBlockState.ST_PROCESS;
                }
                else
                {
                    __nextState = LoweringPhase.ProcessBlockState.ST_ENTER;
                }
            }
            else if (__state == LoweringPhase.ProcessBlockState.ST_ENTER)
            {
                if (__f.___dominated != null)
                {
                    Block __n = __f.___dominated;
                    __f.___dominated = __n.getDominatedSibling();
                    if (__n == __f.___alwaysReachedBlock)
                    {
                        if (__f.___dominated != null)
                        {
                            __n = __f.___dominated;
                            __f.___dominated = __n.getDominatedSibling();
                        }
                        else
                        {
                            __n = null;
                        }
                    }
                    if (__n == null)
                    {
                        __nextState = LoweringPhase.ProcessBlockState.ST_LEAVE;
                    }
                    else
                    {
                        __f = __f.enter(__n);
                        __nextState = LoweringPhase.ProcessBlockState.ST_PROCESS;
                    }
                }
                else
                {
                    __nextState = LoweringPhase.ProcessBlockState.ST_LEAVE;
                }
            }
            else if (__state == LoweringPhase.ProcessBlockState.ST_LEAVE)
            {
                __f.postprocess();
                __f = __f.___parent;
                __nextState = LoweringPhase.ProcessBlockState.ST_ENTER;
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
            __state = __nextState;
        }
    }

    public static void processBlockBounded(final LoweringPhase.Frame<?> __rootFrame)
    {
        LoweringPhase.ProcessBlockState __state = LoweringPhase.ProcessBlockState.ST_PROCESS;
        LoweringPhase.Frame<?> __f = __rootFrame;
        while (__f != null)
        {
            LoweringPhase.ProcessBlockState __nextState;
            if (__state == LoweringPhase.ProcessBlockState.ST_PROCESS || __state == LoweringPhase.ProcessBlockState.ST_PROCESS_ALWAYS_REACHED)
            {
                __f.preprocess();
                __nextState = __state == LoweringPhase.ProcessBlockState.ST_PROCESS_ALWAYS_REACHED ? LoweringPhase.ProcessBlockState.ST_ENTER : LoweringPhase.ProcessBlockState.ST_ENTER_ALWAYS_REACHED;
            }
            else if (__state == LoweringPhase.ProcessBlockState.ST_ENTER_ALWAYS_REACHED)
            {
                if (__f.___alwaysReachedBlock != null && __f.___alwaysReachedBlock.getDominator() == __f.___block)
                {
                    LoweringPhase.Frame<?> __continueRecur = __f.enterAlwaysReached(__f.___alwaysReachedBlock);
                    if (__continueRecur == null)
                    {
                        // stop recursion here
                        __f.postprocess();
                        __f = __f.___parent;
                        __state = LoweringPhase.ProcessBlockState.ST_ENTER;
                        continue;
                    }
                    __f = __continueRecur;
                    __nextState = LoweringPhase.ProcessBlockState.ST_PROCESS;
                }
                else
                {
                    __nextState = LoweringPhase.ProcessBlockState.ST_ENTER;
                }
            }
            else if (__state == LoweringPhase.ProcessBlockState.ST_ENTER)
            {
                if (__f.___dominated != null)
                {
                    Block __n = __f.___dominated;
                    __f.___dominated = __n.getDominatedSibling();
                    if (__n == __f.___alwaysReachedBlock)
                    {
                        if (__f.___dominated != null)
                        {
                            __n = __f.___dominated;
                            __f.___dominated = __n.getDominatedSibling();
                        }
                        else
                        {
                            __n = null;
                        }
                    }
                    if (__n == null)
                    {
                        __nextState = LoweringPhase.ProcessBlockState.ST_LEAVE;
                    }
                    else
                    {
                        LoweringPhase.Frame<?> __continueRecur = __f.enter(__n);
                        if (__continueRecur == null)
                        {
                            // stop recursion here
                            __f.postprocess();
                            __f = __f.___parent;
                            __state = LoweringPhase.ProcessBlockState.ST_ENTER;
                            continue;
                        }
                        __f = __continueRecur;
                        __nextState = LoweringPhase.ProcessBlockState.ST_PROCESS;
                    }
                }
                else
                {
                    __nextState = LoweringPhase.ProcessBlockState.ST_LEAVE;
                }
            }
            else if (__state == LoweringPhase.ProcessBlockState.ST_LEAVE)
            {
                __f.postprocess();
                __f = __f.___parent;
                __nextState = LoweringPhase.ProcessBlockState.ST_ENTER;
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
            __state = __nextState;
        }
    }

    // @class LoweringPhase.Frame
    public abstract static class Frame<T extends LoweringPhase.Frame<?>>
    {
        // @field
        protected final Block ___block;
        // @field
        final T ___parent;
        // @field
        Block ___dominated;
        // @field
        final Block ___alwaysReachedBlock;

        // @cons LoweringPhase.Frame
        public Frame(Block __block, T __parent)
        {
            super();
            this.___block = __block;
            this.___alwaysReachedBlock = __block.getPostdominator();
            this.___dominated = __block.getFirstDominated();
            this.___parent = __parent;
        }

        public LoweringPhase.Frame<?> enterAlwaysReached(Block __b)
        {
            return enter(__b);
        }

        public abstract LoweringPhase.Frame<?> enter(Block __b);

        public abstract void preprocess();

        public abstract void postprocess();
    }
}
