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

///
// A graph that contains at least one distinguished node : the {@link #start() start} node. This
// node is the start of the control flow of the graph.
///
// @class StructuredGraph
public final class StructuredGraph extends Graph
{
    ///
    // The different stages of the compilation of a {@link Graph} regarding the status of
    // {@link GuardNode guards}, {@link DeoptimizingNode deoptimizations} and {@link FrameState
    // framestates}. The stage of a graph progresses monotonously.
    ///
    // @enum StructuredGraph.GuardsStage
    public enum GuardsStage
    {
        ///
        // During this stage, there can be {@link FloatingNode floating} {@link DeoptimizingNode}
        // such as {@link GuardNode GuardNodes}. New {@link DeoptimizingNode DeoptimizingNodes} can
        // be introduced without constraints. {@link FrameState} nodes are associated with
        // {@link StateSplit} nodes.
        ///
        FLOATING_GUARDS,
        ///
        // During this stage, all {@link DeoptimizingNode DeoptimizingNodes} must be
        // {@link FixedNode fixed} but new {@link DeoptimizingNode DeoptimizingNodes} can still be
        // introduced. {@link FrameState} nodes are still associated with {@link StateSplit} nodes.
        ///
        FIXED_DEOPTS,
        ///
        // During this stage, all {@link DeoptimizingNode DeoptimizingNodes} must be
        // {@link FixedNode fixed}. New {@link DeoptimizingNode DeoptimizingNodes} can not be
        // introduced any more. {@link FrameState} nodes are now associated with
        // {@link DeoptimizingNode} nodes.
        ///
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

    ///
    // Constants denoting whether or not {@link Assumption}s can be made while processing a graph.
    ///
    // @enum StructuredGraph.AllowAssumptions
    public enum AllowAssumptions
    {
        YES,
        NO;

        public static StructuredGraph.AllowAssumptions ifTrue(boolean __flag)
        {
            return __flag ? YES : NO;
        }

        public static StructuredGraph.AllowAssumptions ifNonNull(Assumptions __assumptions)
        {
            return __assumptions != null ? YES : NO;
        }
    }

    // @class StructuredGraph.ScheduleResult
    public static final class ScheduleResult
    {
        // @field
        private final ControlFlowGraph ___cfg;
        // @field
        private final NodeMap<Block> ___nodeToBlockMap;
        // @field
        private final BlockMap<List<Node>> ___blockToNodesMap;

        // @cons StructuredGraph.ScheduleResult
        public ScheduleResult(ControlFlowGraph __cfg, NodeMap<Block> __nodeToBlockMap, BlockMap<List<Node>> __blockToNodesMap)
        {
            super();
            this.___cfg = __cfg;
            this.___nodeToBlockMap = __nodeToBlockMap;
            this.___blockToNodesMap = __blockToNodesMap;
        }

        public ControlFlowGraph getCFG()
        {
            return this.___cfg;
        }

        public NodeMap<Block> getNodeToBlockMap()
        {
            return this.___nodeToBlockMap;
        }

        public BlockMap<List<Node>> getBlockToNodesMap()
        {
            return this.___blockToNodesMap;
        }

        public List<Node> nodesFor(Block __block)
        {
            return this.___blockToNodesMap.get(__block);
        }
    }

    ///
    // Object used to create a {@link StructuredGraph}.
    ///
    // @class StructuredGraph.GraphBuilder
    public static final class GraphBuilder
    {
        // @field
        private final Assumptions ___assumptions;
        // @field
        private SpeculationLog ___speculationLog;
        // @field
        private ResolvedJavaMethod ___rootMethod;
        // @field
        private int ___entryBCI = JVMCICompiler.INVOCATION_ENTRY_BCI;
        // @field
        private boolean ___useProfilingInfo = true;

        ///
        // Creates a builder for a graph.
        ///
        // @cons StructuredGraph.GraphBuilder
        public GraphBuilder(StructuredGraph.AllowAssumptions __allowAssumptions)
        {
            super();
            this.___assumptions = __allowAssumptions == StructuredGraph.AllowAssumptions.YES ? new Assumptions() : null;
        }

        ///
        // Creates a builder for a graph that does not support {@link Assumptions}.
        ///
        // @cons StructuredGraph.GraphBuilder
        public GraphBuilder()
        {
            super();
            this.___assumptions = null;
        }

        public ResolvedJavaMethod getMethod()
        {
            return this.___rootMethod;
        }

        public StructuredGraph.GraphBuilder method(ResolvedJavaMethod __method)
        {
            this.___rootMethod = __method;
            return this;
        }

        public SpeculationLog getSpeculationLog()
        {
            return this.___speculationLog;
        }

        public StructuredGraph.GraphBuilder speculationLog(SpeculationLog __log)
        {
            this.___speculationLog = __log;
            return this;
        }

        public int getEntryBCI()
        {
            return this.___entryBCI;
        }

        public StructuredGraph.GraphBuilder entryBCI(int __bci)
        {
            this.___entryBCI = __bci;
            return this;
        }

        public boolean getUseProfilingInfo()
        {
            return this.___useProfilingInfo;
        }

        public StructuredGraph.GraphBuilder useProfilingInfo(boolean __flag)
        {
            this.___useProfilingInfo = __flag;
            return this;
        }

        public StructuredGraph build()
        {
            return new StructuredGraph(this.___rootMethod, this.___entryBCI, this.___assumptions, this.___speculationLog, this.___useProfilingInfo);
        }
    }

    // @def
    public static final long INVALID_GRAPH_ID = -1;
    // @def
    private static final AtomicLong uniqueGraphIds = new AtomicLong();

    // @field
    private StartNode ___start;
    // @field
    private ResolvedJavaMethod ___rootMethod;
    // @field
    private final long ___graphId;
    // @field
    private final int ___entryBCI;
    // @field
    private StructuredGraph.GuardsStage ___guardsStage = StructuredGraph.GuardsStage.FLOATING_GUARDS;
    // @field
    private boolean ___isAfterFloatingReadPhase = false;
    // @field
    private boolean ___isAfterFixedReadPhase = false;
    // @field
    private boolean ___hasValueProxies = true;
    // @field
    private boolean ___isAfterExpandLogic = false;
    // @field
    private final boolean ___useProfilingInfo;
    ///
    // The assumptions made while constructing and transforming this graph.
    ///
    // @field
    private final Assumptions ___assumptions;

    // @field
    private SpeculationLog ___speculationLog;

    // @field
    private StructuredGraph.ScheduleResult ___lastSchedule;

    ///
    // Records the methods that were used while constructing this graph, one entry for each time a
    // specific method is used.
    ///
    // @field
    private final List<ResolvedJavaMethod> ___methods = new ArrayList<>();

    ///
    // Records the fields that were accessed while constructing this graph.
    ///

    // @field
    private EconomicSet<ResolvedJavaField> ___fields = null;

    // @enum StructuredGraph.UnsafeAccessState
    private enum UnsafeAccessState
    {
        NO_ACCESS,
        HAS_ACCESS,
        DISABLED
    }

    // @field
    private StructuredGraph.UnsafeAccessState ___hasUnsafeAccess = StructuredGraph.UnsafeAccessState.NO_ACCESS;

    // @def
    public static final boolean USE_PROFILING_INFO = true;

    // @def
    public static final boolean NO_PROFILING_INFO = false;

    // @cons StructuredGraph
    private StructuredGraph(ResolvedJavaMethod __method, int __entryBCI, Assumptions __assumptions, SpeculationLog __speculationLog, boolean __useProfilingInfo)
    {
        super();
        this.setStart(add(new StartNode()));
        this.___rootMethod = __method;
        this.___graphId = uniqueGraphIds.incrementAndGet();
        this.___entryBCI = __entryBCI;
        this.___assumptions = __assumptions;
        if (__speculationLog != null && !(__speculationLog instanceof GraphSpeculationLog))
        {
            this.___speculationLog = new GraphSpeculationLog(__speculationLog);
        }
        else
        {
            this.___speculationLog = __speculationLog;
        }
        this.___useProfilingInfo = __useProfilingInfo;
    }

    public void setLastSchedule(StructuredGraph.ScheduleResult __result)
    {
        this.___lastSchedule = __result;
    }

    public StructuredGraph.ScheduleResult getLastSchedule()
    {
        return this.___lastSchedule;
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
        return this.___start;
    }

    ///
    // Gets the root method from which this graph was built.
    //
    // @return null if this method was not built from a method or the method is not available
    ///
    public ResolvedJavaMethod method()
    {
        return this.___rootMethod;
    }

    public int getEntryBCI()
    {
        return this.___entryBCI;
    }

    public boolean isOSR()
    {
        return this.___entryBCI != JVMCICompiler.INVOCATION_ENTRY_BCI;
    }

    public long graphId()
    {
        return this.___graphId;
    }

    public void setStart(StartNode __start)
    {
        this.___start = __start;
    }

    ///
    // Creates a copy of this graph.
    ///
    @Override
    public StructuredGraph copy()
    {
        StructuredGraph __copy = new StructuredGraph(method(), this.___entryBCI, this.___assumptions != null ? new Assumptions() : null, this.___speculationLog, this.___useProfilingInfo);
        if (StructuredGraph.AllowAssumptions.ifNonNull(this.___assumptions) == StructuredGraph.AllowAssumptions.YES && this.___assumptions != null)
        {
            __copy.___assumptions.record(this.___assumptions);
        }
        __copy.___hasUnsafeAccess = this.___hasUnsafeAccess;
        __copy.setGuardsStage(getGuardsStage());
        __copy.___isAfterFloatingReadPhase = this.___isAfterFloatingReadPhase;
        __copy.___hasValueProxies = this.___hasValueProxies;
        __copy.___isAfterExpandLogic = this.___isAfterExpandLogic;
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
            private Invoke ___next;

            @Override
            public Iterator<Invoke> iterator()
            {
                // @closure
                return new Iterator<Invoke>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        if (___next == null)
                        {
                            while (__callTargets.hasNext())
                            {
                                Invoke __i = __callTargets.next().invoke();
                                if (__i != null)
                                {
                                    ___next = __i;
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
                            return ___next;
                        }
                        finally
                        {
                            ___next = null;
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

    ///
    // Unlinks a node from all its control flow neighbors and then removes it from its graph. The
    // node must have no {@linkplain Node#usages() usages}.
    //
    // @param node the node to be unlinked and removed
    ///
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
        if (__node == this.___start)
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

    public StructuredGraph.GuardsStage getGuardsStage()
    {
        return this.___guardsStage;
    }

    public void setGuardsStage(StructuredGraph.GuardsStage __guardsStage)
    {
        this.___guardsStage = __guardsStage;
    }

    public boolean isAfterFloatingReadPhase()
    {
        return this.___isAfterFloatingReadPhase;
    }

    public boolean isAfterFixedReadPhase()
    {
        return this.___isAfterFixedReadPhase;
    }

    public void setAfterFloatingReadPhase(boolean __state)
    {
        this.___isAfterFloatingReadPhase = __state;
    }

    public void setAfterFixReadPhase(boolean __state)
    {
        this.___isAfterFixedReadPhase = __state;
    }

    public boolean hasValueProxies()
    {
        return this.___hasValueProxies;
    }

    public void setHasValueProxies(boolean __state)
    {
        this.___hasValueProxies = __state;
    }

    public boolean isAfterExpandLogic()
    {
        return this.___isAfterExpandLogic;
    }

    public void setAfterExpandLogic()
    {
        this.___isAfterExpandLogic = true;
    }

    ///
    // Determines if {@link ProfilingInfo} is used during construction of this graph.
    ///
    public boolean useProfilingInfo()
    {
        return this.___useProfilingInfo;
    }

    ///
    // Gets the profiling info for the {@linkplain #method() root method} of this graph.
    ///
    public ProfilingInfo getProfilingInfo()
    {
        return getProfilingInfo(method());
    }

    ///
    // Gets the profiling info for a given method that is or will be part of this graph, taking into
    // account {@link #useProfilingInfo()}.
    ///
    public ProfilingInfo getProfilingInfo(ResolvedJavaMethod __m)
    {
        if (this.___useProfilingInfo && __m != null)
        {
            return __m.getProfilingInfo();
        }
        else
        {
            return DefaultProfilingInfo.get(TriState.UNKNOWN);
        }
    }

    ///
    // Gets the object for recording assumptions while constructing of this graph.
    //
    // @return {@code null} if assumptions cannot be made for this graph
    ///
    public Assumptions getAssumptions()
    {
        return this.___assumptions;
    }

    ///
    // Gets the methods that were inlined while constructing this graph.
    ///
    public List<ResolvedJavaMethod> getMethods()
    {
        return this.___methods;
    }

    ///
    // Records that {@code method} was used to build this graph.
    ///
    public void recordMethod(ResolvedJavaMethod __method)
    {
        this.___methods.add(__method);
    }

    ///
    // Updates the {@linkplain #getMethods() methods} used to build this graph with the methods used
    // to build another graph.
    ///
    public void updateMethods(StructuredGraph __other)
    {
        this.___methods.addAll(__other.___methods);
    }

    ///
    // Gets the fields that were accessed while constructing this graph.
    ///
    public EconomicSet<ResolvedJavaField> getFields()
    {
        return this.___fields;
    }

    ///
    // Records that {@code field} was accessed in this graph.
    ///
    public void recordField(ResolvedJavaField __field)
    {
        if (this.___fields == null)
        {
            this.___fields = EconomicSet.create(Equivalence.IDENTITY);
        }
        this.___fields.add(__field);
    }

    ///
    // Updates the {@linkplain #getFields() fields} of this graph with the accessed fields of
    // another graph.
    ///
    public void updateFields(StructuredGraph __other)
    {
        if (__other.___fields != null)
        {
            if (this.___fields == null)
            {
                this.___fields = EconomicSet.create(Equivalence.IDENTITY);
            }
            this.___fields.addAll(__other.___fields);
        }
    }

    ///
    // Gets the input bytecode {@linkplain ResolvedJavaMethod#getCodeSize() size} from which this
    // graph is constructed. This ignores how many bytecodes in each constituent method are actually
    // parsed (which may be none for methods whose IR is retrieved from a cache or less than the
    // full amount for any given method due to profile guided branch pruning).
    ///
    public int getBytecodeSize()
    {
        int __res = 0;
        for (ResolvedJavaMethod __e : this.___methods)
        {
            __res += __e.getCodeSize();
        }
        return __res;
    }

    ///
    // @return true if the graph contains only a {@link StartNode} and {@link ReturnNode}
    ///
    public boolean isTrivial()
    {
        return !(this.___start.next() instanceof ReturnNode);
    }

    public boolean hasUnsafeAccess()
    {
        return this.___hasUnsafeAccess == StructuredGraph.UnsafeAccessState.HAS_ACCESS;
    }

    public void markUnsafeAccess()
    {
        if (this.___hasUnsafeAccess == StructuredGraph.UnsafeAccessState.DISABLED)
        {
            return;
        }
        this.___hasUnsafeAccess = StructuredGraph.UnsafeAccessState.HAS_ACCESS;
    }

    public void disableUnsafeAccessTracking()
    {
        this.___hasUnsafeAccess = StructuredGraph.UnsafeAccessState.DISABLED;
    }

    public boolean isUnsafeAccessTrackingEnabled()
    {
        return this.___hasUnsafeAccess != StructuredGraph.UnsafeAccessState.DISABLED;
    }

    public SpeculationLog getSpeculationLog()
    {
        return this.___speculationLog;
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
