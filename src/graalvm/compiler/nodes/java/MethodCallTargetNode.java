package graalvm.compiler.nodes.java;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.core.common.type.TypeReference;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.CallTargetNode;
import graalvm.compiler.nodes.FixedGuardNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.ValueAnchorNode;
import graalvm.compiler.nodes.spi.UncheckedInterfaceProvider;
import graalvm.compiler.nodes.type.StampTool;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

@NodeInfo
public class MethodCallTargetNode extends CallTargetNode implements IterableNodeType, Simplifiable
{
    public static final NodeClass<MethodCallTargetNode> TYPE = NodeClass.create(MethodCallTargetNode.class);
    protected JavaTypeProfile profile;

    public MethodCallTargetNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] arguments, StampPair returnStamp, JavaTypeProfile profile)
    {
        this(TYPE, invokeKind, targetMethod, arguments, returnStamp, profile);
    }

    protected MethodCallTargetNode(NodeClass<? extends MethodCallTargetNode> c, InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] arguments, StampPair returnStamp, JavaTypeProfile profile)
    {
        super(c, arguments, targetMethod, invokeKind, returnStamp);
        this.profile = profile;
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

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Long)
        {
            return super.toString(Verbosity.Short) + "(" + targetMethod() + ")";
        }
        else
        {
            return super.toString(verbosity);
        }
    }

    public static ResolvedJavaMethod findSpecialCallTarget(InvokeKind invokeKind, ValueNode receiver, ResolvedJavaMethod targetMethod, ResolvedJavaType contextType)
    {
        if (invokeKind.isDirect())
        {
            return null;
        }

        // check for trivial cases (e.g. final methods, nonvirtual methods)
        if (targetMethod.canBeStaticallyBound())
        {
            return targetMethod;
        }

        return devirtualizeCall(invokeKind, targetMethod, contextType, receiver.graph().getAssumptions(), receiver.stamp(NodeView.DEFAULT));
    }

    public static ResolvedJavaMethod devirtualizeCall(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ResolvedJavaType contextType, Assumptions assumptions, Stamp receiverStamp)
    {
        TypeReference type = StampTool.typeReferenceOrNull(receiverStamp);
        if (type == null && invokeKind == InvokeKind.Virtual)
        {
            // For virtual calls, we are guaranteed to receive a correct receiver type.
            type = TypeReference.createTrusted(assumptions, targetMethod.getDeclaringClass());
        }

        if (type != null)
        {
            /*
             * either the holder class is exact, or the receiver object has an exact type, or it's
             * an array type
             */
            ResolvedJavaMethod resolvedMethod = type.getType().resolveConcreteMethod(targetMethod, contextType);
            if (resolvedMethod != null && (resolvedMethod.canBeStaticallyBound() || type.isExact() || type.getType().isArray()))
            {
                return resolvedMethod;
            }

            AssumptionResult<ResolvedJavaMethod> uniqueConcreteMethod = type.getType().findUniqueConcreteMethod(targetMethod);
            if (uniqueConcreteMethod != null && uniqueConcreteMethod.canRecordTo(assumptions))
            {
                uniqueConcreteMethod.recordTo(assumptions);
                return uniqueConcreteMethod.getResult();
            }
        }
        return null;
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        // attempt to devirtualize the call
        if (invoke().getContextMethod() == null)
        {
            // avoid invokes that have placeholder bcis: they do not have a valid contextType
            return;
        }
        ResolvedJavaType contextType = (invoke().stateAfter() == null && invoke().stateDuring() == null) ? null : invoke().getContextType();
        ResolvedJavaMethod specialCallTarget = findSpecialCallTarget(invokeKind, receiver(), targetMethod, contextType);
        if (specialCallTarget != null)
        {
            this.setTargetMethod(specialCallTarget);
            setInvokeKind(InvokeKind.Special);
            return;
        }

        Assumptions assumptions = graph().getAssumptions();
        /*
         * Even though we are not registering an assumption (see comment below), the optimization is
         * only valid when speculative optimizations are enabled.
         */
        if (invokeKind().isIndirect() && invokeKind().isInterface() && assumptions != null)
        {
            // check if the type of the receiver can narrow the result
            ValueNode receiver = receiver();

            // try to turn a interface call into a virtual call
            ResolvedJavaType declaredReceiverType = targetMethod().getDeclaringClass();

            /*
             * We need to check the invoke kind to avoid recursive simplification for virtual
             * interface methods calls.
             */
            if (declaredReceiverType.isInterface())
            {
                ResolvedJavaType singleImplementor = declaredReceiverType.getSingleImplementor();
                if (singleImplementor != null && !singleImplementor.equals(declaredReceiverType))
                {
                    TypeReference speculatedType = TypeReference.createTrusted(assumptions, singleImplementor);
                    if (tryCheckCastSingleImplementor(receiver, speculatedType))
                    {
                        return;
                    }
                }
            }

            if (receiver instanceof UncheckedInterfaceProvider)
            {
                UncheckedInterfaceProvider uncheckedInterfaceProvider = (UncheckedInterfaceProvider) receiver;
                Stamp uncheckedStamp = uncheckedInterfaceProvider.uncheckedStamp();
                if (uncheckedStamp != null)
                {
                    TypeReference speculatedType = StampTool.typeReferenceOrNull(uncheckedStamp);
                    if (speculatedType != null)
                    {
                        tryCheckCastSingleImplementor(receiver, speculatedType);
                    }
                }
            }
        }
    }

    private boolean tryCheckCastSingleImplementor(ValueNode receiver, TypeReference speculatedType)
    {
        ResolvedJavaType singleImplementor = speculatedType.getType();
        if (singleImplementor != null)
        {
            ResolvedJavaMethod singleImplementorMethod = singleImplementor.resolveConcreteMethod(targetMethod(), invoke().getContextType());
            if (singleImplementorMethod != null)
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
                ValueAnchorNode anchor = new ValueAnchorNode(null);
                if (anchor != null)
                {
                    graph().add(anchor);
                    graph().addBeforeFixed(invoke().asNode(), anchor);
                }
                LogicNode condition = graph().addOrUniqueWithInputs(InstanceOfNode.create(speculatedType, receiver, getProfile(), anchor));
                FixedGuardNode guard = graph().add(new FixedGuardNode(condition, DeoptimizationReason.OptimizedTypeCheckViolated, DeoptimizationAction.InvalidateRecompile, false));
                graph().addBeforeFixed(invoke().asNode(), guard);
                ValueNode valueNode = graph().addOrUnique(new PiNode(receiver, StampFactory.objectNonNull(speculatedType), guard));
                arguments().set(0, valueNode);
                if (speculatedType.isExact())
                {
                    setInvokeKind(InvokeKind.Special);
                }
                else
                {
                    setInvokeKind(InvokeKind.Virtual);
                }
                setTargetMethod(singleImplementorMethod);
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

    public static MethodCallTargetNode find(StructuredGraph graph, ResolvedJavaMethod method)
    {
        for (MethodCallTargetNode target : graph.getNodes(MethodCallTargetNode.TYPE))
        {
            if (target.targetMethod().equals(method))
            {
                return target;
            }
        }
        return null;
    }
}
