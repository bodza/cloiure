package giraaff.phases.common.inlining.info;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaTypeProfile.ProfiledType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.Invoke;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PhiNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.java.ExceptionObjectNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.java.TypeSwitchNode;
import giraaff.nodes.spi.StampProvider;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.util.Providers;

/**
 * Polymorphic inlining of m methods with n type checks (n &ge; m) in case that the profiling
 * information suggests a reasonable amount of different receiver types and different methods. If an
 * unknown type is encountered a deoptimization is triggered.
 */
public class MultiTypeGuardInlineInfo extends AbstractInlineInfo
{
    private final List<ResolvedJavaMethod> concretes;
    private final double[] methodProbabilities;
    private final double maximumMethodProbability;
    private final ArrayList<Integer> typesToConcretes;
    private final ArrayList<ProfiledType> ptypes;
    private final double notRecordedTypeProbability;
    private final Inlineable[] inlineableElements;

    public MultiTypeGuardInlineInfo(Invoke invoke, ArrayList<ResolvedJavaMethod> concretes, ArrayList<ProfiledType> ptypes, ArrayList<Integer> typesToConcretes, double notRecordedTypeProbability)
    {
        super(invoke);

        this.concretes = concretes;
        this.ptypes = ptypes;
        this.typesToConcretes = typesToConcretes;
        this.notRecordedTypeProbability = notRecordedTypeProbability;
        this.inlineableElements = new Inlineable[concretes.size()];
        this.methodProbabilities = computeMethodProbabilities();
        this.maximumMethodProbability = maximumMethodProbability();
    }

    private static boolean assertUniqueTypes(ArrayList<ProfiledType> ptypes)
    {
        EconomicSet<ResolvedJavaType> set = EconomicSet.create(Equivalence.DEFAULT);
        for (ProfiledType ptype : ptypes)
        {
            set.add(ptype.getType());
        }
        return set.size() == ptypes.size();
    }

    private double[] computeMethodProbabilities()
    {
        double[] result = new double[concretes.size()];
        for (int i = 0; i < typesToConcretes.size(); i++)
        {
            int concrete = typesToConcretes.get(i);
            double probability = ptypes.get(i).getProbability();
            result[concrete] += probability;
        }
        return result;
    }

    private double maximumMethodProbability()
    {
        double max = 0;
        for (int i = 0; i < methodProbabilities.length; i++)
        {
            max = Math.max(max, methodProbabilities[i]);
        }
        return max;
    }

    @Override
    public int numberOfMethods()
    {
        return concretes.size();
    }

    @Override
    public ResolvedJavaMethod methodAt(int index)
    {
        return concretes.get(index);
    }

    @Override
    public Inlineable inlineableElementAt(int index)
    {
        return inlineableElements[index];
    }

    @Override
    public double probabilityAt(int index)
    {
        return methodProbabilities[index];
    }

    @Override
    public double relevanceAt(int index)
    {
        return probabilityAt(index) / maximumMethodProbability;
    }

    @Override
    public void setInlinableElement(int index, Inlineable inlineableElement)
    {
        inlineableElements[index] = inlineableElement;
    }

    @Override
    public EconomicSet<Node> inline(Providers providers, String reason)
    {
        if (hasSingleMethod())
        {
            return inlineSingleMethod(graph(), providers.getStampProvider(), providers.getConstantReflection(), reason);
        }
        else
        {
            return inlineMultipleMethods(graph(), providers, reason);
        }
    }

    @Override
    public boolean shouldInline()
    {
        for (ResolvedJavaMethod method : concretes)
        {
            if (method.shouldBeInlined())
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasSingleMethod()
    {
        return concretes.size() == 1 && !shouldFallbackToInvoke();
    }

    private boolean shouldFallbackToInvoke()
    {
        return notRecordedTypeProbability > 0;
    }

    private EconomicSet<Node> inlineMultipleMethods(StructuredGraph graph, Providers providers, String reason)
    {
        int numberOfMethods = concretes.size();
        FixedNode continuation = invoke.next();

        // setup merge and phi nodes for results and exceptions
        AbstractMergeNode returnMerge = graph.add(new MergeNode());
        returnMerge.setStateAfter(invoke.stateAfter());

        PhiNode returnValuePhi = null;
        if (invoke.asNode().getStackKind() != JavaKind.Void)
        {
            returnValuePhi = graph.addWithoutUnique(new ValuePhiNode(invoke.asNode().stamp(NodeView.DEFAULT).unrestricted(), returnMerge));
        }

        AbstractMergeNode exceptionMerge = null;
        PhiNode exceptionObjectPhi = null;
        if (invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode invokeWithException = (InvokeWithExceptionNode) invoke;
            ExceptionObjectNode exceptionEdge = (ExceptionObjectNode) invokeWithException.exceptionEdge();

            exceptionMerge = graph.add(new MergeNode());

            FixedNode exceptionSux = exceptionEdge.next();
            graph.addBeforeFixed(exceptionSux, exceptionMerge);
            exceptionObjectPhi = graph.addWithoutUnique(new ValuePhiNode(StampFactory.forKind(JavaKind.Object), exceptionMerge));
            exceptionMerge.setStateAfter(exceptionEdge.stateAfter().duplicateModified(invoke.stateAfter().bci, true, JavaKind.Object, new JavaKind[]{JavaKind.Object}, new ValueNode[]{exceptionObjectPhi}));
        }

        // create one separate block for each invoked method
        AbstractBeginNode[] successors = new AbstractBeginNode[numberOfMethods + 1];
        for (int i = 0; i < numberOfMethods; i++)
        {
            successors[i] = createInvocationBlock(graph, invoke, returnMerge, returnValuePhi, exceptionMerge, exceptionObjectPhi, true);
        }

        // create the successor for an unknown type
        FixedNode unknownTypeSux;
        if (shouldFallbackToInvoke())
        {
            unknownTypeSux = createInvocationBlock(graph, invoke, returnMerge, returnValuePhi, exceptionMerge, exceptionObjectPhi, false);
        }
        else
        {
            unknownTypeSux = graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TypeCheckedInliningViolated));
        }
        successors[successors.length - 1] = BeginNode.begin(unknownTypeSux);

        // replace the invoke exception edge
        if (invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode invokeWithExceptionNode = (InvokeWithExceptionNode) invoke;
            ExceptionObjectNode exceptionEdge = (ExceptionObjectNode) invokeWithExceptionNode.exceptionEdge();
            exceptionEdge.replaceAtUsages(exceptionObjectPhi);
            exceptionEdge.setNext(null);
            GraphUtil.killCFG(invokeWithExceptionNode.exceptionEdge());
        }

        // replace the invoke with a switch on the type of the actual receiver
        boolean methodDispatch = createDispatchOnTypeBeforeInvoke(graph, successors, false, providers.getStampProvider(), providers.getConstantReflection());

        invoke.setNext(null);
        returnMerge.setNext(continuation);
        if (returnValuePhi != null)
        {
            invoke.asNode().replaceAtUsages(returnValuePhi);
        }
        invoke.asNode().safeDelete();

        ArrayList<PiNode> replacementNodes = new ArrayList<>();

        // prepare the anchors for the invokes
        for (int i = 0; i < numberOfMethods; i++)
        {
            AbstractBeginNode node = successors[i];
            Invoke invokeForInlining = (Invoke) node.next();

            ResolvedJavaType commonType;
            if (methodDispatch)
            {
                commonType = concretes.get(i).getDeclaringClass();
            }
            else
            {
                commonType = getLeastCommonType(i);
            }

            ValueNode receiver = ((MethodCallTargetNode) invokeForInlining.callTarget()).receiver();
            boolean exact = (getTypeCount(i) == 1 && !methodDispatch);
            PiNode anchoredReceiver = InliningUtil.createAnchoredReceiver(graph, node, commonType, receiver, exact);
            invokeForInlining.callTarget().replaceFirstInput(receiver, anchoredReceiver);

            replacementNodes.add(anchoredReceiver);
        }
        if (shouldFallbackToInvoke())
        {
            replacementNodes.add(null);
        }

        EconomicSet<Node> canonicalizeNodes = EconomicSet.create(Equivalence.DEFAULT);
        // do the actual inlining for every invoke
        for (int i = 0; i < numberOfMethods; i++)
        {
            Invoke invokeForInlining = (Invoke) successors[i].next();
            canonicalizeNodes.addAll(doInline(i, invokeForInlining, reason));
        }
        if (returnValuePhi != null)
        {
            canonicalizeNodes.add(returnValuePhi);
        }
        return canonicalizeNodes;
    }

    protected EconomicSet<Node> doInline(int index, Invoke invokeForInlining, String reason)
    {
        return inline(invokeForInlining, methodAt(index), inlineableElementAt(index), false, reason);
    }

    private int getTypeCount(int concreteMethodIndex)
    {
        int count = 0;
        for (int i = 0; i < typesToConcretes.size(); i++)
        {
            if (typesToConcretes.get(i) == concreteMethodIndex)
            {
                count++;
            }
        }
        return count;
    }

    private ResolvedJavaType getLeastCommonType(int concreteMethodIndex)
    {
        ResolvedJavaType commonType = null;
        for (int i = 0; i < typesToConcretes.size(); i++)
        {
            if (typesToConcretes.get(i) == concreteMethodIndex)
            {
                if (commonType == null)
                {
                    commonType = ptypes.get(i).getType();
                }
                else
                {
                    commonType = commonType.findLeastCommonAncestor(ptypes.get(i).getType());
                }
            }
        }
        return commonType;
    }

    private ResolvedJavaType getLeastCommonType()
    {
        ResolvedJavaType result = getLeastCommonType(0);
        for (int i = 1; i < concretes.size(); i++)
        {
            result = result.findLeastCommonAncestor(getLeastCommonType(i));
        }
        return result;
    }

    private EconomicSet<Node> inlineSingleMethod(StructuredGraph graph, StampProvider stampProvider, ConstantReflectionProvider constantReflection, String reason)
    {
        AbstractBeginNode calleeEntryNode = graph.add(new BeginNode());

        AbstractBeginNode unknownTypeSux = createUnknownTypeSuccessor(graph);
        AbstractBeginNode[] successors = new AbstractBeginNode[]{calleeEntryNode, unknownTypeSux};
        createDispatchOnTypeBeforeInvoke(graph, successors, false, stampProvider, constantReflection);

        calleeEntryNode.setNext(invoke.asNode());

        return inline(invoke, methodAt(0), inlineableElementAt(0), false, reason);
    }

    private boolean createDispatchOnTypeBeforeInvoke(StructuredGraph graph, AbstractBeginNode[] successors, boolean invokeIsOnlySuccessor, StampProvider stampProvider, ConstantReflectionProvider constantReflection)
    {
        ValueNode nonNullReceiver = InliningUtil.nonNullReceiver(invoke);
        LoadHubNode hub = graph.unique(new LoadHubNode(stampProvider, nonNullReceiver));

        ResolvedJavaType[] keys = new ResolvedJavaType[ptypes.size()];
        double[] keyProbabilities = new double[ptypes.size() + 1];
        int[] keySuccessors = new int[ptypes.size() + 1];
        double totalProbability = notRecordedTypeProbability;
        for (int i = 0; i < ptypes.size(); i++)
        {
            keys[i] = ptypes.get(i).getType();
            keyProbabilities[i] = ptypes.get(i).getProbability();
            totalProbability += keyProbabilities[i];
            keySuccessors[i] = invokeIsOnlySuccessor ? 0 : typesToConcretes.get(i);
        }
        keyProbabilities[keyProbabilities.length - 1] = notRecordedTypeProbability;
        keySuccessors[keySuccessors.length - 1] = successors.length - 1;

        // Normalize the probabilities.
        for (int i = 0; i < keyProbabilities.length; i++)
        {
            keyProbabilities[i] /= totalProbability;
        }

        TypeSwitchNode typeSwitch = graph.add(new TypeSwitchNode(hub, successors, keys, keyProbabilities, keySuccessors, constantReflection));
        FixedWithNextNode pred = (FixedWithNextNode) invoke.asNode().predecessor();
        pred.setNext(typeSwitch);
        return false;
    }

    private static AbstractBeginNode createInvocationBlock(StructuredGraph graph, Invoke invoke, AbstractMergeNode returnMerge, PhiNode returnValuePhi, AbstractMergeNode exceptionMerge, PhiNode exceptionObjectPhi, boolean useForInlining)
    {
        Invoke duplicatedInvoke = duplicateInvokeForInlining(graph, invoke, exceptionMerge, exceptionObjectPhi, useForInlining);
        AbstractBeginNode calleeEntryNode = graph.add(new BeginNode());
        calleeEntryNode.setNext(duplicatedInvoke.asNode());

        EndNode endNode = graph.add(new EndNode());
        duplicatedInvoke.setNext(endNode);
        returnMerge.addForwardEnd(endNode);

        if (returnValuePhi != null)
        {
            returnValuePhi.addInput(duplicatedInvoke.asNode());
        }
        return calleeEntryNode;
    }

    private static Invoke duplicateInvokeForInlining(StructuredGraph graph, Invoke invoke, AbstractMergeNode exceptionMerge, PhiNode exceptionObjectPhi, boolean useForInlining)
    {
        Invoke result = (Invoke) invoke.asNode().copyWithInputs();
        Node callTarget = result.callTarget().copyWithInputs();
        result.asNode().replaceFirstInput(result.callTarget(), callTarget);
        result.setUseForInlining(useForInlining);

        JavaKind kind = invoke.asNode().getStackKind();
        if (kind != JavaKind.Void)
        {
            FrameState stateAfter = invoke.stateAfter();
            stateAfter = stateAfter.duplicate(stateAfter.bci);
            stateAfter.replaceFirstInput(invoke.asNode(), result.asNode());
            result.setStateAfter(stateAfter);
        }

        if (invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode invokeWithException = (InvokeWithExceptionNode) invoke;
            ExceptionObjectNode exceptionEdge = (ExceptionObjectNode) invokeWithException.exceptionEdge();
            FrameState stateAfterException = exceptionEdge.stateAfter();

            ExceptionObjectNode newExceptionEdge = (ExceptionObjectNode) exceptionEdge.copyWithInputs();
            // set new state (pop old exception object, push new one)
            newExceptionEdge.setStateAfter(stateAfterException.duplicateModified(JavaKind.Object, JavaKind.Object, newExceptionEdge));

            EndNode endNode = graph.add(new EndNode());
            newExceptionEdge.setNext(endNode);
            exceptionMerge.addForwardEnd(endNode);
            exceptionObjectPhi.addInput(newExceptionEdge);

            ((InvokeWithExceptionNode) result).setExceptionEdge(newExceptionEdge);
        }
        return result;
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers providers)
    {
        if (hasSingleMethod())
        {
            devirtualizeWithTypeSwitch(graph(), InvokeKind.Special, concretes.get(0), providers.getStampProvider(), providers.getConstantReflection());
        }
        else
        {
            tryToDevirtualizeMultipleMethods(graph(), providers.getStampProvider(), providers.getConstantReflection());
        }
    }

    private void tryToDevirtualizeMultipleMethods(StructuredGraph graph, StampProvider stampProvider, ConstantReflectionProvider constantReflection)
    {
        MethodCallTargetNode methodCallTarget = (MethodCallTargetNode) invoke.callTarget();
        if (methodCallTarget.invokeKind() == InvokeKind.Interface)
        {
            ResolvedJavaMethod targetMethod = methodCallTarget.targetMethod();
            ResolvedJavaType leastCommonType = getLeastCommonType();
            ResolvedJavaType contextType = invoke.getContextType();
            // check if we have a common base type that implements the interface -> in that case
            // we have a vtable entry for the interface method and can use a less expensive
            // virtual call
            if (!leastCommonType.isInterface() && targetMethod.getDeclaringClass().isAssignableFrom(leastCommonType))
            {
                ResolvedJavaMethod baseClassTargetMethod = leastCommonType.resolveConcreteMethod(targetMethod, contextType);
                if (baseClassTargetMethod != null)
                {
                    devirtualizeWithTypeSwitch(graph, InvokeKind.Virtual, leastCommonType.resolveConcreteMethod(targetMethod, contextType), stampProvider, constantReflection);
                }
            }
        }
    }

    private void devirtualizeWithTypeSwitch(StructuredGraph graph, InvokeKind kind, ResolvedJavaMethod target, StampProvider stampProvider, ConstantReflectionProvider constantReflection)
    {
        AbstractBeginNode invocationEntry = graph.add(new BeginNode());
        AbstractBeginNode unknownTypeSux = createUnknownTypeSuccessor(graph);
        AbstractBeginNode[] successors = new AbstractBeginNode[]{invocationEntry, unknownTypeSux};
        createDispatchOnTypeBeforeInvoke(graph, successors, true, stampProvider, constantReflection);

        invocationEntry.setNext(invoke.asNode());
        ValueNode receiver = ((MethodCallTargetNode) invoke.callTarget()).receiver();
        PiNode anchoredReceiver = InliningUtil.createAnchoredReceiver(graph, invocationEntry, target.getDeclaringClass(), receiver, false);
        invoke.callTarget().replaceFirstInput(receiver, anchoredReceiver);
        InliningUtil.replaceInvokeCallTarget(invoke, graph, kind, target);
    }

    private static AbstractBeginNode createUnknownTypeSuccessor(StructuredGraph graph)
    {
        return BeginNode.begin(graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TypeCheckedInliningViolated)));
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(shouldFallbackToInvoke() ? "megamorphic" : "polymorphic");
        builder.append(", ");
        builder.append(concretes.size());
        builder.append(" methods [ ");
        for (int i = 0; i < concretes.size(); i++)
        {
            builder.append(concretes.get(i).format("  %H.%n(%p):%r"));
        }
        builder.append(" ], ");
        builder.append(ptypes.size());
        builder.append(" type checks [ ");
        for (int i = 0; i < ptypes.size(); i++)
        {
            builder.append("  ");
            builder.append(ptypes.get(i).getType().getName());
            builder.append(ptypes.get(i).getProbability());
        }
        builder.append(" ]");
        return builder.toString();
    }
}
