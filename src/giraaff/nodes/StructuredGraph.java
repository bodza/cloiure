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
        public static AllowAssumptions ifTrue(boolean __flag)
        {
            return __flag ? YES : NO;
        }

        public static AllowAssumptions ifNonNull(Assumptions __assumptions)
        {
            return __assumptions != null ? YES : NO;
        }
    }

    // @class StructuredGraph.ScheduleResult
    public static final class ScheduleResult
    {
        // @field
        private final ControlFlowGraph cfg;
        // @field
        private final NodeMap<Block> nodeToBlockMap;
        // @field
        private final BlockMap<List<Node>> blockToNodesMap;

        // @cons
        public ScheduleResult(ControlFlowGraph __cfg, NodeMap<Block> __nodeToBlockMap, BlockMap<List<Node>> __blockToNodesMap)
        {
            super();
            this.cfg = __cfg;
            this.nodeToBlockMap = __nodeToBlockMap;
            this.blockToNodesMap = __blockToNodesMap;
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

        public List<Node> nodesFor(Block __block)
        {
            return blockToNodesMap.get(__block);
        }
    }

    /**
     * Object used to create a {@link StructuredGraph}.
     */
    // @class StructuredGraph.Builder
    public static final class Builder
    {
        // @field
        private final Assumptions assumptions;
        // @field
        private SpeculationLog speculationLog;
        // @field
        private ResolvedJavaMethod rootMethod;
        // @field
        private int entryBCI = JVMCICompiler.INVOCATION_ENTRY_BCI;
        // @field
        private boolean useProfilingInfo = true;

        /**
         * Creates a builder for a graph.
         */
        // @cons
        public Builder(AllowAssumptions __allowAssumptions)
        {
            super();
            this.assumptions = __allowAssumptions == AllowAssumptions.YES ? new Assumptions() : null;
        }

        /**
         * Creates a builder for a graph that does not support {@link Assumptions}.
         */
        // @cons
        public Builder()
        {
            super();
            this.assumptions = null;
        }

        public ResolvedJavaMethod getMethod()
        {
            return rootMethod;
        }

        public Builder method(ResolvedJavaMethod __method)
        {
            this.rootMethod = __method;
            return this;
        }

        public SpeculationLog getSpeculationLog()
        {
            return speculationLog;
        }

        public Builder speculationLog(SpeculationLog __log)
        {
            this.speculationLog = __log;
            return this;
        }

        public int getEntryBCI()
        {
            return entryBCI;
        }

        public Builder entryBCI(int __bci)
        {
            this.entryBCI = __bci;
            return this;
        }

        public boolean getUseProfilingInfo()
        {
            return useProfilingInfo;
        }

        public Builder useProfilingInfo(boolean __flag)
        {
            this.useProfilingInfo = __flag;
            return this;
        }

        public StructuredGraph build()
        {
            return new StructuredGraph(rootMethod, entryBCI, assumptions, speculationLog, useProfilingInfo);
        }
    }

    // @def
    public static final long INVALID_GRAPH_ID = -1;
    // @def
    private static final AtomicLong uniqueGraphIds = new AtomicLong();

    // @field
    private StartNode start;
    // @field
    private ResolvedJavaMethod rootMethod;
    // @field
    private final long graphId;
    // @field
    private final int entryBCI;
    // @field
    private GuardsStage guardsStage = GuardsStage.FLOATING_GUARDS;
    // @field
    private boolean isAfterFloatingReadPhase = false;
    // @field
    private boolean isAfterFixedReadPhase = false;
    // @field
    private boolean hasValueProxies = true;
    // @field
    private boolean isAfterExpandLogic = false;
    // @field
    private final boolean useProfilingInfo;
    /**
     * The assumptions made while constructing and transforming this graph.
     */
    // @field
    private final Assumptions assumptions;

    // @field
    private SpeculationLog speculationLog;

    // @field
    private ScheduleResult lastSchedule;

    /**
     * Records the methods that were used while constructing this graph, one entry for each time a
     * specific method is used.
     */
    // @field
    private final List<ResolvedJavaMethod> methods = new ArrayList<>();

    /**
     * Records the fields that were accessed while constructing this graph.
     */

    // @field
    private EconomicSet<ResolvedJavaField> fields = null;

    // @enum StructuredGraph.UnsafeAccessState
    private enum UnsafeAccessState
    {
        NO_ACCESS,
        HAS_ACCESS,
        DISABLED
    }

    // @field
    private UnsafeAccessState hasUnsafeAccess = UnsafeAccessState.NO_ACCESS;

    // @def
    public static final boolean USE_PROFILING_INFO = true;

    // @def
    public static final boolean NO_PROFILING_INFO = false;

    // @cons
    private StructuredGraph(ResolvedJavaMethod __method, int __entryBCI, Assumptions __assumptions, SpeculationLog __speculationLog, boolean __useProfilingInfo)
    {
        super();
        this.setStart(add(new StartNode()));
        this.rootMethod = __method;
        this.graphId = uniqueGraphIds.incrementAndGet();
        this.entryBCI = __entryBCI;
        this.assumptions = __assumptions;
        if (__speculationLog != null && !(__speculationLog instanceof GraphSpeculationLog))
        {
            this.speculationLog = new GraphSpeculationLog(__speculationLog);
        }
        else
        {
            this.speculationLog = __speculationLog;
        }
        this.useProfilingInfo = __useProfilingInfo;
    }

    public void setLastSchedule(ScheduleResult __result)
    {
        lastSchedule = __result;
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
        Stamp __returnStamp = null;
        for (ReturnNode __returnNode : getNodes(ReturnNode.TYPE))
        {
            ValueNode __result = __returnNode.result();
            if (__result != null)
            {
                if (__returnStamp == null)
                {
                    __returnStamp = __result.stamp(NodeView.DEFAULT);
                }
                else
                {
                    __returnStamp = __returnStamp.meet(__result.stamp(NodeView.DEFAULT));
                }
            }
        }
        return __returnStamp;
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

    public void setStart(StartNode __start)
    {
        this.start = __start;
    }

    /**
     * Creates a copy of this graph.
     */
    @Override
    public StructuredGraph copy()
    {
        StructuredGraph __copy = new StructuredGraph(method(), entryBCI, assumptions != null ? new Assumptions() : null, speculationLog, useProfilingInfo);
        if (AllowAssumptions.ifNonNull(assumptions) == AllowAssumptions.YES && assumptions != null)
        {
            __copy.assumptions.record(assumptions);
        }
        __copy.hasUnsafeAccess = hasUnsafeAccess;
        __copy.setGuardsStage(getGuardsStage());
        __copy.isAfterFloatingReadPhase = isAfterFloatingReadPhase;
        __copy.hasValueProxies = hasValueProxies;
        __copy.isAfterExpandLogic = isAfterExpandLogic;
        return __copy;
    }

    public ParameterNode getParameter(int __index)
    {
        for (ParameterNode __param : getNodes(ParameterNode.TYPE))
        {
            if (__param.index() == __index)
            {
                return __param;
            }
        }
        return null;
    }

    public Iterable<Invoke> getInvokes()
    {
        final Iterator<MethodCallTargetNode> __callTargets = getNodes(MethodCallTargetNode.TYPE).iterator();
        // @closure
        return new Iterable<Invoke>()
        {
            // @field
            private Invoke next;

            @Override
            public Iterator<Invoke> iterator()
            {
                // @closure
                return new Iterator<Invoke>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        if (next == null)
                        {
                            while (__callTargets.hasNext())
                            {
                                Invoke __i = __callTargets.next().invoke();
                                if (__i != null)
                                {
                                    next = __i;
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
    public void removeFixed(FixedWithNextNode __node)
    {
        if (__node instanceof AbstractBeginNode)
        {
            ((AbstractBeginNode) __node).prepareDelete();
        }
        GraphUtil.unlinkFixedNode(__node);
        __node.safeDelete();
    }

    public void replaceFixed(FixedWithNextNode __node, Node __replacement)
    {
        if (__replacement instanceof FixedWithNextNode)
        {
            replaceFixedWithFixed(__node, (FixedWithNextNode) __replacement);
        }
        else
        {
            replaceFixedWithFloating(__node, (FloatingNode) __replacement);
        }
    }

    public void replaceFixedWithFixed(FixedWithNextNode __node, FixedWithNextNode __replacement)
    {
        FixedNode __next = __node.next();
        __node.setNext(null);
        __replacement.setNext(__next);
        __node.replaceAndDelete(__replacement);
        if (__node == start)
        {
            setStart((StartNode) __replacement);
        }
    }

    @SuppressWarnings("static-method")
    public void replaceFixedWithFloating(FixedWithNextNode __node, ValueNode __replacement)
    {
        GraphUtil.unlinkFixedNode(__node);
        __node.replaceAtUsagesAndDelete(__replacement);
    }

    @SuppressWarnings("static-method")
    public void removeSplit(ControlSplitNode __node, AbstractBeginNode __survivingSuccessor)
    {
        __node.clearSuccessors();
        __node.replaceAtPredecessor(__survivingSuccessor);
        __node.safeDelete();
    }

    @SuppressWarnings("static-method")
    public void removeSplitPropagate(ControlSplitNode __node, AbstractBeginNode __survivingSuccessor)
    {
        List<Node> __snapshot = __node.successors().snapshot();
        __node.clearSuccessors();
        __node.replaceAtPredecessor(__survivingSuccessor);
        __node.safeDelete();
        for (Node __successor : __snapshot)
        {
            if (__successor != null && __successor.isAlive())
            {
                if (__successor != __survivingSuccessor)
                {
                    GraphUtil.killCFG((FixedNode) __successor);
                }
            }
        }
    }

    public void replaceSplit(ControlSplitNode __node, Node __replacement, AbstractBeginNode __survivingSuccessor)
    {
        if (__replacement instanceof FixedWithNextNode)
        {
            replaceSplitWithFixed(__node, (FixedWithNextNode) __replacement, __survivingSuccessor);
        }
        else
        {
            replaceSplitWithFloating(__node, (FloatingNode) __replacement, __survivingSuccessor);
        }
    }

    @SuppressWarnings("static-method")
    public void replaceSplitWithFixed(ControlSplitNode __node, FixedWithNextNode __replacement, AbstractBeginNode __survivingSuccessor)
    {
        __node.clearSuccessors();
        __replacement.setNext(__survivingSuccessor);
        __node.replaceAndDelete(__replacement);
    }

    @SuppressWarnings("static-method")
    public void replaceSplitWithFloating(ControlSplitNode __node, FloatingNode __replacement, AbstractBeginNode __survivingSuccessor)
    {
        __node.clearSuccessors();
        __node.replaceAtPredecessor(__survivingSuccessor);
        __node.replaceAtUsagesAndDelete(__replacement);
    }

    @SuppressWarnings("static-method")
    public void addAfterFixed(FixedWithNextNode __node, FixedNode __newNode)
    {
        FixedNode __next = __node.next();
        __node.setNext(__newNode);
        if (__next != null)
        {
            FixedWithNextNode __newFixedWithNext = (FixedWithNextNode) __newNode;
            __newFixedWithNext.setNext(__next);
        }
    }

    @SuppressWarnings("static-method")
    public void addBeforeFixed(FixedNode __node, FixedWithNextNode __newNode)
    {
        FixedWithNextNode __pred = (FixedWithNextNode) __node.predecessor();
        __pred.setNext(__newNode);
        __newNode.setNext(__node);
    }

    public void reduceDegenerateLoopBegin(LoopBeginNode __begin)
    {
        if (__begin.forwardEndCount() == 1) // bypass merge and remove
        {
            reduceTrivialMerge(__begin);
        }
        else // convert to merge
        {
            AbstractMergeNode __merge = this.add(new MergeNode());
            for (EndNode __end : __begin.forwardEnds())
            {
                __merge.addForwardEnd(__end);
            }
            this.replaceFixedWithFixed(__begin, __merge);
        }
    }

    @SuppressWarnings("static-method")
    public void reduceTrivialMerge(AbstractMergeNode __merge)
    {
        for (PhiNode __phi : __merge.phis().snapshot())
        {
            ValueNode __singleValue = __phi.valueAt(0);
            if (__phi.hasUsages())
            {
                __phi.replaceAtUsagesAndDelete(__singleValue);
            }
            else
            {
                __phi.safeDelete();
                if (__singleValue != null)
                {
                    GraphUtil.tryKillUnused(__singleValue);
                }
            }
        }
        // remove loop exits
        if (__merge instanceof LoopBeginNode)
        {
            ((LoopBeginNode) __merge).removeExits();
        }
        AbstractEndNode __singleEnd = __merge.forwardEndAt(0);
        FixedNode __sux = __merge.next();
        FrameState __stateAfter = __merge.stateAfter();
        // evacuateGuards
        __merge.prepareDelete((FixedNode) __singleEnd.predecessor());
        __merge.safeDelete();
        if (__stateAfter != null)
        {
            GraphUtil.tryKillUnused(__stateAfter);
        }
        if (__sux == null)
        {
            __singleEnd.replaceAtPredecessor(null);
            __singleEnd.safeDelete();
        }
        else
        {
            __singleEnd.replaceAndDelete(__sux);
        }
    }

    public GuardsStage getGuardsStage()
    {
        return guardsStage;
    }

    public void setGuardsStage(GuardsStage __guardsStage)
    {
        this.guardsStage = __guardsStage;
    }

    public boolean isAfterFloatingReadPhase()
    {
        return isAfterFloatingReadPhase;
    }

    public boolean isAfterFixedReadPhase()
    {
        return isAfterFixedReadPhase;
    }

    public void setAfterFloatingReadPhase(boolean __state)
    {
        isAfterFloatingReadPhase = __state;
    }

    public void setAfterFixReadPhase(boolean __state)
    {
        isAfterFixedReadPhase = __state;
    }

    public boolean hasValueProxies()
    {
        return hasValueProxies;
    }

    public void setHasValueProxies(boolean __state)
    {
        hasValueProxies = __state;
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
    public ProfilingInfo getProfilingInfo(ResolvedJavaMethod __m)
    {
        if (useProfilingInfo && __m != null)
        {
            return __m.getProfilingInfo();
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
    public void recordMethod(ResolvedJavaMethod __method)
    {
        methods.add(__method);
    }

    /**
     * Updates the {@linkplain #getMethods() methods} used to build this graph with the methods used
     * to build another graph.
     */
    public void updateMethods(StructuredGraph __other)
    {
        this.methods.addAll(__other.methods);
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
    public void recordField(ResolvedJavaField __field)
    {
        if (this.fields == null)
        {
            this.fields = EconomicSet.create(Equivalence.IDENTITY);
        }
        fields.add(__field);
    }

    /**
     * Updates the {@linkplain #getFields() fields} of this graph with the accessed fields of
     * another graph.
     */
    public void updateFields(StructuredGraph __other)
    {
        if (__other.fields != null)
        {
            if (this.fields == null)
            {
                this.fields = EconomicSet.create(Equivalence.IDENTITY);
            }
            this.fields.addAll(__other.fields);
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
        int __res = 0;
        for (ResolvedJavaMethod __e : methods)
        {
            __res += __e.getCodeSize();
        }
        return __res;
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
        for (Node __node : getNodes())
        {
            if (__node instanceof StateSplit)
            {
                FrameState __stateAfter = ((StateSplit) __node).stateAfter();
                if (__stateAfter != null)
                {
                    ((StateSplit) __node).setStateAfter(null);
                    // 2 nodes referencing the same framestate
                    if (__stateAfter.isAlive())
                    {
                        GraphUtil.killWithUnusedFloatingInputs(__stateAfter);
                    }
                }
            }
        }
    }

    public boolean hasVirtualizableAllocation()
    {
        for (Node __n : getNodes())
        {
            if (__n instanceof VirtualizableAllocation)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void afterRegister(Node __node)
    {
    }
}
