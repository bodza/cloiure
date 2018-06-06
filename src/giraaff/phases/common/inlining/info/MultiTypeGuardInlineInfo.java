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
import giraaff.nodes.CallTargetNode;
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

///
// Polymorphic inlining of m methods with n type checks (n >= m) in case that the profiling
// information suggests a reasonable amount of different receiver types and different methods.
// If an unknown type is encountered a deoptimization is triggered.
///
// @class MultiTypeGuardInlineInfo
public final class MultiTypeGuardInlineInfo extends AbstractInlineInfo
{
    // @field
    private final List<ResolvedJavaMethod> ___concretes;
    // @field
    private final double[] ___methodProbabilities;
    // @field
    private final double ___maximumMethodProbability;
    // @field
    private final ArrayList<Integer> ___typesToConcretes;
    // @field
    private final ArrayList<ProfiledType> ___ptypes;
    // @field
    private final double ___notRecordedTypeProbability;
    // @field
    private final Inlineable[] ___inlineableElements;

    // @cons MultiTypeGuardInlineInfo
    public MultiTypeGuardInlineInfo(Invoke __invoke, ArrayList<ResolvedJavaMethod> __concretes, ArrayList<ProfiledType> __ptypes, ArrayList<Integer> __typesToConcretes, double __notRecordedTypeProbability)
    {
        super(__invoke);

        this.___concretes = __concretes;
        this.___ptypes = __ptypes;
        this.___typesToConcretes = __typesToConcretes;
        this.___notRecordedTypeProbability = __notRecordedTypeProbability;
        this.___inlineableElements = new Inlineable[__concretes.size()];
        this.___methodProbabilities = computeMethodProbabilities();
        this.___maximumMethodProbability = maximumMethodProbability();
    }

    private static boolean assertUniqueTypes(ArrayList<ProfiledType> __ptypes)
    {
        EconomicSet<ResolvedJavaType> __set = EconomicSet.create(Equivalence.DEFAULT);
        for (ProfiledType __ptype : __ptypes)
        {
            __set.add(__ptype.getType());
        }
        return __set.size() == __ptypes.size();
    }

    private double[] computeMethodProbabilities()
    {
        double[] __result = new double[this.___concretes.size()];
        for (int __i = 0; __i < this.___typesToConcretes.size(); __i++)
        {
            int __concrete = this.___typesToConcretes.get(__i);
            double __probability = this.___ptypes.get(__i).getProbability();
            __result[__concrete] += __probability;
        }
        return __result;
    }

    private double maximumMethodProbability()
    {
        double __max = 0;
        for (int __i = 0; __i < this.___methodProbabilities.length; __i++)
        {
            __max = Math.max(__max, this.___methodProbabilities[__i]);
        }
        return __max;
    }

    @Override
    public int numberOfMethods()
    {
        return this.___concretes.size();
    }

    @Override
    public ResolvedJavaMethod methodAt(int __index)
    {
        return this.___concretes.get(__index);
    }

    @Override
    public Inlineable inlineableElementAt(int __index)
    {
        return this.___inlineableElements[__index];
    }

    @Override
    public double probabilityAt(int __index)
    {
        return this.___methodProbabilities[__index];
    }

    @Override
    public double relevanceAt(int __index)
    {
        return probabilityAt(__index) / this.___maximumMethodProbability;
    }

    @Override
    public void setInlinableElement(int __index, Inlineable __inlineableElement)
    {
        this.___inlineableElements[__index] = __inlineableElement;
    }

    @Override
    public EconomicSet<Node> inline(Providers __providers, String __reason)
    {
        if (hasSingleMethod())
        {
            return inlineSingleMethod(graph(), __providers.getStampProvider(), __providers.getConstantReflection(), __reason);
        }
        else
        {
            return inlineMultipleMethods(graph(), __providers, __reason);
        }
    }

    @Override
    public boolean shouldInline()
    {
        for (ResolvedJavaMethod __method : this.___concretes)
        {
            if (__method.shouldBeInlined())
            {
                return true;
            }
        }
        return false;
    }

    private boolean hasSingleMethod()
    {
        return this.___concretes.size() == 1 && !shouldFallbackToInvoke();
    }

    private boolean shouldFallbackToInvoke()
    {
        return this.___notRecordedTypeProbability > 0;
    }

    private EconomicSet<Node> inlineMultipleMethods(StructuredGraph __graph, Providers __providers, String __reason)
    {
        int __numberOfMethods = this.___concretes.size();
        FixedNode __continuation = this.___invoke.next();

        // setup merge and phi nodes for results and exceptions
        AbstractMergeNode __returnMerge = __graph.add(new MergeNode());
        __returnMerge.setStateAfter(this.___invoke.stateAfter());

        PhiNode __returnValuePhi = null;
        if (this.___invoke.asNode().getStackKind() != JavaKind.Void)
        {
            __returnValuePhi = __graph.addWithoutUnique(new ValuePhiNode(this.___invoke.asNode().stamp(NodeView.DEFAULT).unrestricted(), __returnMerge));
        }

        AbstractMergeNode __exceptionMerge = null;
        PhiNode __exceptionObjectPhi = null;
        if (this.___invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode __invokeWithException = (InvokeWithExceptionNode) this.___invoke;
            ExceptionObjectNode __exceptionEdge = (ExceptionObjectNode) __invokeWithException.exceptionEdge();

            __exceptionMerge = __graph.add(new MergeNode());

            FixedNode __exceptionSux = __exceptionEdge.next();
            __graph.addBeforeFixed(__exceptionSux, __exceptionMerge);
            __exceptionObjectPhi = __graph.addWithoutUnique(new ValuePhiNode(StampFactory.forKind(JavaKind.Object), __exceptionMerge));
            __exceptionMerge.setStateAfter(__exceptionEdge.stateAfter().duplicateModified(this.___invoke.stateAfter().___bci, true, JavaKind.Object, new JavaKind[] { JavaKind.Object }, new ValueNode[] { __exceptionObjectPhi }));
        }

        // create one separate block for each invoked method
        AbstractBeginNode[] __successors = new AbstractBeginNode[__numberOfMethods + 1];
        for (int __i = 0; __i < __numberOfMethods; __i++)
        {
            __successors[__i] = createInvocationBlock(__graph, this.___invoke, __returnMerge, __returnValuePhi, __exceptionMerge, __exceptionObjectPhi, true);
        }

        // create the successor for an unknown type
        FixedNode __unknownTypeSux;
        if (shouldFallbackToInvoke())
        {
            __unknownTypeSux = createInvocationBlock(__graph, this.___invoke, __returnMerge, __returnValuePhi, __exceptionMerge, __exceptionObjectPhi, false);
        }
        else
        {
            __unknownTypeSux = __graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TypeCheckedInliningViolated));
        }
        __successors[__successors.length - 1] = BeginNode.begin(__unknownTypeSux);

        // replace the invoke exception edge
        if (this.___invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode __invokeWithExceptionNode = (InvokeWithExceptionNode) this.___invoke;
            ExceptionObjectNode __exceptionEdge = (ExceptionObjectNode) __invokeWithExceptionNode.exceptionEdge();
            __exceptionEdge.replaceAtUsages(__exceptionObjectPhi);
            __exceptionEdge.setNext(null);
            GraphUtil.killCFG(__invokeWithExceptionNode.exceptionEdge());
        }

        // replace the invoke with a switch on the type of the actual receiver
        boolean __methodDispatch = createDispatchOnTypeBeforeInvoke(__graph, __successors, false, __providers.getStampProvider(), __providers.getConstantReflection());

        this.___invoke.setNext(null);
        __returnMerge.setNext(__continuation);
        if (__returnValuePhi != null)
        {
            this.___invoke.asNode().replaceAtUsages(__returnValuePhi);
        }
        this.___invoke.asNode().safeDelete();

        ArrayList<PiNode> __replacementNodes = new ArrayList<>();

        // prepare the anchors for the invokes
        for (int __i = 0; __i < __numberOfMethods; __i++)
        {
            AbstractBeginNode __node = __successors[__i];
            Invoke __invokeForInlining = (Invoke) __node.next();

            ResolvedJavaType __commonType;
            if (__methodDispatch)
            {
                __commonType = this.___concretes.get(__i).getDeclaringClass();
            }
            else
            {
                __commonType = getLeastCommonType(__i);
            }

            ValueNode __receiver = ((MethodCallTargetNode) __invokeForInlining.callTarget()).receiver();
            boolean __exact = (getTypeCount(__i) == 1 && !__methodDispatch);
            PiNode __anchoredReceiver = InliningUtil.createAnchoredReceiver(__graph, __node, __commonType, __receiver, __exact);
            __invokeForInlining.callTarget().replaceFirstInput(__receiver, __anchoredReceiver);

            __replacementNodes.add(__anchoredReceiver);
        }
        if (shouldFallbackToInvoke())
        {
            __replacementNodes.add(null);
        }

        EconomicSet<Node> __canonicalizeNodes = EconomicSet.create(Equivalence.DEFAULT);
        // do the actual inlining for every invoke
        for (int __i = 0; __i < __numberOfMethods; __i++)
        {
            Invoke __invokeForInlining = (Invoke) __successors[__i].next();
            __canonicalizeNodes.addAll(doInline(__i, __invokeForInlining, __reason));
        }
        if (__returnValuePhi != null)
        {
            __canonicalizeNodes.add(__returnValuePhi);
        }
        return __canonicalizeNodes;
    }

    protected EconomicSet<Node> doInline(int __index, Invoke __invokeForInlining, String __reason)
    {
        return inline(__invokeForInlining, methodAt(__index), inlineableElementAt(__index), false, __reason);
    }

    private int getTypeCount(int __concreteMethodIndex)
    {
        int __count = 0;
        for (int __i = 0; __i < this.___typesToConcretes.size(); __i++)
        {
            if (this.___typesToConcretes.get(__i) == __concreteMethodIndex)
            {
                __count++;
            }
        }
        return __count;
    }

    private ResolvedJavaType getLeastCommonType(int __concreteMethodIndex)
    {
        ResolvedJavaType __commonType = null;
        for (int __i = 0; __i < this.___typesToConcretes.size(); __i++)
        {
            if (this.___typesToConcretes.get(__i) == __concreteMethodIndex)
            {
                if (__commonType == null)
                {
                    __commonType = this.___ptypes.get(__i).getType();
                }
                else
                {
                    __commonType = __commonType.findLeastCommonAncestor(this.___ptypes.get(__i).getType());
                }
            }
        }
        return __commonType;
    }

    private ResolvedJavaType getLeastCommonType()
    {
        ResolvedJavaType __result = getLeastCommonType(0);
        for (int __i = 1; __i < this.___concretes.size(); __i++)
        {
            __result = __result.findLeastCommonAncestor(getLeastCommonType(__i));
        }
        return __result;
    }

    private EconomicSet<Node> inlineSingleMethod(StructuredGraph __graph, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection, String __reason)
    {
        AbstractBeginNode __calleeEntryNode = __graph.add(new BeginNode());

        AbstractBeginNode __unknownTypeSux = createUnknownTypeSuccessor(__graph);
        AbstractBeginNode[] __successors = new AbstractBeginNode[] { __calleeEntryNode, __unknownTypeSux };
        createDispatchOnTypeBeforeInvoke(__graph, __successors, false, __stampProvider, __constantReflection);

        __calleeEntryNode.setNext(this.___invoke.asNode());

        return inline(this.___invoke, methodAt(0), inlineableElementAt(0), false, __reason);
    }

    private boolean createDispatchOnTypeBeforeInvoke(StructuredGraph __graph, AbstractBeginNode[] __successors, boolean __invokeIsOnlySuccessor, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection)
    {
        ValueNode __nonNullReceiver = InliningUtil.nonNullReceiver(this.___invoke);
        LoadHubNode __hub = __graph.unique(new LoadHubNode(__stampProvider, __nonNullReceiver));

        ResolvedJavaType[] __keys = new ResolvedJavaType[this.___ptypes.size()];
        double[] __keyProbabilities = new double[this.___ptypes.size() + 1];
        int[] __keySuccessors = new int[this.___ptypes.size() + 1];
        double __totalProbability = this.___notRecordedTypeProbability;
        for (int __i = 0; __i < this.___ptypes.size(); __i++)
        {
            __keys[__i] = this.___ptypes.get(__i).getType();
            __keyProbabilities[__i] = this.___ptypes.get(__i).getProbability();
            __totalProbability += __keyProbabilities[__i];
            __keySuccessors[__i] = __invokeIsOnlySuccessor ? 0 : this.___typesToConcretes.get(__i);
        }
        __keyProbabilities[__keyProbabilities.length - 1] = this.___notRecordedTypeProbability;
        __keySuccessors[__keySuccessors.length - 1] = __successors.length - 1;

        // Normalize the probabilities.
        for (int __i = 0; __i < __keyProbabilities.length; __i++)
        {
            __keyProbabilities[__i] /= __totalProbability;
        }

        TypeSwitchNode __typeSwitch = __graph.add(new TypeSwitchNode(__hub, __successors, __keys, __keyProbabilities, __keySuccessors, __constantReflection));
        FixedWithNextNode __pred = (FixedWithNextNode) this.___invoke.asNode().predecessor();
        __pred.setNext(__typeSwitch);
        return false;
    }

    private static AbstractBeginNode createInvocationBlock(StructuredGraph __graph, Invoke __invoke, AbstractMergeNode __returnMerge, PhiNode __returnValuePhi, AbstractMergeNode __exceptionMerge, PhiNode __exceptionObjectPhi, boolean __useForInlining)
    {
        Invoke __duplicatedInvoke = duplicateInvokeForInlining(__graph, __invoke, __exceptionMerge, __exceptionObjectPhi, __useForInlining);
        AbstractBeginNode __calleeEntryNode = __graph.add(new BeginNode());
        __calleeEntryNode.setNext(__duplicatedInvoke.asNode());

        EndNode __endNode = __graph.add(new EndNode());
        __duplicatedInvoke.setNext(__endNode);
        __returnMerge.addForwardEnd(__endNode);

        if (__returnValuePhi != null)
        {
            __returnValuePhi.addInput(__duplicatedInvoke.asNode());
        }
        return __calleeEntryNode;
    }

    private static Invoke duplicateInvokeForInlining(StructuredGraph __graph, Invoke __invoke, AbstractMergeNode __exceptionMerge, PhiNode __exceptionObjectPhi, boolean __useForInlining)
    {
        Invoke __result = (Invoke) __invoke.asNode().copyWithInputs();
        Node __callTarget = __result.callTarget().copyWithInputs();
        __result.asNode().replaceFirstInput(__result.callTarget(), __callTarget);
        __result.setUseForInlining(__useForInlining);

        JavaKind __kind = __invoke.asNode().getStackKind();
        if (__kind != JavaKind.Void)
        {
            FrameState __stateAfter = __invoke.stateAfter();
            __stateAfter = __stateAfter.duplicate(__stateAfter.___bci);
            __stateAfter.replaceFirstInput(__invoke.asNode(), __result.asNode());
            __result.setStateAfter(__stateAfter);
        }

        if (__invoke instanceof InvokeWithExceptionNode)
        {
            InvokeWithExceptionNode __invokeWithException = (InvokeWithExceptionNode) __invoke;
            ExceptionObjectNode __exceptionEdge = (ExceptionObjectNode) __invokeWithException.exceptionEdge();
            FrameState __stateAfterException = __exceptionEdge.stateAfter();

            ExceptionObjectNode __newExceptionEdge = (ExceptionObjectNode) __exceptionEdge.copyWithInputs();
            // set new state (pop old exception object, push new one)
            __newExceptionEdge.setStateAfter(__stateAfterException.duplicateModified(JavaKind.Object, JavaKind.Object, __newExceptionEdge));

            EndNode __endNode = __graph.add(new EndNode());
            __newExceptionEdge.setNext(__endNode);
            __exceptionMerge.addForwardEnd(__endNode);
            __exceptionObjectPhi.addInput(__newExceptionEdge);

            ((InvokeWithExceptionNode) __result).setExceptionEdge(__newExceptionEdge);
        }
        return __result;
    }

    @Override
    public void tryToDevirtualizeInvoke(Providers __providers)
    {
        if (hasSingleMethod())
        {
            devirtualizeWithTypeSwitch(graph(), CallTargetNode.InvokeKind.Special, this.___concretes.get(0), __providers.getStampProvider(), __providers.getConstantReflection());
        }
        else
        {
            tryToDevirtualizeMultipleMethods(graph(), __providers.getStampProvider(), __providers.getConstantReflection());
        }
    }

    private void tryToDevirtualizeMultipleMethods(StructuredGraph __graph, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection)
    {
        MethodCallTargetNode __methodCallTarget = (MethodCallTargetNode) this.___invoke.callTarget();
        if (__methodCallTarget.invokeKind() == CallTargetNode.InvokeKind.Interface)
        {
            ResolvedJavaMethod __targetMethod = __methodCallTarget.targetMethod();
            ResolvedJavaType __leastCommonType = getLeastCommonType();
            ResolvedJavaType __contextType = this.___invoke.getContextType();
            // check if we have a common base type that implements the interface -> in that case
            // we have a vtable entry for the interface method and can use a less expensive virtual call
            if (!__leastCommonType.isInterface() && __targetMethod.getDeclaringClass().isAssignableFrom(__leastCommonType))
            {
                ResolvedJavaMethod __baseClassTargetMethod = __leastCommonType.resolveConcreteMethod(__targetMethod, __contextType);
                if (__baseClassTargetMethod != null)
                {
                    devirtualizeWithTypeSwitch(__graph, CallTargetNode.InvokeKind.Virtual, __leastCommonType.resolveConcreteMethod(__targetMethod, __contextType), __stampProvider, __constantReflection);
                }
            }
        }
    }

    private void devirtualizeWithTypeSwitch(StructuredGraph __graph, CallTargetNode.InvokeKind __kind, ResolvedJavaMethod __target, StampProvider __stampProvider, ConstantReflectionProvider __constantReflection)
    {
        AbstractBeginNode __invocationEntry = __graph.add(new BeginNode());
        AbstractBeginNode __unknownTypeSux = createUnknownTypeSuccessor(__graph);
        AbstractBeginNode[] __successors = new AbstractBeginNode[] { __invocationEntry, __unknownTypeSux };
        createDispatchOnTypeBeforeInvoke(__graph, __successors, true, __stampProvider, __constantReflection);

        __invocationEntry.setNext(this.___invoke.asNode());
        ValueNode __receiver = ((MethodCallTargetNode) this.___invoke.callTarget()).receiver();
        PiNode __anchoredReceiver = InliningUtil.createAnchoredReceiver(__graph, __invocationEntry, __target.getDeclaringClass(), __receiver, false);
        this.___invoke.callTarget().replaceFirstInput(__receiver, __anchoredReceiver);
        InliningUtil.replaceInvokeCallTarget(this.___invoke, __graph, __kind, __target);
    }

    private static AbstractBeginNode createUnknownTypeSuccessor(StructuredGraph __graph)
    {
        return BeginNode.begin(__graph.add(new DeoptimizeNode(DeoptimizationAction.InvalidateReprofile, DeoptimizationReason.TypeCheckedInliningViolated)));
    }
}
