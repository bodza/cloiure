package giraaff.nodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.DefaultProfilingInfo;
import jdk.vm.ci.meta.ProfilingInfo;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.SpeculationLog;
import jdk.vm.ci.meta.TriState;
import jdk.vm.ci.runtime.JVMCICompiler;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableEconomicMap;

import giraaff.core.common.cfg.BlockMap;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.NodeMap;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionValues;

/**
 * A graph that contains at least one distinguished node : the {@link #start() start} node. This
 * node is the start of the control flow of the graph.
 */
// @class StructuredGraph
public final class StructuredGraph extends Graph
{
    /**
     * The different stages of the compilation of a {@link Graph} regarding the status of
     * {@link GuardNode guards}, {@link DeoptimizingNode deoptimizations} and {@link FrameState
     * framestates}. The stage of a graph progresses monotonously.
     */
    // @enum StructuredGraph.GuardsStage
    public enum GuardsStage
    {
        /**
         * During this stage, there can be {@link FloatingNode floating} {@link DeoptimizingNode}
         * such as {@link GuardNode GuardNodes}. New {@link DeoptimizingNode DeoptimizingNodes} can
         * be introduced without constraints. {@link FrameState} nodes are associated with
         * {@link StateSplit} nodes.
         */
        FLOATING_GUARDS,
        /**
         * During this stage, all {@link DeoptimizingNode DeoptimizingNodes} must be
         * {@link FixedNode fixed} but new {@link DeoptimizingNode DeoptimizingNodes} can still be
         * introduced. {@link FrameState} nodes are still associated with {@link StateSplit} nodes.
         */
        FIXED_DEOPTS,
        /**
         * During this stage, all {@link DeoptimizingNode DeoptimizingNodes} must be
         * {@link FixedNode fixed}. New {@link DeoptimizingNode DeoptimizingNodes} can not be
         * introduced any more. {@link FrameState} nodes are now associated with
         * {@link DeoptimizingNode} nodes.
         */
        AFTER_FSA;

        public boolean allowsFloatingGuards()
        {
            return this == FLOATING_GUARDS;
        }

        public boolean areFrameStatesAtDeopts()
        {
            return this == AFTER_FSA;
        }

        public boolean areFrameStatesAtSideEffects()
        {
            return !this.areFrameStatesAtDeopts();
        }

        public boolean areDeoptsFixed()
        {
            return this.ordinal() >= FIXED_DEOPTS.ordinal();
        }
    }

    /**
     * Constants denoting whether or not {@link Assumption}s can be made while processing a graph.
     */
    // @enum StructuredGraph.AllowAssumptions
    public enum AllowAssumptions
    {
        YES,
        NO;
        public static AllowAssumptions ifTrue(boolean flag)
        {
            return flag ? YES : NO;
        }

        public static AllowAssumptions ifNonNull(Assumptions assumptions)
        {
            return assumptions != null ? YES : NO;
        }
    }

    // @class StructuredGraph.ScheduleResult
    public static final class ScheduleResult
    {
        private final ControlFlowGraph cfg;
        private final NodeMap<Block> nodeToBlockMap;
        private final BlockMap<List<Node>> blockToNodesMap;

        // @cons
        public ScheduleResult(ControlFlowGraph cfg, NodeMap<Block> nodeToBlockMap, BlockMap<List<Node>> blockToNodesMap)
        {
            super();
            this.cfg = cfg;
            this.nodeToBlockMap = nodeToBlockMap;
            this.blockToNodesMap = blockToNodesMap;
        }

        public ControlFlowGraph getCFG()
        {
            return cfg;
        }

        public NodeMap<Block> getNodeToBlockMap()
        {
            return nodeToBlockMap;
        }

        public BlockMap<List<Node>> getBlockToNodesMap()
        {
            return blockToNodesMap;
        }

        public List<Node> nodesFor(Block block)
        {
            return blockToNodesMap.get(block);
        }
    }

    /**
     * Object used to create a {@link StructuredGraph}.
     */
    // @class StructuredGraph.Builder
    public static final class Builder
    {
        private final Assumptions assumptions;
        private SpeculationLog speculationLog;
        private ResolvedJavaMethod rootMethod;
        private int entryBCI = JVMCICompiler.INVOCATION_ENTRY_BCI;
        private boolean useProfilingInfo = true;
        private final OptionValues options;

        /**
         * Creates a builder for a graph.
         */
        // @cons
        public Builder(OptionValues options, AllowAssumptions allowAssumptions)
        {
            super();
            this.options = options;
            this.assumptions = allowAssumptions == AllowAssumptions.YES ? new Assumptions() : null;
        }

        /**
         * Creates a builder for a graph that does not support {@link Assumptions}.
         */
        // @cons
        public Builder(OptionValues options)
        {
            super();
            this.options = options;
            this.assumptions = null;
        }

        public ResolvedJavaMethod getMethod()
        {
            return rootMethod;
        }

        public Builder method(ResolvedJavaMethod method)
        {
            this.rootMethod = method;
            return this;
        }

        public SpeculationLog getSpeculationLog()
        {
            return speculationLog;
        }

        public Builder speculationLog(SpeculationLog log)
        {
            this.speculationLog = log;
            return this;
        }

        public int getEntryBCI()
        {
            return entryBCI;
        }

        public Builder entryBCI(int bci)
        {
            this.entryBCI = bci;
            return this;
        }

        public boolean getUseProfilingInfo()
        {
            return useProfilingInfo;
        }

        public Builder useProfilingInfo(boolean flag)
        {
            this.useProfilingInfo = flag;
            return this;
        }

        public StructuredGraph build()
        {
            return new StructuredGraph(rootMethod, entryBCI, assumptions, speculationLog, useProfilingInfo, options);
        }
    }

    public static final long INVALID_GRAPH_ID = -1;
    private static final AtomicLong uniqueGraphIds = new AtomicLong();

    private StartNode start;
    private ResolvedJavaMethod rootMethod;
    private final long graphId;
    private final int entryBCI;
    private GuardsStage guardsStage = GuardsStage.FLOATING_GUARDS;
    private boolean isAfterFloatingReadPhase = false;
    private boolean isAfterFixedReadPhase = false;
    private boolean hasValueProxies = true;
    private boolean isAfterExpandLogic = false;
    private final boolean useProfilingInfo;
    /**
     * The assumptions made while constructing and transforming this graph.
     */
    private final Assumptions assumptions;

    private SpeculationLog speculationLog;

    private ScheduleResult lastSchedule;

    /**
     * Records the methods that were used while constructing this graph, one entry for each time a
     * specific method is used.
     */
    private final List<ResolvedJavaMethod> methods = new ArrayList<>();

    /**
     * Records the fields that were accessed while constructing this graph.
     */

    private EconomicSet<ResolvedJavaField> fields = null;

    // @enum StructuredGraph.UnsafeAccessState
    private enum UnsafeAccessState
    {
        NO_ACCESS,
        HAS_ACCESS,
        DISABLED
    }

    private UnsafeAccessState hasUnsafeAccess = UnsafeAccessState.NO_ACCESS;

    public static final boolean USE_PROFILING_INFO = true;

    public static final boolean NO_PROFILING_INFO = false;

    // @cons
    private StructuredGraph(ResolvedJavaMethod method, int entryBCI, Assumptions assumptions, SpeculationLog speculationLog, boolean useProfilingInfo, OptionValues options)
    {
        super(options);
        this.setStart(add(new StartNode()));
        this.rootMethod = method;
        this.graphId = uniqueGraphIds.incrementAndGet();
        this.entryBCI = entryBCI;
        this.assumptions = assumptions;
        if (speculationLog != null && !(speculationLog instanceof GraphSpeculationLog))
        {
            this.speculationLog = new GraphSpeculationLog(speculationLog);
        }
        else
        {
            this.speculationLog = speculationLog;
        }
        this.useProfilingInfo = useProfilingInfo;
    }

    public void setLastSchedule(ScheduleResult result)
    {
        lastSchedule = result;
    }

    public ScheduleResult getLastSchedule()
    {
        return lastSchedule;
    }

    public void clearLastSchedule()
    {
        setLastSchedule(null);
    }

    @Override
    public boolean maybeCompress()
    {
        if (super.maybeCompress())
        {
            // The schedule contains a NodeMap which is unusable after compression.
            clearLastSchedule();
            return true;
        }
        return false;
    }

    public Stamp getReturnStamp()
    {
        Stamp returnStamp = null;
        for (ReturnNode returnNode : getNodes(ReturnNode.TYPE))
        {
            ValueNode result = returnNode.result();
            if (result != null)
            {
                if (returnStamp == null)
                {
                    returnStamp = result.stamp(NodeView.DEFAULT);
                }
                else
                {
                    returnStamp = returnStamp.meet(result.stamp(NodeView.DEFAULT));
                }
            }
        }
        return returnStamp;
    }

    public StartNode start()
    {
        return start;
    }

    /**
     * Gets the root method from which this graph was built.
     *
     * @return null if this method was not built from a method or the method is not available
     */
    public ResolvedJavaMethod method()
    {
        return rootMethod;
    }

    public int getEntryBCI()
    {
        return entryBCI;
    }

    public boolean isOSR()
    {
        return entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;
    }

    public long graphId()
    {
        return graphId;
    }

    public void setStart(StartNode start)
    {
        this.start = start;
    }

    /**
     * Creates a copy of this graph.
     */
    @Override
    public StructuredGraph copy()
    {
        StructuredGraph copy = new StructuredGraph(method(), entryBCI, assumptions != null ? new Assumptions() : null, speculationLog, useProfilingInfo, getOptions());
        if (AllowAssumptions.ifNonNull(assumptions) == AllowAssumptions.YES && assumptions != null)
        {
            copy.assumptions.record(assumptions);
        }
        copy.hasUnsafeAccess = hasUnsafeAccess;
        copy.setGuardsStage(getGuardsStage());
        copy.isAfterFloatingReadPhase = isAfterFloatingReadPhase;
        copy.hasValueProxies = hasValueProxies;
        copy.isAfterExpandLogic = isAfterExpandLogic;
        return copy;
    }

    public ParameterNode getParameter(int index)
    {
        for (ParameterNode param : getNodes(ParameterNode.TYPE))
        {
            if (param.index() == index)
            {
                return param;
            }
        }
        return null;
    }

    public Iterable<Invoke> getInvokes()
    {
        final Iterator<MethodCallTargetNode> callTargets = getNodes(MethodCallTargetNode.TYPE).iterator();
        return new Iterable<Invoke>()
        {
            private Invoke next;

            @Override
            public Iterator<Invoke> iterator()
            {
                return new Iterator<Invoke>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        if (next == null)
                        {
                            while (callTargets.hasNext())
                            {
                                Invoke i = callTargets.next().invoke();
                                if (i != null)
                                {
                                    next = i;
                                    return true;
                                }
                            }
                            return false;
                        }
                        else
                        {
                            return true;
                        }
                    }

                    @Override
                    public Invoke next()
                    {
                        try
                        {
                            return next;
                        }
                        finally
                        {
                            next = null;
                        }
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public boolean hasLoops()
    {
        return hasNode(LoopBeginNode.TYPE);
    }

    /**
     * Unlinks a node from all its control flow neighbors and then removes it from its graph. The
     * node must have no {@linkplain Node#usages() usages}.
     *
     * @param node the node to be unlinked and removed
     */
    @SuppressWarnings("static-method")
    public void removeFixed(FixedWithNextNode node)
    {
        if (node instanceof AbstractBeginNode)
        {
            ((AbstractBeginNode) node).prepareDelete();
        }
        GraphUtil.unlinkFixedNode(node);
        node.safeDelete();
    }

    public void replaceFixed(FixedWithNextNode node, Node replacement)
    {
        if (replacement instanceof FixedWithNextNode)
        {
            replaceFixedWithFixed(node, (FixedWithNextNode) replacement);
        }
        else
        {
            replaceFixedWithFloating(node, (FloatingNode) replacement);
        }
    }

    public void replaceFixedWithFixed(FixedWithNextNode node, FixedWithNextNode replacement)
    {
        FixedNode next = node.next();
        node.setNext(null);
        replacement.setNext(next);
        node.replaceAndDelete(replacement);
        if (node == start)
        {
            setStart((StartNode) replacement);
        }
    }

    @SuppressWarnings("static-method")
    public void replaceFixedWithFloating(FixedWithNextNode node, ValueNode replacement)
    {
        GraphUtil.unlinkFixedNode(node);
        node.replaceAtUsagesAndDelete(replacement);
    }

    @SuppressWarnings("static-method")
    public void removeSplit(ControlSplitNode node, AbstractBeginNode survivingSuccessor)
    {
        node.clearSuccessors();
        node.replaceAtPredecessor(survivingSuccessor);
        node.safeDelete();
    }

    @SuppressWarnings("static-method")
    public void removeSplitPropagate(ControlSplitNode node, AbstractBeginNode survivingSuccessor)
    {
        List<Node> snapshot = node.successors().snapshot();
        node.clearSuccessors();
        node.replaceAtPredecessor(survivingSuccessor);
        node.safeDelete();
        for (Node successor : snapshot)
        {
            if (successor != null && successor.isAlive())
            {
                if (successor != survivingSuccessor)
                {
                    GraphUtil.killCFG((FixedNode) successor);
                }
            }
        }
    }

    public void replaceSplit(ControlSplitNode node, Node replacement, AbstractBeginNode survivingSuccessor)
    {
        if (replacement instanceof FixedWithNextNode)
        {
            replaceSplitWithFixed(node, (FixedWithNextNode) replacement, survivingSuccessor);
        }
        else
        {
            replaceSplitWithFloating(node, (FloatingNode) replacement, survivingSuccessor);
        }
    }

    @SuppressWarnings("static-method")
    public void replaceSplitWithFixed(ControlSplitNode node, FixedWithNextNode replacement, AbstractBeginNode survivingSuccessor)
    {
        node.clearSuccessors();
        replacement.setNext(survivingSuccessor);
        node.replaceAndDelete(replacement);
    }

    @SuppressWarnings("static-method")
    public void replaceSplitWithFloating(ControlSplitNode node, FloatingNode replacement, AbstractBeginNode survivingSuccessor)
    {
        node.clearSuccessors();
        node.replaceAtPredecessor(survivingSuccessor);
        node.replaceAtUsagesAndDelete(replacement);
    }

    @SuppressWarnings("static-method")
    public void addAfterFixed(FixedWithNextNode node, FixedNode newNode)
    {
        FixedNode next = node.next();
        node.setNext(newNode);
        if (next != null)
        {
            FixedWithNextNode newFixedWithNext = (FixedWithNextNode) newNode;
            newFixedWithNext.setNext(next);
        }
    }

    @SuppressWarnings("static-method")
    public void addBeforeFixed(FixedNode node, FixedWithNextNode newNode)
    {
        FixedWithNextNode pred = (FixedWithNextNode) node.predecessor();
        pred.setNext(newNode);
        newNode.setNext(node);
    }

    public void reduceDegenerateLoopBegin(LoopBeginNode begin)
    {
        if (begin.forwardEndCount() == 1) // bypass merge and remove
        {
            reduceTrivialMerge(begin);
        }
        else // convert to merge
        {
            AbstractMergeNode merge = this.add(new MergeNode());
            for (EndNode end : begin.forwardEnds())
            {
                merge.addForwardEnd(end);
            }
            this.replaceFixedWithFixed(begin, merge);
        }
    }

    @SuppressWarnings("static-method")
    public void reduceTrivialMerge(AbstractMergeNode merge)
    {
        for (PhiNode phi : merge.phis().snapshot())
        {
            ValueNode singleValue = phi.valueAt(0);
            if (phi.hasUsages())
            {
                phi.replaceAtUsagesAndDelete(singleValue);
            }
            else
            {
                phi.safeDelete();
                if (singleValue != null)
                {
                    GraphUtil.tryKillUnused(singleValue);
                }
            }
        }
        // remove loop exits
        if (merge instanceof LoopBeginNode)
        {
            ((LoopBeginNode) merge).removeExits();
        }
        AbstractEndNode singleEnd = merge.forwardEndAt(0);
        FixedNode sux = merge.next();
        FrameState stateAfter = merge.stateAfter();
        // evacuateGuards
        merge.prepareDelete((FixedNode) singleEnd.predecessor());
        merge.safeDelete();
        if (stateAfter != null)
        {
            GraphUtil.tryKillUnused(stateAfter);
        }
        if (sux == null)
        {
            singleEnd.replaceAtPredecessor(null);
            singleEnd.safeDelete();
        }
        else
        {
            singleEnd.replaceAndDelete(sux);
        }
    }

    public GuardsStage getGuardsStage()
    {
        return guardsStage;
    }

    public void setGuardsStage(GuardsStage guardsStage)
    {
        this.guardsStage = guardsStage;
    }

    public boolean isAfterFloatingReadPhase()
    {
        return isAfterFloatingReadPhase;
    }

    public boolean isAfterFixedReadPhase()
    {
        return isAfterFixedReadPhase;
    }

    public void setAfterFloatingReadPhase(boolean state)
    {
        isAfterFloatingReadPhase = state;
    }

    public void setAfterFixReadPhase(boolean state)
    {
        isAfterFixedReadPhase = state;
    }

    public boolean hasValueProxies()
    {
        return hasValueProxies;
    }

    public void setHasValueProxies(boolean state)
    {
        hasValueProxies = state;
    }

    public boolean isAfterExpandLogic()
    {
        return isAfterExpandLogic;
    }

    public void setAfterExpandLogic()
    {
        isAfterExpandLogic = true;
    }

    /**
     * Determines if {@link ProfilingInfo} is used during construction of this graph.
     */
    public boolean useProfilingInfo()
    {
        return useProfilingInfo;
    }

    /**
     * Gets the profiling info for the {@linkplain #method() root method} of this graph.
     */
    public ProfilingInfo getProfilingInfo()
    {
        return getProfilingInfo(method());
    }

    /**
     * Gets the profiling info for a given method that is or will be part of this graph, taking into
     * account {@link #useProfilingInfo()}.
     */
    public ProfilingInfo getProfilingInfo(ResolvedJavaMethod m)
    {
        if (useProfilingInfo && m != null)
        {
            return m.getProfilingInfo();
        }
        else
        {
            return DefaultProfilingInfo.get(TriState.UNKNOWN);
        }
    }

    /**
     * Gets the object for recording assumptions while constructing of this graph.
     *
     * @return {@code null} if assumptions cannot be made for this graph
     */
    public Assumptions getAssumptions()
    {
        return assumptions;
    }

    /**
     * Gets the methods that were inlined while constructing this graph.
     */
    public List<ResolvedJavaMethod> getMethods()
    {
        return methods;
    }

    /**
     * Records that {@code method} was used to build this graph.
     */
    public void recordMethod(ResolvedJavaMethod method)
    {
        methods.add(method);
    }

    /**
     * Updates the {@linkplain #getMethods() methods} used to build this graph with the methods used
     * to build another graph.
     */
    public void updateMethods(StructuredGraph other)
    {
        this.methods.addAll(other.methods);
    }

    /**
     * Gets the fields that were accessed while constructing this graph.
     */
    public EconomicSet<ResolvedJavaField> getFields()
    {
        return fields;
    }

    /**
     * Records that {@code field} was accessed in this graph.
     */
    public void recordField(ResolvedJavaField field)
    {
        if (this.fields == null)
        {
            this.fields = EconomicSet.create(Equivalence.IDENTITY);
        }
        fields.add(field);
    }

    /**
     * Updates the {@linkplain #getFields() fields} of this graph with the accessed fields of
     * another graph.
     */
    public void updateFields(StructuredGraph other)
    {
        if (other.fields != null)
        {
            if (this.fields == null)
            {
                this.fields = EconomicSet.create(Equivalence.IDENTITY);
            }
            this.fields.addAll(other.fields);
        }
    }

    /**
     * Gets the input bytecode {@linkplain ResolvedJavaMethod#getCodeSize() size} from which this
     * graph is constructed. This ignores how many bytecodes in each constituent method are actually
     * parsed (which may be none for methods whose IR is retrieved from a cache or less than the
     * full amount for any given method due to profile guided branch pruning).
     */
    public int getBytecodeSize()
    {
        int res = 0;
        for (ResolvedJavaMethod e : methods)
        {
            res += e.getCodeSize();
        }
        return res;
    }

    /**
     * @return true if the graph contains only a {@link StartNode} and {@link ReturnNode}
     */
    public boolean isTrivial()
    {
        return !(start.next() instanceof ReturnNode);
    }

    public boolean hasUnsafeAccess()
    {
        return hasUnsafeAccess == UnsafeAccessState.HAS_ACCESS;
    }

    public void markUnsafeAccess()
    {
        if (hasUnsafeAccess == UnsafeAccessState.DISABLED)
        {
            return;
        }
        hasUnsafeAccess = UnsafeAccessState.HAS_ACCESS;
    }

    public void disableUnsafeAccessTracking()
    {
        hasUnsafeAccess = UnsafeAccessState.DISABLED;
    }

    public boolean isUnsafeAccessTrackingEnabled()
    {
        return hasUnsafeAccess != UnsafeAccessState.DISABLED;
    }

    public SpeculationLog getSpeculationLog()
    {
        return speculationLog;
    }

    public void clearAllStateAfter()
    {
        for (Node node : getNodes())
        {
            if (node instanceof StateSplit)
            {
                FrameState stateAfter = ((StateSplit) node).stateAfter();
                if (stateAfter != null)
                {
                    ((StateSplit) node).setStateAfter(null);
                    // 2 nodes referencing the same framestate
                    if (stateAfter.isAlive())
                    {
                        GraphUtil.killWithUnusedFloatingInputs(stateAfter);
                    }
                }
            }
        }
    }

    public boolean hasVirtualizableAllocation()
    {
        for (Node n : getNodes())
        {
            if (n instanceof VirtualizableAllocation)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void afterRegister(Node node)
    {
    }
}
