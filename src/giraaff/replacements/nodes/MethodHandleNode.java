package giraaff.replacements.nodes;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MethodHandleAccessProvider;
import jdk.vm.ci.meta.MethodHandleAccessProvider.IntrinsicMethod;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.AnchoringNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.extended.ValueAnchorNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

///
// Node for invocation methods defined on the class {@link MethodHandle}.
///
// @class MethodHandleNode
public final class MethodHandleNode extends MacroStateSplitNode implements Simplifiable
{
    // @def
    public static final NodeClass<MethodHandleNode> TYPE = NodeClass.create(MethodHandleNode.class);

    // @field
    protected final IntrinsicMethod ___intrinsicMethod;

    // @cons MethodHandleNode
    public MethodHandleNode(IntrinsicMethod __intrinsicMethod, CallTargetNode.InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        super(TYPE, __invokeKind, __targetMethod, __bci, __returnStamp, __arguments);
        this.___intrinsicMethod = __intrinsicMethod;
    }

    ///
    // Attempts to transform application of an intrinsifiable {@link MethodHandle} method into an
    // invocation on another method with possibly transformed arguments.
    //
    // @param methodHandleAccess objects for accessing the implementation internals of a
    //            {@link MethodHandle}
    // @param intrinsicMethod denotes the intrinsifiable {@link MethodHandle} method being processed
    // @param bci the BCI of the original {@link MethodHandle} call
    // @param returnStamp return stamp of the original {@link MethodHandle} call
    // @param arguments arguments to the original {@link MethodHandle} call
    // @return a more direct invocation derived from the {@link MethodHandle} call or null
    ///
    public static InvokeNode tryResolveTargetInvoke(MethodHandleNode.GraphAdder __adder, MethodHandleAccessProvider __methodHandleAccess, IntrinsicMethod __intrinsicMethod, ResolvedJavaMethod __original, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        switch (__intrinsicMethod)
        {
            case INVOKE_BASIC:
                return getInvokeBasicTarget(__adder, __intrinsicMethod, __methodHandleAccess, __original, __bci, __returnStamp, __arguments);
            case LINK_TO_STATIC:
            case LINK_TO_SPECIAL:
            case LINK_TO_VIRTUAL:
            case LINK_TO_INTERFACE:
                return getLinkToTarget(__adder, __intrinsicMethod, __methodHandleAccess, __original, __bci, __returnStamp, __arguments);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    ///
    // A simple utility class for adding nodes to the graph when building a MethodHandle invoke.
    ///
    // @class MethodHandleNode.GraphAdder
    public abstract static class GraphAdder
    {
        // @field
        private final StructuredGraph ___graph;

        // @cons MethodHandleNode.GraphAdder
        public GraphAdder(StructuredGraph __graph)
        {
            super();
            this.___graph = __graph;
        }

        ///
        // Call {@link StructuredGraph#addOrUnique(giraaff.graph.Node)} on {@code node}
        // and link any {@link FixedWithNextNode}s into the current control flow.
        //
        // @return the newly added node
        ///
        public abstract <T extends ValueNode> T add(T __node);

        ///
        // @return an {@link AnchoringNode} if floating guards should be created, otherwise
        //         {@link FixedGuardNode}s will be used.
        ///
        public AnchoringNode getGuardAnchor()
        {
            return null;
        }

        public Assumptions getAssumptions()
        {
            return this.___graph.getAssumptions();
        }
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        MethodHandleAccessProvider __methodHandleAccess = __tool.getConstantReflection().getMethodHandleAccess();
        ValueNode[] __argumentsArray = this.___arguments.toArray(new ValueNode[this.___arguments.size()]);

        final FixedNode __before = this;
        // @closure
        MethodHandleNode.GraphAdder adder = new MethodHandleNode.GraphAdder(graph())
        {
            @Override
            public <T extends ValueNode> T add(T __node)
            {
                T __added = graph().addOrUnique(__node);
                if (__added instanceof FixedWithNextNode)
                {
                    graph().addBeforeFixed(__before, (FixedWithNextNode) __added);
                }
                return __added;
            }
        };
        InvokeNode __invoke = tryResolveTargetInvoke(adder, __methodHandleAccess, this.___intrinsicMethod, this.___targetMethod, this.___bci, this.___returnStamp, __argumentsArray);
        if (__invoke != null)
        {
            __invoke = graph().addOrUniqueWithInputs(__invoke);
            __invoke.setStateAfter(stateAfter());
            FixedNode __currentNext = next();
            replaceAtUsages(__invoke);
            GraphUtil.removeFixedWithUnusedInputs(this);
            graph().addBeforeFixed(__currentNext, __invoke);
        }
    }

    ///
    // Get the receiver of a MethodHandle.invokeBasic call.
    //
    // @return the receiver argument node
    ///
    private static ValueNode getReceiver(ValueNode[] __arguments)
    {
        return __arguments[0];
    }

    ///
    // Get the MemberName argument of a MethodHandle.linkTo* call.
    //
    // @return the MemberName argument node (which is the last argument)
    ///
    private static ValueNode getMemberName(ValueNode[] __arguments)
    {
        return __arguments[__arguments.length - 1];
    }

    ///
    // Used for the MethodHandle.invokeBasic method (the {@link IntrinsicMethod#INVOKE_BASIC }
    // method) to get the target {@link InvokeNode} if the method handle receiver is constant.
    //
    // @return invoke node for the {@link java.lang.invoke.MethodHandle} target
    ///
    private static InvokeNode getInvokeBasicTarget(MethodHandleNode.GraphAdder __adder, IntrinsicMethod __intrinsicMethod, MethodHandleAccessProvider __methodHandleAccess, ResolvedJavaMethod __original, int __bci, StampPair __returnStamp, ValueNode[] __arguments)
    {
        ValueNode __methodHandleNode = getReceiver(__arguments);
        if (__methodHandleNode.isConstant())
        {
            return getTargetInvokeNode(__adder, __intrinsicMethod, __bci, __returnStamp, __arguments, __methodHandleAccess.resolveInvokeBasicTarget(__methodHandleNode.asJavaConstant(), true), __original);
        }
        return null;
    }

    ///
    // Used for the MethodHandle.linkTo* methods (the {@link IntrinsicMethod#LINK_TO_STATIC},
    // {@link IntrinsicMethod#LINK_TO_SPECIAL}, {@link IntrinsicMethod#LINK_TO_VIRTUAL}, and
    // {@link IntrinsicMethod#LINK_TO_INTERFACE} methods) to get the target {@link InvokeNode} if
    // the member name argument is constant.
    //
    // @return invoke node for the member name target
    ///
    private static InvokeNode getLinkToTarget(MethodHandleNode.GraphAdder __adder, IntrinsicMethod __intrinsicMethod, MethodHandleAccessProvider __methodHandleAccess, ResolvedJavaMethod __original, int __bci, StampPair __returnStamp, ValueNode[] __arguments)
    {
        ValueNode __memberNameNode = getMemberName(__arguments);
        if (__memberNameNode.isConstant())
        {
            return getTargetInvokeNode(__adder, __intrinsicMethod, __bci, __returnStamp, __arguments, __methodHandleAccess.resolveLinkToTarget(__memberNameNode.asJavaConstant()), __original);
        }
        return null;
    }

    ///
    // Helper function to get the {@link InvokeNode} for the targetMethod of a java.lang.invoke.MemberName.
    //
    // @param target the target, already loaded from the member name node
    //
    // @return invoke node for the member name target
    ///
    private static InvokeNode getTargetInvokeNode(MethodHandleNode.GraphAdder __adder, IntrinsicMethod __intrinsicMethod, int __bci, StampPair __returnStamp, ValueNode[] __originalArguments, ResolvedJavaMethod __target, ResolvedJavaMethod __original)
    {
        if (__target == null)
        {
            return null;
        }

        // In lambda forms we erase signature types to avoid resolving issues involving
        // class loaders. When we optimize a method handle invoke to a direct call
        // we must cast the receiver and arguments to its actual types.
        Signature __signature = __target.getSignature();
        final boolean __isStatic = __target.isStatic();
        final int __receiverSkip = __isStatic ? 0 : 1;

        Assumptions __assumptions = __adder.getAssumptions();
        ResolvedJavaMethod __realTarget = null;
        if (__target.canBeStaticallyBound())
        {
            __realTarget = __target;
        }
        else
        {
            ResolvedJavaType __targetType = __target.getDeclaringClass();
            // try to bind based on the declaredType
            AssumptionResult<ResolvedJavaMethod> __concreteMethod = __targetType.findUniqueConcreteMethod(__target);
            if (__concreteMethod == null)
            {
                // try to get the most accurate receiver type
                if (__intrinsicMethod == IntrinsicMethod.LINK_TO_VIRTUAL || __intrinsicMethod == IntrinsicMethod.LINK_TO_INTERFACE)
                {
                    ValueNode __receiver = getReceiver(__originalArguments);
                    TypeReference __receiverType = StampTool.typeReferenceOrNull(__receiver.stamp(NodeView.DEFAULT));
                    if (__receiverType != null)
                    {
                        __concreteMethod = __receiverType.getType().findUniqueConcreteMethod(__target);
                    }
                }
            }
            if (__concreteMethod != null && __concreteMethod.canRecordTo(__assumptions))
            {
                __concreteMethod.recordTo(__assumptions);
                __realTarget = __concreteMethod.getResult();
            }
        }

        if (__realTarget != null)
        {
            // don't mutate the passed in arguments
            ValueNode[] __arguments = __originalArguments.clone();

            // cast receiver to its type
            if (!__isStatic)
            {
                JavaType __receiverType = __target.getDeclaringClass();
                maybeCastArgument(__adder, __arguments, 0, __receiverType);
            }

            // cast reference arguments to its type
            for (int __index = 0; __index < __signature.getParameterCount(false); __index++)
            {
                JavaType __parameterType = __signature.getParameterType(__index, __target.getDeclaringClass());
                maybeCastArgument(__adder, __arguments, __receiverSkip + __index, __parameterType);
            }
            return createTargetInvokeNode(__assumptions, __intrinsicMethod, __realTarget, __original, __bci, __returnStamp, __arguments);
        }
        return null;
    }

    ///
    // Inserts a node to cast the argument at index to the given type if the given type is more
    // concrete than the argument type.
    //
    // @param index of the argument to be cast
    // @param type the type the argument should be cast to
    ///
    private static void maybeCastArgument(MethodHandleNode.GraphAdder __adder, ValueNode[] __arguments, int __index, JavaType __type)
    {
        ValueNode __argument = __arguments[__index];
        if (__type instanceof ResolvedJavaType && !((ResolvedJavaType) __type).isJavaLangObject())
        {
            Assumptions __assumptions = __adder.getAssumptions();
            TypeReference __targetType = TypeReference.create(__assumptions, (ResolvedJavaType) __type);
            // When an argument is a Word type, we can have a mismatch of primitive/object types here.
            // Not inserting a PiNode is a safe fallback, and Word types need no additional type information anyway.
            if (__targetType != null && !__targetType.getType().isPrimitive() && !__argument.getStackKind().isPrimitive())
            {
                ResolvedJavaType __argumentType = StampTool.typeOrNull(__argument.stamp(NodeView.DEFAULT));
                if (__argumentType == null || (__argumentType.isAssignableFrom(__targetType.getType()) && !__argumentType.equals(__targetType.getType())))
                {
                    LogicNode __inst = InstanceOfNode.createAllowNull(__targetType, __argument, null, null);
                    if (!__inst.isTautology())
                    {
                        __inst = __adder.add(__inst);
                        AnchoringNode __guardAnchor = __adder.getGuardAnchor();
                        DeoptimizationReason __reason = DeoptimizationReason.ClassCastException;
                        DeoptimizationAction __action = DeoptimizationAction.InvalidateRecompile;
                        JavaConstant __speculation = JavaConstant.NULL_POINTER;
                        GuardingNode __guard;
                        if (__guardAnchor == null)
                        {
                            FixedGuardNode __fixedGuard = __adder.add(new FixedGuardNode(__inst, __reason, __action, __speculation, false));
                            __guard = __fixedGuard;
                        }
                        else
                        {
                            GuardNode __newGuard = __adder.add(new GuardNode(__inst, __guardAnchor, __reason, __action, false, __speculation));
                            __adder.add(new ValueAnchorNode(__newGuard));
                            __guard = __newGuard;
                        }
                        ValueNode __valueNode = __adder.add(PiNode.create(__argument, StampFactory.object(__targetType), __guard.asNode()));
                        __arguments[__index] = __valueNode;
                    }
                }
            }
        }
    }

    ///
    // Creates an {@link InvokeNode} for the given target method. The {@link CallTargetNode} passed
    // to the InvokeNode is in fact a {@link ResolvedMethodHandleCallTargetNode}.
    //
    // @return invoke node for the member name target
    ///
    private static InvokeNode createTargetInvokeNode(Assumptions __assumptions, IntrinsicMethod __intrinsicMethod, ResolvedJavaMethod __target, ResolvedJavaMethod __original, int __bci, StampPair __returnStamp, ValueNode[] __arguments)
    {
        CallTargetNode.InvokeKind __targetInvokeKind = __target.isStatic() ? CallTargetNode.InvokeKind.Static : CallTargetNode.InvokeKind.Special;
        JavaType __targetReturnType = __target.getSignature().getReturnType(null);

        // MethodHandleLinkTo* nodes have a trailing MemberName argument which needs to be popped.
        ValueNode[] __targetArguments;
        switch (__intrinsicMethod)
        {
            case INVOKE_BASIC:
            {
                __targetArguments = __arguments;
                break;
            }
            case LINK_TO_STATIC:
            case LINK_TO_SPECIAL:
            case LINK_TO_VIRTUAL:
            case LINK_TO_INTERFACE:
            {
                __targetArguments = Arrays.copyOfRange(__arguments, 0, __arguments.length - 1);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        StampPair __targetReturnStamp = StampFactory.forDeclaredType(__assumptions, __targetReturnType, false);

        MethodCallTargetNode __callTarget = ResolvedMethodHandleCallTargetNode.create(__targetInvokeKind, __target, __targetArguments, __targetReturnStamp, __original, __arguments, __returnStamp);

        // The call target can have a different return type than the invoker, e.g. the target returns
        // an Object but the invoker void. In this case we need to use the stamp of the invoker.
        // Note: always using the invoker's stamp would be wrong because it's a less concrete type
        // (usually java.lang.Object).
        if (__returnStamp.getTrustedStamp().getStackKind() == JavaKind.Void)
        {
            return new InvokeNode(__callTarget, __bci, StampFactory.forVoid());
        }
        else
        {
            return new InvokeNode(__callTarget, __bci);
        }
    }
}
