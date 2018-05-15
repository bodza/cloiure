package graalvm.compiler.hotspot.replacements;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.replacements.nodes.MacroStateSplitNode;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

@NodeInfo
public final class ReflectionGetCallerClassNode extends MacroStateSplitNode implements Canonicalizable, Lowerable {

    public static final NodeClass<ReflectionGetCallerClassNode> TYPE = NodeClass.create(ReflectionGetCallerClassNode.class);

    public ReflectionGetCallerClassNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, int bci, StampPair returnStamp, ValueNode... arguments) {
        super(TYPE, invokeKind, targetMethod, bci, returnStamp, arguments);
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        ConstantNode callerClassNode = getCallerClassNode(tool.getMetaAccess(), tool.getConstantReflection());
        if (callerClassNode != null) {
            return callerClassNode;
        }
        return this;
    }

    @Override
    public void lower(LoweringTool tool) {
        ConstantNode callerClassNode = getCallerClassNode(tool.getMetaAccess(), tool.getConstantReflection());

        if (callerClassNode != null) {
            graph().replaceFixedWithFloating(this, graph().addOrUniqueWithInputs(callerClassNode));
        } else {
            InvokeNode invoke = createInvoke();
            graph().replaceFixedWithFixed(this, invoke);
            invoke.lower(tool);
        }
    }

    /**
     * If inlining is deep enough this method returns a {@link ConstantNode} of the caller class by
     * walking the stack.
     *
     * @param metaAccess
     * @return ConstantNode of the caller class, or null
     */
    private ConstantNode getCallerClassNode(MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection) {
        // Walk back up the frame states to find the caller at the required depth.
        FrameState state = stateAfter();

        // Cf. JVM_GetCallerClass
        // NOTE: Start the loop at depth 1 because the current frame state does
        // not include the Reflection.getCallerClass() frame.
        for (int n = 1; state != null; state = state.outerFrameState(), n++) {
            HotSpotResolvedJavaMethod method = (HotSpotResolvedJavaMethod) state.getMethod();
            switch (n) {
                case 0:
                    throw GraalError.shouldNotReachHere("current frame state does not include the Reflection.getCallerClass frame");
                case 1:
                    // Frame 0 and 1 must be caller sensitive (see JVM_GetCallerClass).
                    if (!method.isCallerSensitive()) {
                        return null;  // bail-out; let JVM_GetCallerClass do the work
                    }
                    break;
                default:
                    if (!method.ignoredBySecurityStackWalk()) {
                        // We have reached the desired frame; return the holder class.
                        HotSpotResolvedObjectType callerClass = method.getDeclaringClass();
                        return ConstantNode.forConstant(constantReflection.asJavaClass(callerClass), metaAccess);
                    }
                    break;
            }
        }
        return null;  // bail-out; let JVM_GetCallerClass do the work
    }

}
