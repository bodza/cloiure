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
import giraaff.graph.Graph.Mark;
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
import giraaff.nodes.StructuredGraph.ScheduleResult;
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
import giraaff.options.OptionValues;
import giraaff.phases.BasePhase;
import giraaff.phases.Phase;
import giraaff.phases.common.LoweringPhase.ProcessBlockState;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.PhaseContext;
import giraaff.util.GraalError;

/**
 * Processes all {@link Lowerable} nodes to do their lowering.
 */
public class LoweringPhase extends BasePhase<PhaseContext>
{
    static final class DummyGuardHandle extends ValueNode implements GuardedNode
    {
        public static final NodeClass<DummyGuardHandle> TYPE = NodeClass.create(DummyGuardHandle.class);

        @Input(InputType.Guard) GuardingNode guard;

        protected DummyGuardHandle(GuardingNode guard)
        {
            super(TYPE, StampFactory.forVoid());
            this.guard = guard;
        }

        @Override
        public GuardingNode getGuard()
        {
            return guard;
        }

        @Override
        public void setGuard(GuardingNode guard)
        {
            updateUsagesInterface(this.guard, guard);
            this.guard = guard;
        }

        @Override
        public ValueNode asNode()
        {
            return this;
        }
    }

    final class LoweringToolImpl implements LoweringTool
    {
        private final PhaseContext context;
        private final NodeBitMap activeGuards;
        private AnchoringNode guardAnchor;
        private FixedWithNextNode lastFixedNode;

        LoweringToolImpl(PhaseContext context, AnchoringNode guardAnchor, NodeBitMap activeGuards, FixedWithNextNode lastFixedNode)
        {
            this.context = context;
            this.guardAnchor = guardAnchor;
            this.activeGuards = activeGuards;
            this.lastFixedNode = lastFixedNode;
        }

        @Override
        public LoweringStage getLoweringStage()
        {
            return loweringStage;
        }

        @Override
        public ConstantReflectionProvider getConstantReflection()
        {
            return context.getConstantReflection();
        }

        @Override
        public ConstantFieldProvider getConstantFieldProvider()
        {
            return context.getConstantFieldProvider();
        }

        @Override
        public MetaAccessProvider getMetaAccess()
        {
            return context.getMetaAccess();
        }

        @Override
        public LoweringProvider getLowerer()
        {
            return context.getLowerer();
        }

        @Override
        public Replacements getReplacements()
        {
            return context.getReplacements();
        }

        @Override
        public AnchoringNode getCurrentGuardAnchor()
        {
            return guardAnchor;
        }

        @Override
        public GuardingNode createGuard(FixedNode before, LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action)
        {
            return createGuard(before, condition, deoptReason, action, JavaConstant.NULL_POINTER, false);
        }

        @Override
        public StampProvider getStampProvider()
        {
            return context.getStampProvider();
        }

        @Override
        public GuardingNode createGuard(FixedNode before, LogicNode condition, DeoptimizationReason deoptReason, DeoptimizationAction action, JavaConstant speculation, boolean negated)
        {
            StructuredGraph graph = before.graph();
            if (GraalOptions.OptEliminateGuards.getValue(graph.getOptions()))
            {
                for (Node usage : condition.usages())
                {
                    if (!activeGuards.isNew(usage) && activeGuards.isMarked(usage) && ((GuardNode) usage).isNegated() == negated)
                    {
                        return (GuardNode) usage;
                    }
                }
            }
            if (!condition.graph().getGuardsStage().allowsFloatingGuards())
            {
                FixedGuardNode fixedGuard = graph.add(new FixedGuardNode(condition, deoptReason, action, speculation, negated));
                graph.addBeforeFixed(before, fixedGuard);
                DummyGuardHandle handle = graph.add(new DummyGuardHandle(fixedGuard));
                fixedGuard.lower(this);
                GuardingNode result = handle.getGuard();
                handle.safeDelete();
                return result;
            }
            else
            {
                GuardNode newGuard = graph.unique(new GuardNode(condition, guardAnchor, deoptReason, action, negated, speculation));
                if (GraalOptions.OptEliminateGuards.getValue(graph.getOptions()))
                {
                    activeGuards.markAndGrow(newGuard);
                }
                return newGuard;
            }
        }

        @Override
        public FixedWithNextNode lastFixedNode()
        {
            return lastFixedNode;
        }

        private void setLastFixedNode(FixedWithNextNode n)
        {
            lastFixedNode = n;
        }
    }

    private final CanonicalizerPhase canonicalizer;
    private final LoweringTool.LoweringStage loweringStage;

    public LoweringPhase(CanonicalizerPhase canonicalizer, LoweringTool.LoweringStage loweringStage)
    {
        this.canonicalizer = canonicalizer;
        this.loweringStage = loweringStage;
    }

    /**
     * Checks that second lowering of a given graph did not introduce any new nodes.
     *
     * @param graph a graph that was just {@linkplain #lower lowered}
     */
    private boolean checkPostLowering(StructuredGraph graph, PhaseContext context)
    {
        Mark expectedMark = graph.getMark();
        lower(graph, context, LoweringMode.VERIFY_LOWERING);
        Mark mark = graph.getMark();
        return true;
    }

    @Override
    protected void run(final StructuredGraph graph, PhaseContext context)
    {
        lower(graph, context, LoweringMode.LOWERING);
    }

    private void lower(StructuredGraph graph, PhaseContext context, LoweringMode mode)
    {
        IncrementalCanonicalizerPhase<PhaseContext> incrementalCanonicalizer = new IncrementalCanonicalizerPhase<>(canonicalizer);
        incrementalCanonicalizer.appendPhase(new Round(context, mode, graph.getOptions()));
        incrementalCanonicalizer.apply(graph, context);
    }

    private enum LoweringMode
    {
        LOWERING,
        VERIFY_LOWERING
    }

    private final class Round extends Phase
    {
        private final PhaseContext context;
        private final LoweringMode mode;
        private ScheduleResult schedule;
        private final SchedulePhase schedulePhase;

        private Round(PhaseContext context, LoweringMode mode, OptionValues options)
        {
            this.context = context;
            this.mode = mode;

            /*
             * In VERIFY_LOWERING, we want to verify whether the lowering itself changes the graph.
             * Make sure we're not detecting spurious changes because the SchedulePhase modifies the graph.
             */
            boolean immutableSchedule = mode == LoweringMode.VERIFY_LOWERING;

            this.schedulePhase = new SchedulePhase(immutableSchedule, options);
        }

        @Override
        protected CharSequence getName()
        {
            switch (mode)
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
        public void run(StructuredGraph graph)
        {
            schedulePhase.apply(graph);
            schedule = graph.getLastSchedule();
            schedule.getCFG().computePostdominators();
            Block startBlock = schedule.getCFG().getStartBlock();
            ProcessFrame rootFrame = new ProcessFrame(startBlock, graph.createNodeBitMap(), startBlock.getBeginNode(), null);
            LoweringPhase.processBlock(rootFrame);
        }

        private class ProcessFrame extends Frame<ProcessFrame>
        {
            private final NodeBitMap activeGuards;
            private AnchoringNode anchor;

            ProcessFrame(Block block, NodeBitMap activeGuards, AnchoringNode anchor, ProcessFrame parent)
            {
                super(block, parent);
                this.activeGuards = activeGuards;
                this.anchor = anchor;
            }

            @Override
            public void preprocess()
            {
                this.anchor = Round.this.process(block, activeGuards, anchor);
            }

            @Override
            public ProcessFrame enter(Block b)
            {
                return new ProcessFrame(b, activeGuards, b.getBeginNode(), this);
            }

            @Override
            public Frame<?> enterAlwaysReached(Block b)
            {
                AnchoringNode newAnchor = anchor;
                if (parent != null && b.getLoop() != parent.block.getLoop() && !b.isLoopHeader())
                {
                    // We are exiting a loop => cannot reuse the anchor without inserting loop proxies.
                    newAnchor = b.getBeginNode();
                }
                return new ProcessFrame(b, activeGuards, newAnchor, this);
            }

            @Override
            public void postprocess()
            {
                if (anchor == block.getBeginNode() && GraalOptions.OptEliminateGuards.getValue(activeGuards.graph().getOptions()))
                {
                    for (GuardNode guard : anchor.asNode().usages().filter(GuardNode.class))
                    {
                        if (activeGuards.isMarkedAndGrow(guard))
                        {
                            activeGuards.clear(guard);
                        }
                    }
                }
            }
        }

        private AnchoringNode process(final Block b, final NodeBitMap activeGuards, final AnchoringNode startAnchor)
        {
            final LoweringToolImpl loweringTool = new LoweringToolImpl(context, startAnchor, activeGuards, b.getBeginNode());

            // Lower the instructions of this block.
            List<Node> nodes = schedule.nodesFor(b);
            for (Node node : nodes)
            {
                if (node.isDeleted())
                {
                    // This case can happen when previous lowerings deleted nodes.
                    continue;
                }

                // Cache the next node to be able to reconstruct the previous of the next node after lowering.
                FixedNode nextNode = null;
                if (node instanceof FixedWithNextNode)
                {
                    nextNode = ((FixedWithNextNode) node).next();
                }
                else
                {
                    nextNode = loweringTool.lastFixedNode().next();
                }

                if (node instanceof Lowerable)
                {
                    ((Lowerable) node).lower(loweringTool);
                    if (loweringTool.guardAnchor.asNode().isDeleted())
                    {
                        // TODO nextNode could be deleted but this is not currently supported
                        loweringTool.guardAnchor = AbstractBeginNode.prevBegin(nextNode);
                    }
                }

                if (!nextNode.isAlive())
                {
                    // can happen when the rest of the block is killed by lowering (e.g. by an unconditional deopt)
                    break;
                }
                else
                {
                    Node nextLastFixed = nextNode.predecessor();
                    if (!(nextLastFixed instanceof FixedWithNextNode))
                    {
                        // Insert begin node, to have a valid last fixed for next lowerable node.
                        // This is about lowering a FixedWithNextNode to a control split while this
                        // FixedWithNextNode is followed by some kind of BeginNode.
                        // For example the when a FixedGuard followed by a loop exit is lowered to a
                        // control-split + deopt.
                        AbstractBeginNode begin = node.graph().add(new BeginNode());
                        nextLastFixed.replaceFirstSuccessor(nextNode, begin);
                        begin.setNext(nextNode);
                        nextLastFixed = begin;
                    }
                    loweringTool.setLastFixedNode((FixedWithNextNode) nextLastFixed);
                }
            }
            return loweringTool.getCurrentGuardAnchor();
        }

        /**
         * Gets all usages of a floating, lowerable node that are unscheduled.
         *
         * Given that the lowering of such nodes may introduce fixed nodes, they must be lowered in
         * the context of a usage that dominates all other usages. The fixed nodes resulting from
         * lowering are attached to the fixed node context of the dominating usage. This ensures the
         * post-lowering graph still has a valid schedule.
         *
         * @param node a {@link Lowerable} node
         */
        private Collection<Node> getUnscheduledUsages(Node node)
        {
            List<Node> unscheduledUsages = new ArrayList<>();
            if (node instanceof FloatingNode)
            {
                for (Node usage : node.usages())
                {
                    if (usage instanceof ValueNode && !(usage instanceof PhiNode) && !(usage instanceof ProxyNode))
                    {
                        if (schedule.getCFG().getNodeToBlock().isNew(usage) || schedule.getCFG().blockFor(usage) == null)
                        {
                            unscheduledUsages.add(usage);
                        }
                    }
                }
            }
            return unscheduledUsages;
        }
    }

    enum ProcessBlockState
    {
        ST_ENTER,
        ST_PROCESS,
        ST_ENTER_ALWAYS_REACHED,
        ST_LEAVE,
        ST_PROCESS_ALWAYS_REACHED;
    }

    /**
     * This state-machine resembles the following recursion:
     *
     * <pre>
     * void processBlock(Block block) {
     *     preprocess();
     *     // Process always reached block first.
     *     Block alwaysReachedBlock = block.getPostdominator();
     *     if (alwaysReachedBlock != null &amp;&amp; alwaysReachedBlock.getDominator() == block) {
     *         processBlock(alwaysReachedBlock);
     *     }
     *
     *     // Now go for the other dominators.
     *     for (Block dominated : block.getDominated()) {
     *         if (dominated != alwaysReachedBlock) {
     *             assert dominated.getDominator() == block;
     *             processBlock(dominated);
     *         }
     *     }
     *     postprocess();
     * }
     * </pre>
     *
     * This is necessary, as the recursive implementation quickly exceed the stack depth on SPARC.
     *
     * @param rootFrame contains the starting block.
     */
    public static void processBlock(final Frame<?> rootFrame)
    {
        ProcessBlockState state = ProcessBlockState.ST_PROCESS;
        Frame<?> f = rootFrame;
        while (f != null)
        {
            ProcessBlockState nextState;
            if (state == ProcessBlockState.ST_PROCESS || state == ProcessBlockState.ST_PROCESS_ALWAYS_REACHED)
            {
                f.preprocess();
                nextState = state == ProcessBlockState.ST_PROCESS_ALWAYS_REACHED ? ProcessBlockState.ST_ENTER : ProcessBlockState.ST_ENTER_ALWAYS_REACHED;
            }
            else if (state == ProcessBlockState.ST_ENTER_ALWAYS_REACHED)
            {
                if (f.alwaysReachedBlock != null && f.alwaysReachedBlock.getDominator() == f.block)
                {
                    f = f.enterAlwaysReached(f.alwaysReachedBlock);
                    nextState = ProcessBlockState.ST_PROCESS;
                }
                else
                {
                    nextState = ProcessBlockState.ST_ENTER;
                }
            }
            else if (state == ProcessBlockState.ST_ENTER)
            {
                if (f.dominated != null)
                {
                    Block n = f.dominated;
                    f.dominated = n.getDominatedSibling();
                    if (n == f.alwaysReachedBlock)
                    {
                        if (f.dominated != null)
                        {
                            n = f.dominated;
                            f.dominated = n.getDominatedSibling();
                        }
                        else
                        {
                            n = null;
                        }
                    }
                    if (n == null)
                    {
                        nextState = ProcessBlockState.ST_LEAVE;
                    }
                    else
                    {
                        f = f.enter(n);
                        nextState = ProcessBlockState.ST_PROCESS;
                    }
                }
                else
                {
                    nextState = ProcessBlockState.ST_LEAVE;
                }
            }
            else if (state == ProcessBlockState.ST_LEAVE)
            {
                f.postprocess();
                f = f.parent;
                nextState = ProcessBlockState.ST_ENTER;
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
            state = nextState;
        }
    }

    public static void processBlockBounded(final Frame<?> rootFrame)
    {
        ProcessBlockState state = ProcessBlockState.ST_PROCESS;
        Frame<?> f = rootFrame;
        while (f != null)
        {
            ProcessBlockState nextState;
            if (state == ProcessBlockState.ST_PROCESS || state == ProcessBlockState.ST_PROCESS_ALWAYS_REACHED)
            {
                f.preprocess();
                nextState = state == ProcessBlockState.ST_PROCESS_ALWAYS_REACHED ? ProcessBlockState.ST_ENTER : ProcessBlockState.ST_ENTER_ALWAYS_REACHED;
            }
            else if (state == ProcessBlockState.ST_ENTER_ALWAYS_REACHED)
            {
                if (f.alwaysReachedBlock != null && f.alwaysReachedBlock.getDominator() == f.block)
                {
                    Frame<?> continueRecur = f.enterAlwaysReached(f.alwaysReachedBlock);
                    if (continueRecur == null)
                    {
                        // stop recursion here
                        f.postprocess();
                        f = f.parent;
                        state = ProcessBlockState.ST_ENTER;
                        continue;
                    }
                    f = continueRecur;
                    nextState = ProcessBlockState.ST_PROCESS;
                }
                else
                {
                    nextState = ProcessBlockState.ST_ENTER;
                }
            }
            else if (state == ProcessBlockState.ST_ENTER)
            {
                if (f.dominated != null)
                {
                    Block n = f.dominated;
                    f.dominated = n.getDominatedSibling();
                    if (n == f.alwaysReachedBlock)
                    {
                        if (f.dominated != null)
                        {
                            n = f.dominated;
                            f.dominated = n.getDominatedSibling();
                        }
                        else
                        {
                            n = null;
                        }
                    }
                    if (n == null)
                    {
                        nextState = ProcessBlockState.ST_LEAVE;
                    }
                    else
                    {
                        Frame<?> continueRecur = f.enter(n);
                        if (continueRecur == null)
                        {
                            // stop recursion here
                            f.postprocess();
                            f = f.parent;
                            state = ProcessBlockState.ST_ENTER;
                            continue;
                        }
                        f = continueRecur;
                        nextState = ProcessBlockState.ST_PROCESS;
                    }
                }
                else
                {
                    nextState = ProcessBlockState.ST_LEAVE;
                }
            }
            else if (state == ProcessBlockState.ST_LEAVE)
            {
                f.postprocess();
                f = f.parent;
                nextState = ProcessBlockState.ST_ENTER;
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
            state = nextState;
        }
    }

    public abstract static class Frame<T extends Frame<?>>
    {
        protected final Block block;
        final T parent;
        Block dominated;
        final Block alwaysReachedBlock;

        public Frame(Block block, T parent)
        {
            this.block = block;
            this.alwaysReachedBlock = block.getPostdominator();
            this.dominated = block.getFirstDominated();
            this.parent = parent;
        }

        public Frame<?> enterAlwaysReached(Block b)
        {
            return enter(b);
        }

        public abstract Frame<?> enter(Block b);

        public abstract void preprocess();

        public abstract void postprocess();
    }
}
