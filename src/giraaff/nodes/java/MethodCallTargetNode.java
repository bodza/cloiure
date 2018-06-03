package giraaff.nodes.java;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.ValueAnchorNode;
import giraaff.nodes.spi.UncheckedInterfaceProvider;
import giraaff.nodes.type.StampTool;

// @class MethodCallTargetNode
public class MethodCallTargetNode extends CallTargetNode implements IterableNodeType, Simplifiable
{
    // @def
    public static final NodeClass<MethodCallTargetNode> TYPE = NodeClass.create(MethodCallTargetNode.class);

    // @field
    protected JavaTypeProfile profile;

    // @cons
    public MethodCallTargetNode(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __arguments, StampPair __returnStamp, JavaTypeProfile __profile)
    {
        this(TYPE, __invokeKind, __targetMethod, __arguments, __returnStamp, __profile);
    }

    // @cons
    protected MethodCallTargetNode(NodeClass<? extends MethodCallTargetNode> __c, InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ValueNode[] __arguments, StampPair __returnStamp, JavaTypeProfile __profile)
    {
        super(__c, __arguments, __targetMethod, __invokeKind, __returnStamp);
        this.profile = __profile;
    }

    /**
     * Gets the instruction that produces the receiver object for this invocation, if any.
     *
     * @return the instruction that produces the receiver object for this invocation if any,
     *         {@code null} if this invocation does not take a receiver object
     */
    public ValueNode receiver()
    {
        return isStatic() ? null : arguments().get(0);
    }

    /**
     * Checks whether this is an invocation of a static method.
     *
     * @return {@code true} if the invocation is a static invocation
     */
    public boolean isStatic()
    {
        return invokeKind() == InvokeKind.Static;
    }

    public JavaKind returnKind()
    {
        return targetMethod().getSignature().getReturnKind();
    }

    public Invoke invoke()
    {
        return (Invoke) this.usages().first();
    }

    public static ResolvedJavaMethod findSpecialCallTarget(InvokeKind __invokeKind, ValueNode __receiver, ResolvedJavaMethod __targetMethod, ResolvedJavaType __contextType)
    {
        if (__invokeKind.isDirect())
        {
            return null;
        }

        // check for trivial cases (e.g. final methods, nonvirtual methods)
        if (__targetMethod.canBeStaticallyBound())
        {
            return __targetMethod;
        }

        return devirtualizeCall(__invokeKind, __targetMethod, __contextType, __receiver.graph().getAssumptions(), __receiver.stamp(NodeView.DEFAULT));
    }

    public static ResolvedJavaMethod devirtualizeCall(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, ResolvedJavaType __contextType, Assumptions __assumptions, Stamp __receiverStamp)
    {
        TypeReference __type = StampTool.typeReferenceOrNull(__receiverStamp);
        if (__type == null && __invokeKind == InvokeKind.Virtual)
        {
            // For virtual calls, we are guaranteed to receive a correct receiver type.
            __type = TypeReference.createTrusted(__assumptions, __targetMethod.getDeclaringClass());
        }

        if (__type != null)
        {
            // either the holder class is exact, or the receiver object has an exact type, or it's an array type
            ResolvedJavaMethod __resolvedMethod = __type.getType().resolveConcreteMethod(__targetMethod, __contextType);
            if (__resolvedMethod != null && (__resolvedMethod.canBeStaticallyBound() || __type.isExact() || __type.getType().isArray()))
            {
                return __resolvedMethod;
            }

            AssumptionResult<ResolvedJavaMethod> __uniqueConcreteMethod = __type.getType().findUniqueConcreteMethod(__targetMethod);
            if (__uniqueConcreteMethod != null && __uniqueConcreteMethod.canRecordTo(__assumptions))
            {
                __uniqueConcreteMethod.recordTo(__assumptions);
                return __uniqueConcreteMethod.getResult();
            }
        }
        return null;
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        // attempt to devirtualize the call
        if (invoke().getContextMethod() == null)
        {
            // avoid invokes that have placeholder bcis: they do not have a valid contextType
            return;
        }
        ResolvedJavaType __contextType = (invoke().stateAfter() == null && invoke().stateDuring() == null) ? null : invoke().getContextType();
        ResolvedJavaMethod __specialCallTarget = findSpecialCallTarget(invokeKind, receiver(), targetMethod, __contextType);
        if (__specialCallTarget != null)
        {
            this.setTargetMethod(__specialCallTarget);
            setInvokeKind(InvokeKind.Special);
            return;
        }

        Assumptions __assumptions = graph().getAssumptions();
        /*
         * Even though we are not registering an assumption (see comment below), the optimization is
         * only valid when speculative optimizations are enabled.
         */
        if (invokeKind().isIndirect() && invokeKind().isInterface() && __assumptions != null)
        {
            // check if the type of the receiver can narrow the result
            ValueNode __receiver = receiver();

            // try to turn a interface call into a virtual call
            ResolvedJavaType __declaredReceiverType = targetMethod().getDeclaringClass();

            // We need to check the invoke kind to avoid recursive simplification for virtual interface methods calls.
            if (__declaredReceiverType.isInterface())
            {
                ResolvedJavaType __singleImplementor = __declaredReceiverType.getSingleImplementor();
                if (__singleImplementor != null && !__singleImplementor.equals(__declaredReceiverType))
                {
                    TypeReference __speculatedType = TypeReference.createTrusted(__assumptions, __singleImplementor);
                    if (tryCheckCastSingleImplementor(__receiver, __speculatedType))
                    {
                        return;
                    }
                }
            }

            if (__receiver instanceof UncheckedInterfaceProvider)
            {
                UncheckedInterfaceProvider __uncheckedInterfaceProvider = (UncheckedInterfaceProvider) __receiver;
                Stamp __uncheckedStamp = __uncheckedInterfaceProvider.uncheckedStamp();
                if (__uncheckedStamp != null)
                {
                    TypeReference __speculatedType = StampTool.typeReferenceOrNull(__uncheckedStamp);
                    if (__speculatedType != null)
                    {
                        tryCheckCastSingleImplementor(__receiver, __speculatedType);
                    }
                }
            }
        }
    }

    private boolean tryCheckCastSingleImplementor(ValueNode __receiver, TypeReference __speculatedType)
    {
        ResolvedJavaType __singleImplementor = __speculatedType.getType();
        if (__singleImplementor != null)
        {
            ResolvedJavaMethod __singleImplementorMethod = __singleImplementor.resolveConcreteMethod(targetMethod(), invoke().getContextType());
            if (__singleImplementorMethod != null)
            {
                /**
                 * We have an invoke on an interface with a single implementor. We can replace this
                 * with an invoke virtual.
                 *
                 * To do so we need to ensure two properties: 1) the receiver must implement the
                 * interface (declaredReceiverType). The verifier does not prove this so we need a
                 * dynamic check. 2) we need to ensure that there is still only one implementor of
                 * this interface, i.e. that we are calling the right method. We could do this with
                 * an assumption but as we need an instanceof check anyway we can verify both
                 * properties by checking of the receiver is an instance of the single implementor.
                 */
                ValueAnchorNode __anchor = new ValueAnchorNode(null);
                if (__anchor != null)
                {
                    graph().add(__anchor);
                    graph().addBeforeFixed(invoke().asNode(), __anchor);
                }
                LogicNode __condition = graph().addOrUniqueWithInputs(InstanceOfNode.create(__speculatedType, __receiver, getProfile(), __anchor));
                FixedGuardNode __guard = graph().add(new FixedGuardNode(__condition, DeoptimizationReason.OptimizedTypeCheckViolated, DeoptimizationAction.InvalidateRecompile, false));
                graph().addBeforeFixed(invoke().asNode(), __guard);
                ValueNode __valueNode = graph().addOrUnique(new PiNode(__receiver, StampFactory.objectNonNull(__speculatedType), __guard));
                arguments().set(0, __valueNode);
                if (__speculatedType.isExact())
                {
                    setInvokeKind(InvokeKind.Special);
                }
                else
                {
                    setInvokeKind(InvokeKind.Virtual);
                }
                setTargetMethod(__singleImplementorMethod);
                return true;
            }
        }
        return false;
    }

    public JavaTypeProfile getProfile()
    {
        return profile;
    }

    @Override
    public String targetName()
    {
        if (targetMethod() == null)
        {
            return "??Invalid!";
        }
        return targetMethod().format("%h.%n");
    }

    public static MethodCallTargetNode find(StructuredGraph __graph, ResolvedJavaMethod __method)
    {
        for (MethodCallTargetNode __target : __graph.getNodes(MethodCallTargetNode.TYPE))
        {
            if (__target.targetMethod().equals(__method))
            {
                return __target;
            }
        }
        return null;
    }
}
