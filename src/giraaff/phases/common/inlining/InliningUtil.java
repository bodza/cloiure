package giraaff.phases.common.inlining;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.UnmodifiableEconomicMap;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Graph.DuplicationReplacement;
import giraaff.graph.Graph.Mark;
import giraaff.graph.Graph.NodeEventScope;
import giraaff.graph.Node;
import giraaff.graph.NodeInputList;
import giraaff.graph.NodeMap;
import giraaff.graph.NodeWorkList;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.Invoke;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.KillingBeginNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.ReturnNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.GuardsStage;
import giraaff.nodes.UnwindNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.java.ExceptionObjectNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.java.MonitorExitNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.spi.Replacements;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.util.ValueMergeUtil;
import giraaff.util.GraalError;

// @class InliningUtil
public final class InliningUtil extends ValueMergeUtil
{
    // @cons
    private InliningUtil()
    {
        super();
    }

    public static void replaceInvokeCallTarget(Invoke __invoke, StructuredGraph __graph, InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod)
    {
        MethodCallTargetNode __oldCallTarget = (MethodCallTargetNode) __invoke.callTarget();
        MethodCallTargetNode __newCallTarget = __graph.add(new MethodCallTargetNode(__invokeKind, __targetMethod, __oldCallTarget.arguments().toArray(new ValueNode[0]), __oldCallTarget.returnStamp(), __oldCallTarget.getProfile()));
        __invoke.asNode().replaceFirstInput(__oldCallTarget, __newCallTarget);
    }

    public static PiNode createAnchoredReceiver(StructuredGraph __graph, GuardingNode __anchor, ResolvedJavaType __commonType, ValueNode __receiver, boolean __exact)
    {
        return createAnchoredReceiver(__graph, __anchor, __receiver, __exact ? StampFactory.objectNonNull(TypeReference.createExactTrusted(__commonType)) : StampFactory.objectNonNull(TypeReference.createTrusted(__graph.getAssumptions(), __commonType)));
    }

    private static PiNode createAnchoredReceiver(StructuredGraph __graph, GuardingNode __anchor, ValueNode __receiver, Stamp __stamp)
    {
        // to avoid that floating reads on receiver fields float above the type check
        return __graph.unique(new PiNode(__receiver, __stamp, (ValueNode) __anchor));
    }

    ///
    // @return null iff the check succeeds, otherwise a (non-null) descriptive message.
    ///
    public static String checkInvokeConditions(Invoke __invoke)
    {
        if (__invoke.predecessor() == null || !__invoke.asNode().isAlive())
        {
            return "the invoke is dead code";
        }
        if (!(__invoke.callTarget() instanceof MethodCallTargetNode))
        {
            return "the invoke has already been lowered, or has been created as a low-level node";
        }
        MethodCallTargetNode __callTarget = (MethodCallTargetNode) __invoke.callTarget();
        if (__callTarget.targetMethod() == null)
        {
            return "target method is null";
        }
        if (!__invoke.useForInlining())
        {
            return "the invoke is marked to be not used for inlining";
        }
        ValueNode __receiver = __callTarget.receiver();
        if (__receiver != null && __receiver.isConstant() && __receiver.isNullConstant())
        {
            return "receiver is null";
        }
        return null;
    }

    ///
    // Performs an actual inlining, thereby replacing the given invoke with the given
    // {@code inlineGraph}.
    //
    // @param invoke the invoke that will be replaced
    // @param inlineGraph the graph that the invoke will be replaced with
    // @param receiverNullCheck true if a null check needs to be generated for non-static inlinings,
    //            false if no such check is required
    // @param inlineeMethod the actual method being inlined. Maybe be null for snippets.
    ///
    public static UnmodifiableEconomicMap<Node, Node> inline(Invoke __invoke, StructuredGraph __inlineGraph, boolean __receiverNullCheck, ResolvedJavaMethod __inlineeMethod)
    {
        return inline(__invoke, __inlineGraph, __receiverNullCheck, __inlineeMethod, "reason not specified", "phase not specified");
    }

    ///
    // Performs an actual inlining, thereby replacing the given invoke with the given
    // {@code inlineGraph}.
    //
    // @param invoke the invoke that will be replaced
    // @param inlineGraph the graph that the invoke will be replaced with
    // @param receiverNullCheck true if a null check needs to be generated for non-static inlinings,
    //            false if no such check is required
    // @param inlineeMethod the actual method being inlined. Maybe be null for snippets.
    // @param reason the reason for inlining, used in tracing
    // @param phase the phase that invoked inlining
    ///
    public static UnmodifiableEconomicMap<Node, Node> inline(Invoke __invoke, StructuredGraph __inlineGraph, boolean __receiverNullCheck, ResolvedJavaMethod __inlineeMethod, String __reason, String __phase)
    {
        FixedNode __invokeNode = __invoke.asNode();
        StructuredGraph __graph = __invokeNode.graph();
        final NodeInputList<ValueNode> __parameters = __invoke.callTarget().arguments();

        if (__receiverNullCheck && !((MethodCallTargetNode) __invoke.callTarget()).isStatic())
        {
            nonNullReceiver(__invoke);
        }

        ArrayList<Node> __nodes = new ArrayList<>(__inlineGraph.getNodes().count());
        ArrayList<ReturnNode> __returnNodes = new ArrayList<>(4);
        ArrayList<Invoke> __partialIntrinsicExits = new ArrayList<>();
        UnwindNode __unwindNode = null;
        final StartNode __entryPointNode = __inlineGraph.start();
        FixedNode __firstCFGNode = __entryPointNode.next();
        if (__firstCFGNode == null)
        {
            throw new IllegalStateException("Inlined graph is in invalid state: " + __inlineGraph);
        }
        for (Node __node : __inlineGraph.getNodes())
        {
            if (__node == __entryPointNode || (__node == __entryPointNode.stateAfter() && __node.usages().count() == 1) || __node instanceof ParameterNode)
            {
                // Do nothing.
            }
            else
            {
                __nodes.add(__node);
                if (__node instanceof ReturnNode)
                {
                    __returnNodes.add((ReturnNode) __node);
                }
                else if (__node instanceof Invoke)
                {
                    Invoke __invokeInInlineGraph = (Invoke) __node;
                    if (__invokeInInlineGraph.bci() == BytecodeFrame.UNKNOWN_BCI)
                    {
                        ResolvedJavaMethod __target1 = __inlineeMethod;
                        ResolvedJavaMethod __target2 = __invokeInInlineGraph.callTarget().targetMethod();
                        __partialIntrinsicExits.add(__invokeInInlineGraph);
                    }
                }
                else if (__node instanceof UnwindNode)
                {
                    __unwindNode = (UnwindNode) __node;
                }
            }
        }

        final AbstractBeginNode __prevBegin = AbstractBeginNode.prevBegin(__invokeNode);
        // @closure
        DuplicationReplacement localReplacement = new DuplicationReplacement()
        {
            @Override
            public Node replacement(Node __node)
            {
                if (__node instanceof ParameterNode)
                {
                    return __parameters.get(((ParameterNode) __node).index());
                }
                else if (__node == __entryPointNode)
                {
                    return __prevBegin;
                }
                return __node;
            }
        };

        Mark __mark = __graph.getMark();
        // Instead, attach the inlining log of the child graph to the current inlining log.
        EconomicMap<Node, Node> __duplicates = __graph.addDuplicates(__nodes, __inlineGraph, __inlineGraph.getNodeCount(), localReplacement);

        FrameState __stateAfter = __invoke.stateAfter();

        FrameState __stateAtExceptionEdge = null;
        if (__invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode __invokeWithException = ((InvokeWithExceptionNode) __invoke);
            if (__unwindNode != null)
            {
                ExceptionObjectNode __obj = (ExceptionObjectNode) __invokeWithException.exceptionEdge();
                __stateAtExceptionEdge = __obj.stateAfter();
            }
        }

        if (__stateAfter != null)
        {
            processFrameStates(__invoke, __inlineGraph, __duplicates, __stateAtExceptionEdge, __returnNodes.size() > 1);
            int __callerLockDepth = __stateAfter.nestedLockDepth();
            if (__callerLockDepth != 0)
            {
                for (MonitorIdNode __original : __inlineGraph.getNodes(MonitorIdNode.TYPE))
                {
                    MonitorIdNode __monitor = (MonitorIdNode) __duplicates.get(__original);
                    processMonitorId(__invoke.stateAfter(), __monitor);
                }
            }
        }

        __firstCFGNode = (FixedNode) __duplicates.get(__firstCFGNode);
        for (int __i = 0; __i < __returnNodes.size(); __i++)
        {
            __returnNodes.set(__i, (ReturnNode) __duplicates.get(__returnNodes.get(__i)));
        }
        for (Invoke __exit : __partialIntrinsicExits)
        {
            // A partial intrinsic exit must be replaced with a call to the intrinsified method.
            Invoke __dup = (Invoke) __duplicates.get(__exit.asNode());
            if (__dup instanceof InvokeNode)
            {
                InvokeNode __repl = __graph.add(new InvokeNode(__invoke.callTarget(), __invoke.bci()));
                __dup.intrinsify(__repl.asNode());
            }
            else
            {
                ((InvokeWithExceptionNode) __dup).replaceWithNewBci(__invoke.bci());
            }
        }
        if (__unwindNode != null)
        {
            __unwindNode = (UnwindNode) __duplicates.get(__unwindNode);
        }

        finishInlining(__invoke, __graph, __firstCFGNode, __returnNodes, __unwindNode, __inlineGraph.getAssumptions(), __inlineGraph);
        GraphUtil.killCFG(__invokeNode);

        return __duplicates;
    }

    ///
    // Inline {@code inlineGraph} into the current replacing the node {@code Invoke} and return the
    // set of nodes which should be canonicalized. The set should only contain nodes which modified by
    // the inlining since the current graph and {@code inlineGraph} are expected to already be canonical.
    //
    // @return the set of nodes to canonicalize
    ///
    public static EconomicSet<Node> inlineForCanonicalization(Invoke __invoke, StructuredGraph __inlineGraph, boolean __receiverNullCheck, ResolvedJavaMethod __inlineeMethod, String __reason, String __phase)
    {
        return inlineForCanonicalization(__invoke, __inlineGraph, __receiverNullCheck, __inlineeMethod, null, __reason, __phase);
    }

    @SuppressWarnings("try")
    public static EconomicSet<Node> inlineForCanonicalization(Invoke __invoke, StructuredGraph __inlineGraph, boolean __receiverNullCheck, ResolvedJavaMethod __inlineeMethod, Consumer<UnmodifiableEconomicMap<Node, Node>> __duplicatesConsumer, String __reason, String __phase)
    {
        HashSetNodeEventListener __listener = new HashSetNodeEventListener();
        // This code assumes that Graph.addDuplicates doesn't trigger the NodeEventListener to track
        // only nodes which were modified into the process of inlining the graph into the current graph.
        try (NodeEventScope __nes = __invoke.asNode().graph().trackNodeEvents(__listener))
        {
            UnmodifiableEconomicMap<Node, Node> __duplicates = InliningUtil.inline(__invoke, __inlineGraph, __receiverNullCheck, __inlineeMethod, __reason, __phase);
            if (__duplicatesConsumer != null)
            {
                __duplicatesConsumer.accept(__duplicates);
            }
        }
        return __listener.getNodes();
    }

    private static ValueNode finishInlining(Invoke __invoke, StructuredGraph __graph, FixedNode __firstNode, List<ReturnNode> __returnNodes, UnwindNode __unwindNode, Assumptions __inlinedAssumptions, StructuredGraph __inlineGraph)
    {
        FixedNode __invokeNode = __invoke.asNode();
        FrameState __stateAfter = __invoke.stateAfter();

        __invokeNode.replaceAtPredecessor(__firstNode);

        if (__invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode __invokeWithException = ((InvokeWithExceptionNode) __invoke);
            if (__unwindNode != null && __unwindNode.isAlive())
            {
                ExceptionObjectNode __obj = (ExceptionObjectNode) __invokeWithException.exceptionEdge();
                __obj.replaceAtUsages(__unwindNode.exception());
                Node __n = __obj.next();
                __obj.setNext(null);
                __unwindNode.replaceAndDelete(__n);

                __obj.replaceAtPredecessor(null);
                __obj.safeDelete();
            }
            else
            {
                __invokeWithException.killExceptionEdge();
            }

            // get rid of memory kill
            AbstractBeginNode __begin = __invokeWithException.next();
            if (__begin instanceof KillingBeginNode)
            {
                AbstractBeginNode __newBegin = new BeginNode();
                __graph.addAfterFixed(__begin, __graph.add(__newBegin));
                __begin.replaceAtUsages(__newBegin);
                __graph.removeFixed(__begin);
            }
        }
        else
        {
            if (__unwindNode != null && __unwindNode.isAlive())
            {
                DeoptimizeNode __deoptimizeNode = addDeoptimizeNode(__graph, DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NotCompiledExceptionHandler);
                __unwindNode.replaceAndDelete(__deoptimizeNode);
            }
        }

        ValueNode __returnValue;
        if (!__returnNodes.isEmpty())
        {
            FixedNode __n = __invoke.next();
            __invoke.setNext(null);
            if (__returnNodes.size() == 1)
            {
                ReturnNode __returnNode = __returnNodes.get(0);
                __returnValue = __returnNode.result();
                __invokeNode.replaceAtUsages(__returnValue);
                __returnNode.replaceAndDelete(__n);
            }
            else
            {
                MergeNode __merge = __graph.add(new MergeNode());
                __merge.setStateAfter(__stateAfter);
                __returnValue = mergeReturns(__merge, __returnNodes);
                __invokeNode.replaceAtUsages(__returnValue);
                if (__merge.isPhiAtMerge(__returnValue))
                {
                    fixFrameStates(__graph, __merge, (PhiNode) __returnValue);
                }
                __merge.setNext(__n);
            }
        }
        else
        {
            __returnValue = null;
            __invokeNode.replaceAtUsages(null);
            GraphUtil.killCFG(__invoke.next());
        }

        // copy assumptions from inlinee to caller
        Assumptions __assumptions = __graph.getAssumptions();
        if (__assumptions != null)
        {
            if (__inlinedAssumptions != null)
            {
                __assumptions.record(__inlinedAssumptions);
            }
        }

        // copy inlined methods from inlinee to caller
        __graph.updateMethods(__inlineGraph);

        if (__inlineGraph.hasUnsafeAccess())
        {
            __graph.markUnsafeAccess();
        }

        return __returnValue;
    }

    private static void fixFrameStates(StructuredGraph __graph, MergeNode __originalMerge, PhiNode __returnPhi)
    {
        // It is possible that some of the frame states that came from AFTER_BCI reference a Phi node
        // that was created to merge multiple returns. This can create cycles (see GR-3949 and GR-3957).
        // To detect this, we follow the control paths starting from the merge node,
        // split the Phi node inputs at merges and assign the proper input to each frame state.
        NodeMap<Node> __seen = new NodeMap<>(__graph);
        ArrayDeque<Node> __workList = new ArrayDeque<>();
        ArrayDeque<ValueNode> __valueList = new ArrayDeque<>();
        __workList.push(__originalMerge);
        __valueList.push(__returnPhi);
        while (!__workList.isEmpty())
        {
            Node __current = __workList.pop();
            ValueNode __currentValue = __valueList.pop();
            if (__seen.containsKey(__current))
            {
                continue;
            }
            __seen.put(__current, __current);
            if (__current instanceof StateSplit && __current != __originalMerge)
            {
                StateSplit __stateSplit = (StateSplit) __current;
                FrameState __state = __stateSplit.stateAfter();
                if (__state != null && __state.values().contains(__returnPhi))
                {
                    int __index = 0;
                    FrameState __duplicate = __state.duplicate();
                    for (ValueNode __value : __state.values())
                    {
                        if (__value == __returnPhi)
                        {
                            __duplicate.values().set(__index, __currentValue);
                        }
                        __index++;
                    }
                    __stateSplit.setStateAfter(__duplicate);
                    GraphUtil.tryKillUnused(__state);
                }
            }
            if (__current instanceof AbstractMergeNode)
            {
                AbstractMergeNode __currentMerge = (AbstractMergeNode) __current;
                for (EndNode __pred : __currentMerge.cfgPredecessors())
                {
                    ValueNode __newValue = __currentValue;
                    if (__currentMerge.isPhiAtMerge(__currentValue))
                    {
                        PhiNode __currentPhi = (PhiNode) __currentValue;
                        __newValue = __currentPhi.valueAt(__pred);
                    }
                    __workList.push(__pred);
                    __valueList.push(__newValue);
                }
            }
            else if (__current.predecessor() != null)
            {
                __workList.push(__current.predecessor());
                __valueList.push(__currentValue);
            }
        }
    }

    public static void processMonitorId(FrameState __stateAfter, MonitorIdNode __monitorIdNode)
    {
        if (__stateAfter != null)
        {
            int __callerLockDepth = __stateAfter.nestedLockDepth();
            __monitorIdNode.setLockDepth(__monitorIdNode.getLockDepth() + __callerLockDepth);
        }
    }

    protected static void processFrameStates(Invoke __invoke, StructuredGraph __inlineGraph, EconomicMap<Node, Node> __duplicates, FrameState __stateAtExceptionEdge, boolean __alwaysDuplicateStateAfter)
    {
        FrameState __stateAtReturn = __invoke.stateAfter();
        FrameState __outerFrameState = null;
        JavaKind __invokeReturnKind = __invoke.asNode().getStackKind();
        EconomicMap<Node, Node> __replacements = EconomicMap.create();
        for (FrameState __original : __inlineGraph.getNodes(FrameState.TYPE))
        {
            FrameState __frameState = (FrameState) __duplicates.get(__original);
            if (__frameState != null && __frameState.isAlive())
            {
                if (__outerFrameState == null)
                {
                    __outerFrameState = __stateAtReturn.duplicateModifiedDuringCall(__invoke.bci(), __invokeReturnKind);
                }
                processFrameState(__frameState, __invoke, __replacements, __inlineGraph.method(), __stateAtExceptionEdge, __outerFrameState, __alwaysDuplicateStateAfter, __invoke.callTarget().targetMethod(), __invoke.callTarget().arguments());
            }
        }
        // If processing the frame states replaced any nodes, update the duplicates map.
        __duplicates.replaceAll((__key, __value) -> __replacements.containsKey(__value) ? __replacements.get(__value) : __value);
    }

    public static FrameState processFrameState(FrameState __frameState, Invoke __invoke, EconomicMap<Node, Node> __replacements, ResolvedJavaMethod __inlinedMethod, FrameState __stateAtExceptionEdge, FrameState __outerFrameState, boolean __alwaysDuplicateStateAfter, ResolvedJavaMethod __invokeTargetMethod, List<ValueNode> __invokeArgsList)
    {
        final FrameState __stateAtReturn = __invoke.stateAfter();
        JavaKind __invokeReturnKind = __invoke.asNode().getStackKind();

        if (__frameState.___bci == BytecodeFrame.AFTER_BCI)
        {
            return handleAfterBciFrameState(__frameState, __invoke, __alwaysDuplicateStateAfter);
        }
        else if (__stateAtExceptionEdge != null && isStateAfterException(__frameState))
        {
            // pop exception object from invoke's stateAfter and replace with this frameState's
            // exception object (top of stack)
            FrameState __stateAfterException = __stateAtExceptionEdge;
            if (__frameState.stackSize() > 0 && __stateAtExceptionEdge.stackAt(0) != __frameState.stackAt(0))
            {
                __stateAfterException = __stateAtExceptionEdge.duplicateModified(JavaKind.Object, JavaKind.Object, __frameState.stackAt(0));
            }
            __frameState.replaceAndDelete(__stateAfterException);
            return __stateAfterException;
        }
        else if ((__frameState.___bci == BytecodeFrame.UNWIND_BCI && __frameState.graph().getGuardsStage() == GuardsStage.FLOATING_GUARDS) || __frameState.___bci == BytecodeFrame.AFTER_EXCEPTION_BCI)
        {
            // This path converts the frame states relevant for exception unwinding to deoptimization.
            // This is only allowed in configurations when Graal compiles code for speculative execution
            // (e.g. JIT compilation in HotSpot) but not when compiled code must be deoptimization free
            // (e.g. AOT compilation for native image generation). There is currently no global flag in StructuredGraph
            // to distinguish such modes, but the GuardsStage during inlining indicates the mode in which Graal operates.
            handleMissingAfterExceptionFrameState(__frameState, __invoke, __replacements, __alwaysDuplicateStateAfter);
            return __frameState;
        }
        else if (__frameState.___bci == BytecodeFrame.BEFORE_BCI)
        {
            // This is an intrinsic. Deoptimizing within an intrinsic must re-execute the intrinsified invocation.
            ValueNode[] __invokeArgs = __invokeArgsList.isEmpty() ? NO_ARGS : __invokeArgsList.toArray(new ValueNode[__invokeArgsList.size()]);
            FrameState __stateBeforeCall = __stateAtReturn.duplicateModifiedBeforeCall(__invoke.bci(), __invokeReturnKind, __invokeTargetMethod.getSignature().toParameterKinds(!__invokeTargetMethod.isStatic()), __invokeArgs);
            __frameState.replaceAndDelete(__stateBeforeCall);
            return __stateBeforeCall;
        }
        else
        {
            // only handle the outermost frame states
            if (__frameState.outerFrameState() == null)
            {
                __frameState.setOuterFrameState(__outerFrameState);
            }
            return __frameState;
        }
    }

    private static FrameState handleAfterBciFrameState(FrameState __frameState, Invoke __invoke, boolean __alwaysDuplicateStateAfter)
    {
        FrameState __stateAtReturn = __invoke.stateAfter();
        JavaKind __invokeReturnKind = __invoke.asNode().getStackKind();
        FrameState __stateAfterReturn = __stateAtReturn;
        if (__frameState.getCode() == null)
        {
            // this is a frame state for a side effect within an intrinsic that was parsed for post-parse intrinsification
            for (Node __usage : __frameState.usages())
            {
                if (__usage instanceof ForeignCallNode)
                {
                    // a foreign call inside an intrinsic needs to have the BCI of the invoke being intrinsified
                    ForeignCallNode __foreign = (ForeignCallNode) __usage;
                    __foreign.setBci(__invoke.bci());
                }
            }
        }

        // pop return kind from invoke's stateAfter and replace with this frameState's return value (top of stack)
        if (__frameState.stackSize() > 0 && (__alwaysDuplicateStateAfter || __stateAfterReturn.stackAt(0) != __frameState.stackAt(0)))
        {
            // a non-void return value
            __stateAfterReturn = __stateAtReturn.duplicateModified(__invokeReturnKind, __invokeReturnKind, __frameState.stackAt(0));
        }
        else
        {
            // a void return value
            __stateAfterReturn = __stateAtReturn.duplicate();
        }

        // return value does no longer need to be limited by the monitor exit
        for (MonitorExitNode __n : __frameState.usages().filter(MonitorExitNode.class))
        {
            __n.clearEscapedReturnValue();
        }

        __frameState.replaceAndDelete(__stateAfterReturn);
        return __stateAfterReturn;
    }

    // @def
    private static final ValueNode[] NO_ARGS = {};

    private static boolean isStateAfterException(FrameState __frameState)
    {
        return __frameState.___bci == BytecodeFrame.AFTER_EXCEPTION_BCI || (__frameState.___bci == BytecodeFrame.UNWIND_BCI && !__frameState.getMethod().isSynchronized());
    }

    public static FrameState handleMissingAfterExceptionFrameState(FrameState __nonReplaceableFrameState, Invoke __invoke, EconomicMap<Node, Node> __replacements, boolean __alwaysDuplicateStateAfter)
    {
        StructuredGraph __graph = __nonReplaceableFrameState.graph();
        NodeWorkList __workList = __graph.createNodeWorkList();
        __workList.add(__nonReplaceableFrameState);
        for (Node __node : __workList)
        {
            FrameState __fs = (FrameState) __node;
            for (Node __usage : __fs.usages().snapshot())
            {
                if (!__usage.isAlive())
                {
                    continue;
                }
                if (__usage instanceof FrameState)
                {
                    __workList.add(__usage);
                }
                else
                {
                    StateSplit __stateSplit = (StateSplit) __usage;
                    FixedNode __fixedStateSplit = __stateSplit.asNode();
                    if (__fixedStateSplit instanceof AbstractMergeNode)
                    {
                        AbstractMergeNode __merge = (AbstractMergeNode) __fixedStateSplit;
                        while (__merge.isAlive())
                        {
                            AbstractEndNode __end = __merge.forwardEnds().first();
                            DeoptimizeNode __deoptimizeNode = addDeoptimizeNode(__graph, DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NotCompiledExceptionHandler);
                            __end.replaceAtPredecessor(__deoptimizeNode);
                            GraphUtil.killCFG(__end);
                        }
                    }
                    else if (__fixedStateSplit instanceof ExceptionObjectNode)
                    {
                        // The target invoke does not have an exception edge. This means that the bytecode parser made the wrong
                        // assumption of making an InvokeWithExceptionNode for the partial intrinsic exit. We therefore replace the
                        // InvokeWithExceptionNode with a normal InvokeNode -- the deoptimization occurs when the invoke throws.
                        InvokeWithExceptionNode __oldInvoke = (InvokeWithExceptionNode) __fixedStateSplit.predecessor();
                        FrameState __oldFrameState = __oldInvoke.stateAfter();
                        InvokeNode __newInvoke = __oldInvoke.replaceWithInvoke();
                        __newInvoke.setStateAfter(__oldFrameState.duplicate());
                        if (__replacements != null)
                        {
                            __replacements.put(__oldInvoke, __newInvoke);
                        }
                        handleAfterBciFrameState(__newInvoke.stateAfter(), __invoke, __alwaysDuplicateStateAfter);
                    }
                    else
                    {
                        FixedNode __deoptimizeNode = addDeoptimizeNode(__graph, DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NotCompiledExceptionHandler);
                        if (__fixedStateSplit instanceof AbstractBeginNode)
                        {
                            __deoptimizeNode = BeginNode.begin(__deoptimizeNode);
                        }
                        __fixedStateSplit.replaceAtPredecessor(__deoptimizeNode);
                        GraphUtil.killCFG(__fixedStateSplit);
                    }
                }
            }
        }
        return __nonReplaceableFrameState;
    }

    private static DeoptimizeNode addDeoptimizeNode(StructuredGraph __graph, DeoptimizationAction __action, DeoptimizationReason __reason)
    {
        GraalError.guarantee(__graph.getGuardsStage() == GuardsStage.FLOATING_GUARDS, "Cannot introduce speculative deoptimization when Graal is used with fixed guards");
        return __graph.add(new DeoptimizeNode(__action, __reason));
    }

    ///
    // Gets the receiver for an invoke, adding a guard if necessary to ensure it is non-null, and
    // ensuring that the resulting type is compatible with the method being invoked.
    ///
    public static ValueNode nonNullReceiver(Invoke __invoke)
    {
        MethodCallTargetNode __callTarget = (MethodCallTargetNode) __invoke.callTarget();
        StructuredGraph __graph = __callTarget.graph();
        ValueNode __oldReceiver = __callTarget.arguments().get(0);
        ValueNode __newReceiver = __oldReceiver;
        if (__newReceiver.getStackKind() == JavaKind.Object)
        {
            if (__invoke.getInvokeKind() == InvokeKind.Special)
            {
                Stamp __paramStamp = __newReceiver.stamp(NodeView.DEFAULT);
                Stamp __stamp = __paramStamp.join(StampFactory.object(TypeReference.create(__graph.getAssumptions(), __callTarget.targetMethod().getDeclaringClass())));
                if (!__stamp.equals(__paramStamp))
                {
                    // The verifier and previous optimizations guarantee unconditionally that the
                    // receiver is at least of the type of the method holder for a special invoke.
                    __newReceiver = __graph.unique(new PiNode(__newReceiver, __stamp));
                }
            }

            if (!StampTool.isPointerNonNull(__newReceiver))
            {
                LogicNode __condition = __graph.unique(IsNullNode.create(__newReceiver));
                FixedGuardNode __fixedGuard = __graph.add(new FixedGuardNode(__condition, DeoptimizationReason.NullCheckException, DeoptimizationAction.InvalidateReprofile, true));
                PiNode __nonNullReceiver = __graph.unique(new PiNode(__newReceiver, StampFactory.objectNonNull(), __fixedGuard));
                __graph.addBeforeFixed(__invoke.asNode(), __fixedGuard);
                __newReceiver = __nonNullReceiver;
            }
        }

        if (__newReceiver != __oldReceiver)
        {
            __callTarget.replaceFirstInput(__oldReceiver, __newReceiver);
        }
        return __newReceiver;
    }

    public static boolean canIntrinsify(Replacements __replacements, ResolvedJavaMethod __target, int __invokeBci)
    {
        return __replacements.hasSubstitution(__target, __invokeBci);
    }

    public static StructuredGraph getIntrinsicGraph(Replacements __replacements, ResolvedJavaMethod __target, int __invokeBci)
    {
        return __replacements.getSubstitution(__target, __invokeBci);
    }

    public static FixedWithNextNode inlineMacroNode(Invoke __invoke, ResolvedJavaMethod __concrete, Class<? extends FixedWithNextNode> __macroNodeClass)
    {
        StructuredGraph __graph = __invoke.asNode().graph();
        if (!__concrete.equals(((MethodCallTargetNode) __invoke.callTarget()).targetMethod()))
        {
            InliningUtil.replaceInvokeCallTarget(__invoke, __graph, InvokeKind.Special, __concrete);
        }

        FixedWithNextNode __macroNode = createMacroNodeInstance(__macroNodeClass, __invoke);

        CallTargetNode __callTarget = __invoke.callTarget();
        if (__invoke instanceof InvokeNode)
        {
            __graph.replaceFixedWithFixed((InvokeNode) __invoke, __graph.add(__macroNode));
        }
        else
        {
            InvokeWithExceptionNode __invokeWithException = (InvokeWithExceptionNode) __invoke;
            __invokeWithException.killExceptionEdge();
            __graph.replaceSplitWithFixed(__invokeWithException, __graph.add(__macroNode), __invokeWithException.next());
        }
        GraphUtil.killWithUnusedFloatingInputs(__callTarget);
        return __macroNode;
    }

    private static FixedWithNextNode createMacroNodeInstance(Class<? extends FixedWithNextNode> __macroNodeClass, Invoke __invoke)
    {
        try
        {
            Constructor<?> __cons = __macroNodeClass.getDeclaredConstructor(Invoke.class);
            return (FixedWithNextNode) __cons.newInstance(__invoke);
        }
        catch (ReflectiveOperationException | IllegalArgumentException | SecurityException __e)
        {
            throw new GraalError(__e);
        }
    }

    ///
    // This method exclude InstrumentationNode from inlining heuristics.
    ///
    public static int getNodeCount(StructuredGraph __graph)
    {
        return __graph.getNodeCount();
    }
}
