package giraaff.hotspot.replacements;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.CallTargetNode.InvokeKind;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.InvokeNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.nodes.MacroStateSplitNode;
import giraaff.util.GraalError;

// @class ReflectionGetCallerClassNode
public final class ReflectionGetCallerClassNode extends MacroStateSplitNode implements Canonicalizable, Lowerable
{
    // @def
    public static final NodeClass<ReflectionGetCallerClassNode> TYPE = NodeClass.create(ReflectionGetCallerClassNode.class);

    // @cons
    public ReflectionGetCallerClassNode(InvokeKind __invokeKind, ResolvedJavaMethod __targetMethod, int __bci, StampPair __returnStamp, ValueNode... __arguments)
    {
        super(TYPE, __invokeKind, __targetMethod, __bci, __returnStamp, __arguments);
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        ConstantNode __callerClassNode = getCallerClassNode(__tool.getMetaAccess(), __tool.getConstantReflection());
        if (__callerClassNode != null)
        {
            return __callerClassNode;
        }
        return this;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        ConstantNode __callerClassNode = getCallerClassNode(__tool.getMetaAccess(), __tool.getConstantReflection());

        if (__callerClassNode != null)
        {
            graph().replaceFixedWithFloating(this, graph().addOrUniqueWithInputs(__callerClassNode));
        }
        else
        {
            InvokeNode __invoke = createInvoke();
            graph().replaceFixedWithFixed(this, __invoke);
            __invoke.lower(__tool);
        }
    }

    ///
    // If inlining is deep enough this method returns a {@link ConstantNode} of the caller class by walking the stack.
    //
    // @return ConstantNode of the caller class, or null
    ///
    private ConstantNode getCallerClassNode(MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection)
    {
        // Walk back up the frame states to find the caller at the required depth.
        FrameState __state = stateAfter();

        // cf. JVM_GetCallerClass
        // NOTE: Start the loop at depth 1 because the current frame state does not
        // include the Reflection.getCallerClass() frame.
        for (int __n = 1; __state != null; __state = __state.outerFrameState(), __n++)
        {
            HotSpotResolvedJavaMethod __method = (HotSpotResolvedJavaMethod) __state.getMethod();
            switch (__n)
            {
                case 0:
                    throw GraalError.shouldNotReachHere("current frame state does not include the Reflection.getCallerClass frame");
                case 1:
                    // Frame 0 and 1 must be caller sensitive (see JVM_GetCallerClass).
                    if (!__method.isCallerSensitive())
                    {
                        return null; // bail-out; let JVM_GetCallerClass do the work
                    }
                    break;
                default:
                    if (!__method.ignoredBySecurityStackWalk())
                    {
                        // We have reached the desired frame; return the holder class.
                        HotSpotResolvedObjectType __callerClass = __method.getDeclaringClass();
                        return ConstantNode.forConstant(__constantReflection.asJavaClass(__callerClass), __metaAccess);
                    }
                    break;
            }
        }
        return null; // bail-out; let JVM_GetCallerClass do the work
    }
}
