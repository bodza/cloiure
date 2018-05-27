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
import giraaff.graph.GraalGraphError;
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

public class InliningUtil extends ValueMergeUtil
{
    public static void replaceInvokeCallTarget(Invoke invoke, StructuredGraph graph, InvokeKind invokeKind, ResolvedJavaMethod targetMethod)
    {
        MethodCallTargetNode oldCallTarget = (MethodCallTargetNode) invoke.callTarget();
        MethodCallTargetNode newCallTarget = graph.add(new MethodCallTargetNode(invokeKind, targetMethod, oldCallTarget.arguments().toArray(new ValueNode[0]), oldCallTarget.returnStamp(), oldCallTarget.getProfile()));
        invoke.asNode().replaceFirstInput(oldCallTarget, newCallTarget);
    }

    public static PiNode createAnchoredReceiver(StructuredGraph graph, GuardingNode anchor, ResolvedJavaType commonType, ValueNode receiver, boolean exact)
    {
        return createAnchoredReceiver(graph, anchor, receiver, exact ? StampFactory.objectNonNull(TypeReference.createExactTrusted(commonType)) : StampFactory.objectNonNull(TypeReference.createTrusted(graph.getAssumptions(), commonType)));
    }

    private static PiNode createAnchoredReceiver(StructuredGraph graph, GuardingNode anchor, ValueNode receiver, Stamp stamp)
    {
        // to avoid that floating reads on receiver fields float above the type check
        return graph.unique(new PiNode(receiver, stamp, (ValueNode) anchor));
    }

    /**
     * @return null iff the check succeeds, otherwise a (non-null) descriptive message.
     */
    public static String checkInvokeConditions(Invoke invoke)
    {
        if (invoke.predecessor() == null || !invoke.asNode().isAlive())
        {
            return "the invoke is dead code";
        }
        if (!(invoke.callTarget() instanceof MethodCallTargetNode))
        {
            return "the invoke has already been lowered, or has been created as a low-level node";
        }
        MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
        if (callTarget.targetMethod() == null)
        {
            return "target method is null";
        }
        if (!invoke.useForInlining())
        {
            return "the invoke is marked to be not used for inlining";
        }
        ValueNode receiver = callTarget.receiver();
        if (receiver != null && receiver.isConstant() && receiver.isNullConstant())
        {
            return "receiver is null";
        }
        return null;
    }

    /**
     * Performs an actual inlining, thereby replacing the given invoke with the given
     * {@code inlineGraph}.
     *
     * @param invoke the invoke that will be replaced
     * @param inlineGraph the graph that the invoke will be replaced with
     * @param receiverNullCheck true if a null check needs to be generated for non-static inlinings,
     *            false if no such check is required
     * @param inlineeMethod the actual method being inlined. Maybe be null for snippets.
     */
    public static UnmodifiableEconomicMap<Node, Node> inline(Invoke invoke, StructuredGraph inlineGraph, boolean receiverNullCheck, ResolvedJavaMethod inlineeMethod)
    {
        try
        {
            return inline(invoke, inlineGraph, receiverNullCheck, inlineeMethod, "reason not specified", "phase not specified");
        }
        catch (GraalError ex)
        {
            ex.addContext("inlining into", invoke.asNode().graph().method());
            ex.addContext("inlinee", inlineGraph.method());
            throw ex;
        }
    }

    /**
     * Performs an actual inlining, thereby replacing the given invoke with the given
     * {@code inlineGraph}.
     *
     * @param invoke the invoke that will be replaced
     * @param inlineGraph the graph that the invoke will be replaced with
     * @param receiverNullCheck true if a null check needs to be generated for non-static inlinings,
     *            false if no such check is required
     * @param inlineeMethod the actual method being inlined. Maybe be null for snippets.
     * @param reason the reason for inlining, used in tracing
     * @param phase the phase that invoked inlining
     */
    public static UnmodifiableEconomicMap<Node, Node> inline(Invoke invoke, StructuredGraph inlineGraph, boolean receiverNullCheck, ResolvedJavaMethod inlineeMethod, String reason, String phase)
    {
        FixedNode invokeNode = invoke.asNode();
        StructuredGraph graph = invokeNode.graph();
        final NodeInputList<ValueNode> parameters = invoke.callTarget().arguments();

        if (receiverNullCheck && !((MethodCallTargetNode) invoke.callTarget()).isStatic())
        {
            nonNullReceiver(invoke);
        }

        ArrayList<Node> nodes = new ArrayList<>(inlineGraph.getNodes().count());
        ArrayList<ReturnNode> returnNodes = new ArrayList<>(4);
        ArrayList<Invoke> partialIntrinsicExits = new ArrayList<>();
        UnwindNode unwindNode = null;
        final StartNode entryPointNode = inlineGraph.start();
        FixedNode firstCFGNode = entryPointNode.next();
        if (firstCFGNode == null)
        {
            throw new IllegalStateException("Inlined graph is in invalid state: " + inlineGraph);
        }
        for (Node node : inlineGraph.getNodes())
        {
            if (node == entryPointNode || (node == entryPointNode.stateAfter() && node.usages().count() == 1) || node instanceof ParameterNode)
            {
                // Do nothing.
            }
            else
            {
                nodes.add(node);
                if (node instanceof ReturnNode)
                {
                    returnNodes.add((ReturnNode) node);
                }
                else if (node instanceof Invoke)
                {
                    Invoke invokeInInlineGraph = (Invoke) node;
                    if (invokeInInlineGraph.bci() == BytecodeFrame.UNKNOWN_BCI)
                    {
                        ResolvedJavaMethod target1 = inlineeMethod;
                        ResolvedJavaMethod target2 = invokeInInlineGraph.callTarget().targetMethod();
                        partialIntrinsicExits.add(invokeInInlineGraph);
                    }
                }
                else if (node instanceof UnwindNode)
                {
                    unwindNode = (UnwindNode) node;
                }
            }
        }

        final AbstractBeginNode prevBegin = AbstractBeginNode.prevBegin(invokeNode);
        DuplicationReplacement localReplacement = new DuplicationReplacement()
        {
            @Override
            public Node replacement(Node node)
            {
                if (node instanceof ParameterNode)
                {
                    return parameters.get(((ParameterNode) node).index());
                }
                else if (node == entryPointNode)
                {
                    return prevBegin;
                }
                return node;
            }
        };

        Mark mark = graph.getMark();
        // Instead, attach the inlining log of the child graph to the current inlining log.
        EconomicMap<Node, Node> duplicates = graph.addDuplicates(nodes, inlineGraph, inlineGraph.getNodeCount(), localReplacement);

        FrameState stateAfter = invoke.stateAfter();

        FrameState stateAtExceptionEdge = null;
        if (invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode invokeWithException = ((InvokeWithExceptionNode) invoke);
            if (unwindNode != null)
            {
                ExceptionObjectNode obj = (ExceptionObjectNode) invokeWithException.exceptionEdge();
                stateAtExceptionEdge = obj.stateAfter();
            }
        }

        if (stateAfter != null)
        {
            processFrameStates(invoke, inlineGraph, duplicates, stateAtExceptionEdge, returnNodes.size() > 1);
            int callerLockDepth = stateAfter.nestedLockDepth();
            if (callerLockDepth != 0)
            {
                for (MonitorIdNode original : inlineGraph.getNodes(MonitorIdNode.TYPE))
                {
                    MonitorIdNode monitor = (MonitorIdNode) duplicates.get(original);
                    processMonitorId(invoke.stateAfter(), monitor);
                }
            }
        }

        firstCFGNode = (FixedNode) duplicates.get(firstCFGNode);
        for (int i = 0; i < returnNodes.size(); i++)
        {
            returnNodes.set(i, (ReturnNode) duplicates.get(returnNodes.get(i)));
        }
        for (Invoke exit : partialIntrinsicExits)
        {
            // A partial intrinsic exit must be replaced with a call to the intrinsified method.
            Invoke dup = (Invoke) duplicates.get(exit.asNode());
            if (dup instanceof InvokeNode)
            {
                InvokeNode repl = graph.add(new InvokeNode(invoke.callTarget(), invoke.bci()));
                dup.intrinsify(repl.asNode());
            }
            else
            {
                ((InvokeWithExceptionNode) dup).replaceWithNewBci(invoke.bci());
            }
        }
        if (unwindNode != null)
        {
            unwindNode = (UnwindNode) duplicates.get(unwindNode);
        }

        finishInlining(invoke, graph, firstCFGNode, returnNodes, unwindNode, inlineGraph.getAssumptions(), inlineGraph);
        GraphUtil.killCFG(invokeNode);

        return duplicates;
    }

    /**
     * Inline {@code inlineGraph} into the current replacing the node {@code Invoke} and return the
     * set of nodes which should be canonicalized. The set should only contain nodes which modified by
     * the inlining since the current graph and {@code inlineGraph} are expected to already be canonical.
     *
     * @return the set of nodes to canonicalize
     */
    public static EconomicSet<Node> inlineForCanonicalization(Invoke invoke, StructuredGraph inlineGraph, boolean receiverNullCheck, ResolvedJavaMethod inlineeMethod, String reason, String phase)
    {
        return inlineForCanonicalization(invoke, inlineGraph, receiverNullCheck, inlineeMethod, null, reason, phase);
    }

    @SuppressWarnings("try")
    public static EconomicSet<Node> inlineForCanonicalization(Invoke invoke, StructuredGraph inlineGraph, boolean receiverNullCheck, ResolvedJavaMethod inlineeMethod, Consumer<UnmodifiableEconomicMap<Node, Node>> duplicatesConsumer, String reason, String phase)
    {
        HashSetNodeEventListener listener = new HashSetNodeEventListener();
        /*
         * This code relies on the fact that Graph.addDuplicates doesn't trigger the
         * NodeEventListener to track only nodes which were modified into the process of inlining
         * the graph into the current graph.
         */
        try (NodeEventScope nes = invoke.asNode().graph().trackNodeEvents(listener))
        {
            UnmodifiableEconomicMap<Node, Node> duplicates = InliningUtil.inline(invoke, inlineGraph, receiverNullCheck, inlineeMethod, reason, phase);
            if (duplicatesConsumer != null)
            {
                duplicatesConsumer.accept(duplicates);
            }
        }
        return listener.getNodes();
    }

    private static ValueNode finishInlining(Invoke invoke, StructuredGraph graph, FixedNode firstNode, List<ReturnNode> returnNodes, UnwindNode unwindNode, Assumptions inlinedAssumptions, StructuredGraph inlineGraph)
    {
        FixedNode invokeNode = invoke.asNode();
        FrameState stateAfter = invoke.stateAfter();

        invokeNode.replaceAtPredecessor(firstNode);

        if (invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode invokeWithException = ((InvokeWithExceptionNode) invoke);
            if (unwindNode != null && unwindNode.isAlive())
            {
                ExceptionObjectNode obj = (ExceptionObjectNode) invokeWithException.exceptionEdge();
                obj.replaceAtUsages(unwindNode.exception());
                Node n = obj.next();
                obj.setNext(null);
                unwindNode.replaceAndDelete(n);

                obj.replaceAtPredecessor(null);
                obj.safeDelete();
            }
            else
            {
                invokeWithException.killExceptionEdge();
            }

            // get rid of memory kill
            AbstractBeginNode begin = invokeWithException.next();
            if (begin instanceof KillingBeginNode)
            {
                AbstractBeginNode newBegin = new BeginNode();
                graph.addAfterFixed(begin, graph.add(newBegin));
                begin.replaceAtUsages(newBegin);
                graph.removeFixed(begin);
            }
        }
        else
        {
            if (unwindNode != null && unwindNode.isAlive())
            {
                DeoptimizeNode deoptimizeNode = addDeoptimizeNode(graph, DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NotCompiledExceptionHandler);
                unwindNode.replaceAndDelete(deoptimizeNode);
            }
        }

        ValueNode returnValue;
        if (!returnNodes.isEmpty())
        {
            FixedNode n = invoke.next();
            invoke.setNext(null);
            if (returnNodes.size() == 1)
            {
                ReturnNode returnNode = returnNodes.get(0);
                returnValue = returnNode.result();
                invokeNode.replaceAtUsages(returnValue);
                returnNode.replaceAndDelete(n);
            }
            else
            {
                MergeNode merge = graph.add(new MergeNode());
                merge.setStateAfter(stateAfter);
                returnValue = mergeReturns(merge, returnNodes);
                invokeNode.replaceAtUsages(returnValue);
                if (merge.isPhiAtMerge(returnValue))
                {
                    fixFrameStates(graph, merge, (PhiNode) returnValue);
                }
                merge.setNext(n);
            }
        }
        else
        {
            returnValue = null;
            invokeNode.replaceAtUsages(null);
            GraphUtil.killCFG(invoke.next());
        }

        // copy assumptions from inlinee to caller
        Assumptions assumptions = graph.getAssumptions();
        if (assumptions != null)
        {
            if (inlinedAssumptions != null)
            {
                assumptions.record(inlinedAssumptions);
            }
        }

        // copy inlined methods from inlinee to caller
        graph.updateMethods(inlineGraph);

        if (inlineGraph.hasUnsafeAccess())
        {
            graph.markUnsafeAccess();
        }

        return returnValue;
    }

    private static void fixFrameStates(StructuredGraph graph, MergeNode originalMerge, PhiNode returnPhi)
    {
        // It is possible that some of the frame states that came from AFTER_BCI reference a Phi node
        // that was created to merge multiple returns. This can create cycles (see GR-3949 and GR-3957).
        // To detect this, we follow the control paths starting from the merge node,
        // split the Phi node inputs at merges and assign the proper input to each frame state.
        NodeMap<Node> seen = new NodeMap<>(graph);
        ArrayDeque<Node> workList = new ArrayDeque<>();
        ArrayDeque<ValueNode> valueList = new ArrayDeque<>();
        workList.push(originalMerge);
        valueList.push(returnPhi);
        while (!workList.isEmpty())
        {
            Node current = workList.pop();
            ValueNode currentValue = valueList.pop();
            if (seen.containsKey(current))
            {
                continue;
            }
            seen.put(current, current);
            if (current instanceof StateSplit && current != originalMerge)
            {
                StateSplit stateSplit = (StateSplit) current;
                FrameState state = stateSplit.stateAfter();
                if (state != null && state.values().contains(returnPhi))
                {
                    int index = 0;
                    FrameState duplicate = state.duplicate();
                    for (ValueNode value : state.values())
                    {
                        if (value == returnPhi)
                        {
                            duplicate.values().set(index, currentValue);
                        }
                        index++;
                    }
                    stateSplit.setStateAfter(duplicate);
                    GraphUtil.tryKillUnused(state);
                }
            }
            if (current instanceof AbstractMergeNode)
            {
                AbstractMergeNode currentMerge = (AbstractMergeNode) current;
                for (EndNode pred : currentMerge.cfgPredecessors())
                {
                    ValueNode newValue = currentValue;
                    if (currentMerge.isPhiAtMerge(currentValue))
                    {
                        PhiNode currentPhi = (PhiNode) currentValue;
                        newValue = currentPhi.valueAt(pred);
                    }
                    workList.push(pred);
                    valueList.push(newValue);
                }
            }
            else if (current.predecessor() != null)
            {
                workList.push(current.predecessor());
                valueList.push(currentValue);
            }
        }
    }

    public static void processMonitorId(FrameState stateAfter, MonitorIdNode monitorIdNode)
    {
        if (stateAfter != null)
        {
            int callerLockDepth = stateAfter.nestedLockDepth();
            monitorIdNode.setLockDepth(monitorIdNode.getLockDepth() + callerLockDepth);
        }
    }

    protected static void processFrameStates(Invoke invoke, StructuredGraph inlineGraph, EconomicMap<Node, Node> duplicates, FrameState stateAtExceptionEdge, boolean alwaysDuplicateStateAfter)
    {
        FrameState stateAtReturn = invoke.stateAfter();
        FrameState outerFrameState = null;
        JavaKind invokeReturnKind = invoke.asNode().getStackKind();
        EconomicMap<Node, Node> replacements = EconomicMap.create();
        for (FrameState original : inlineGraph.getNodes(FrameState.TYPE))
        {
            FrameState frameState = (FrameState) duplicates.get(original);
            if (frameState != null && frameState.isAlive())
            {
                if (outerFrameState == null)
                {
                    outerFrameState = stateAtReturn.duplicateModifiedDuringCall(invoke.bci(), invokeReturnKind);
                }
                processFrameState(frameState, invoke, replacements, inlineGraph.method(), stateAtExceptionEdge, outerFrameState, alwaysDuplicateStateAfter, invoke.callTarget().targetMethod(), invoke.callTarget().arguments());
            }
        }
        // If processing the frame states replaced any nodes, update the duplicates map.
        duplicates.replaceAll((key, value) -> replacements.containsKey(value) ? replacements.get(value) : value);
    }

    public static FrameState processFrameState(FrameState frameState, Invoke invoke, EconomicMap<Node, Node> replacements, ResolvedJavaMethod inlinedMethod, FrameState stateAtExceptionEdge, FrameState outerFrameState, boolean alwaysDuplicateStateAfter, ResolvedJavaMethod invokeTargetMethod, List<ValueNode> invokeArgsList)
    {
        final FrameState stateAtReturn = invoke.stateAfter();
        JavaKind invokeReturnKind = invoke.asNode().getStackKind();

        if (frameState.bci == BytecodeFrame.AFTER_BCI)
        {
            return handleAfterBciFrameState(frameState, invoke, alwaysDuplicateStateAfter);
        }
        else if (stateAtExceptionEdge != null && isStateAfterException(frameState))
        {
            // pop exception object from invoke's stateAfter and replace with this frameState's
            // exception object (top of stack)
            FrameState stateAfterException = stateAtExceptionEdge;
            if (frameState.stackSize() > 0 && stateAtExceptionEdge.stackAt(0) != frameState.stackAt(0))
            {
                stateAfterException = stateAtExceptionEdge.duplicateModified(JavaKind.Object, JavaKind.Object, frameState.stackAt(0));
            }
            frameState.replaceAndDelete(stateAfterException);
            return stateAfterException;
        }
        else if ((frameState.bci == BytecodeFrame.UNWIND_BCI && frameState.graph().getGuardsStage() == GuardsStage.FLOATING_GUARDS) || frameState.bci == BytecodeFrame.AFTER_EXCEPTION_BCI)
        {
            /*
             * This path converts the frame states relevant for exception unwinding to deoptimization.
             * This is only allowed in configurations when Graal compiles code for speculative execution
             * (e.g. JIT compilation in HotSpot) but not when compiled code must be deoptimization free
             * (e.g. AOT compilation for native image generation). There is currently no global flag in StructuredGraph
             * to distinguish such modes, but the GuardsStage during inlining indicates the mode in which Graal operates.
             */
            handleMissingAfterExceptionFrameState(frameState, invoke, replacements, alwaysDuplicateStateAfter);
            return frameState;
        }
        else if (frameState.bci == BytecodeFrame.BEFORE_BCI)
        {
            // This is an intrinsic. Deoptimizing within an intrinsic must re-execute the intrinsified invocation.
            ValueNode[] invokeArgs = invokeArgsList.isEmpty() ? NO_ARGS : invokeArgsList.toArray(new ValueNode[invokeArgsList.size()]);
            FrameState stateBeforeCall = stateAtReturn.duplicateModifiedBeforeCall(invoke.bci(), invokeReturnKind, invokeTargetMethod.getSignature().toParameterKinds(!invokeTargetMethod.isStatic()), invokeArgs);
            frameState.replaceAndDelete(stateBeforeCall);
            return stateBeforeCall;
        }
        else
        {
            // only handle the outermost frame states
            if (frameState.outerFrameState() == null)
            {
                frameState.setOuterFrameState(outerFrameState);
            }
            return frameState;
        }
    }

    private static FrameState handleAfterBciFrameState(FrameState frameState, Invoke invoke, boolean alwaysDuplicateStateAfter)
    {
        FrameState stateAtReturn = invoke.stateAfter();
        JavaKind invokeReturnKind = invoke.asNode().getStackKind();
        FrameState stateAfterReturn = stateAtReturn;
        if (frameState.getCode() == null)
        {
            // this is a frame state for a side effect within an intrinsic that was parsed for post-parse intrinsification
            for (Node usage : frameState.usages())
            {
                if (usage instanceof ForeignCallNode)
                {
                    // a foreign call inside an intrinsic needs to have the BCI of the invoke being intrinsified
                    ForeignCallNode foreign = (ForeignCallNode) usage;
                    foreign.setBci(invoke.bci());
                }
            }
        }

        // pop return kind from invoke's stateAfter and replace with this frameState's return value (top of stack)
        if (frameState.stackSize() > 0 && (alwaysDuplicateStateAfter || stateAfterReturn.stackAt(0) != frameState.stackAt(0)))
        {
            // a non-void return value
            stateAfterReturn = stateAtReturn.duplicateModified(invokeReturnKind, invokeReturnKind, frameState.stackAt(0));
        }
        else
        {
            // a void return value
            stateAfterReturn = stateAtReturn.duplicate();
        }

        // return value does no longer need to be limited by the monitor exit
        for (MonitorExitNode n : frameState.usages().filter(MonitorExitNode.class))
        {
            n.clearEscapedReturnValue();
        }

        frameState.replaceAndDelete(stateAfterReturn);
        return stateAfterReturn;
    }

    private static final ValueNode[] NO_ARGS = {};

    private static boolean isStateAfterException(FrameState frameState)
    {
        return frameState.bci == BytecodeFrame.AFTER_EXCEPTION_BCI || (frameState.bci == BytecodeFrame.UNWIND_BCI && !frameState.getMethod().isSynchronized());
    }

    public static FrameState handleMissingAfterExceptionFrameState(FrameState nonReplaceableFrameState, Invoke invoke, EconomicMap<Node, Node> replacements, boolean alwaysDuplicateStateAfter)
    {
        StructuredGraph graph = nonReplaceableFrameState.graph();
        NodeWorkList workList = graph.createNodeWorkList();
        workList.add(nonReplaceableFrameState);
        for (Node node : workList)
        {
            FrameState fs = (FrameState) node;
            for (Node usage : fs.usages().snapshot())
            {
                if (!usage.isAlive())
                {
                    continue;
                }
                if (usage instanceof FrameState)
                {
                    workList.add(usage);
                }
                else
                {
                    StateSplit stateSplit = (StateSplit) usage;
                    FixedNode fixedStateSplit = stateSplit.asNode();
                    if (fixedStateSplit instanceof AbstractMergeNode)
                    {
                        AbstractMergeNode merge = (AbstractMergeNode) fixedStateSplit;
                        while (merge.isAlive())
                        {
                            AbstractEndNode end = merge.forwardEnds().first();
                            DeoptimizeNode deoptimizeNode = addDeoptimizeNode(graph, DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NotCompiledExceptionHandler);
                            end.replaceAtPredecessor(deoptimizeNode);
                            GraphUtil.killCFG(end);
                        }
                    }
                    else if (fixedStateSplit instanceof ExceptionObjectNode)
                    {
                        // The target invoke does not have an exception edge. This means that the bytecode parser made the wrong
                        // assumption of making an InvokeWithExceptionNode for the partial intrinsic exit. We therefore replace the
                        // InvokeWithExceptionNode with a normal InvokeNode -- the deoptimization occurs when the invoke throws.
                        InvokeWithExceptionNode oldInvoke = (InvokeWithExceptionNode) fixedStateSplit.predecessor();
                        FrameState oldFrameState = oldInvoke.stateAfter();
                        InvokeNode newInvoke = oldInvoke.replaceWithInvoke();
                        newInvoke.setStateAfter(oldFrameState.duplicate());
                        if (replacements != null)
                        {
                            replacements.put(oldInvoke, newInvoke);
                        }
                        handleAfterBciFrameState(newInvoke.stateAfter(), invoke, alwaysDuplicateStateAfter);
                    }
                    else
                    {
                        FixedNode deoptimizeNode = addDeoptimizeNode(graph, DeoptimizationAction.InvalidateRecompile, DeoptimizationReason.NotCompiledExceptionHandler);
                        if (fixedStateSplit instanceof AbstractBeginNode)
                        {
                            deoptimizeNode = BeginNode.begin(deoptimizeNode);
                        }
                        fixedStateSplit.replaceAtPredecessor(deoptimizeNode);
                        GraphUtil.killCFG(fixedStateSplit);
                    }
                }
            }
        }
        return nonReplaceableFrameState;
    }

    private static DeoptimizeNode addDeoptimizeNode(StructuredGraph graph, DeoptimizationAction action, DeoptimizationReason reason)
    {
        GraalError.guarantee(graph.getGuardsStage() == GuardsStage.FLOATING_GUARDS, "Cannot introduce speculative deoptimization when Graal is used with fixed guards");
        return graph.add(new DeoptimizeNode(action, reason));
    }

    /**
     * Gets the receiver for an invoke, adding a guard if necessary to ensure it is non-null, and
     * ensuring that the resulting type is compatible with the method being invoked.
     */
    public static ValueNode nonNullReceiver(Invoke invoke)
    {
        MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
        StructuredGraph graph = callTarget.graph();
        ValueNode oldReceiver = callTarget.arguments().get(0);
        ValueNode newReceiver = oldReceiver;
        if (newReceiver.getStackKind() == JavaKind.Object)
        {
            if (invoke.getInvokeKind() == InvokeKind.Special)
            {
                Stamp paramStamp = newReceiver.stamp(NodeView.DEFAULT);
                Stamp stamp = paramStamp.join(StampFactory.object(TypeReference.create(graph.getAssumptions(), callTarget.targetMethod().getDeclaringClass())));
                if (!stamp.equals(paramStamp))
                {
                    // The verifier and previous optimizations guarantee unconditionally that the
                    // receiver is at least of the type of the method holder for a special invoke.
                    newReceiver = graph.unique(new PiNode(newReceiver, stamp));
                }
            }

            if (!StampTool.isPointerNonNull(newReceiver))
            {
                LogicNode condition = graph.unique(IsNullNode.create(newReceiver));
                FixedGuardNode fixedGuard = graph.add(new FixedGuardNode(condition, DeoptimizationReason.NullCheckException, DeoptimizationAction.InvalidateReprofile, true));
                PiNode nonNullReceiver = graph.unique(new PiNode(newReceiver, StampFactory.objectNonNull(), fixedGuard));
                graph.addBeforeFixed(invoke.asNode(), fixedGuard);
                newReceiver = nonNullReceiver;
            }
        }

        if (newReceiver != oldReceiver)
        {
            callTarget.replaceFirstInput(oldReceiver, newReceiver);
        }
        return newReceiver;
    }

    public static boolean canIntrinsify(Replacements replacements, ResolvedJavaMethod target, int invokeBci)
    {
        return replacements.hasSubstitution(target, invokeBci);
    }

    public static StructuredGraph getIntrinsicGraph(Replacements replacements, ResolvedJavaMethod target, int invokeBci)
    {
        return replacements.getSubstitution(target, invokeBci);
    }

    public static FixedWithNextNode inlineMacroNode(Invoke invoke, ResolvedJavaMethod concrete, Class<? extends FixedWithNextNode> macroNodeClass) throws GraalError
    {
        StructuredGraph graph = invoke.asNode().graph();
        if (!concrete.equals(((MethodCallTargetNode) invoke.callTarget()).targetMethod()))
        {
            InliningUtil.replaceInvokeCallTarget(invoke, graph, InvokeKind.Special, concrete);
        }

        FixedWithNextNode macroNode = createMacroNodeInstance(macroNodeClass, invoke);

        CallTargetNode callTarget = invoke.callTarget();
        if (invoke instanceof InvokeNode)
        {
            graph.replaceFixedWithFixed((InvokeNode) invoke, graph.add(macroNode));
        }
        else
        {
            InvokeWithExceptionNode invokeWithException = (InvokeWithExceptionNode) invoke;
            invokeWithException.killExceptionEdge();
            graph.replaceSplitWithFixed(invokeWithException, graph.add(macroNode), invokeWithException.next());
        }
        GraphUtil.killWithUnusedFloatingInputs(callTarget);
        return macroNode;
    }

    private static FixedWithNextNode createMacroNodeInstance(Class<? extends FixedWithNextNode> macroNodeClass, Invoke invoke) throws GraalError
    {
        try
        {
            Constructor<?> cons = macroNodeClass.getDeclaredConstructor(Invoke.class);
            return (FixedWithNextNode) cons.newInstance(invoke);
        }
        catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e)
        {
            throw new GraalGraphError(e).addContext(invoke.asNode()).addContext("macroSubstitution", macroNodeClass);
        }
    }

    /**
     * This method exclude InstrumentationNode from inlining heuristics.
     */
    public static int getNodeCount(StructuredGraph graph)
    {
        return graph.getNodeCount();
    }
}
