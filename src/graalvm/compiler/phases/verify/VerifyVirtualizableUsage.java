package graalvm.compiler.phases.verify;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeInputList;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.phases.VerifyPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 *
 * Verifies that node types implementing the {@link Virtualizable} interface use it correctly.
 * Implementors of {@link Virtualizable#virtualize(graalvm.compiler.nodes.spi.VirtualizerTool)}
 * must not apply effects on their {@link Graph graph} that cannot be easily undone.
 */
public class VerifyVirtualizableUsage extends VerifyPhase<PhaseContext> {
    @Override
    public boolean checkContract() {
        return false;
    }

    @Override
    protected boolean verify(StructuredGraph graph, PhaseContext context) {
        final ResolvedJavaType graphType = context.getMetaAccess().lookupJavaType(Graph.class);
        final ResolvedJavaType virtualizableType = context.getMetaAccess().lookupJavaType(Virtualizable.class);
        final ResolvedJavaType constantNodeType = context.getMetaAccess().lookupJavaType(ConstantNode.class);
        if (virtualizableType.isAssignableFrom(graph.method().getDeclaringClass()) && graph.method().getName().equals("virtualize")) {
            for (MethodCallTargetNode t : graph.getNodes(MethodCallTargetNode.TYPE)) {
                int bci = t.invoke().bci();
                ResolvedJavaMethod callee = t.targetMethod();
                String calleeName = callee.getName();
                if (callee.getDeclaringClass().equals(graphType)) {
                    if (calleeName.equals("add") || calleeName.equals("addWithoutUnique") || calleeName.equals("addOrUnique") || calleeName.equals("addWithoutUniqueWithInputs") ||
                                    calleeName.equals("addOrUniqueWithInputs")) {
                        verifyVirtualizableEffectArguments(constantNodeType, graph.method(), callee, bci, t.arguments(), 1);
                    }
                }
            }
        }
        return true;
    }

    private static void verifyVirtualizableEffectArguments(ResolvedJavaType constantNodeType, ResolvedJavaMethod caller, ResolvedJavaMethod callee, int bciCaller,
                    NodeInputList<? extends Node> arguments, int startIdx) {
        /*
         * Virtualizable.virtualize should never apply effects on the graph during the execution of
         * the call as the handling of loops during pea might be speculative and does not hold. We
         * should only allow nodes changing the graph that do no harm like constants.
         */
        int i = 0;
        for (Node arg : arguments) {
            if (i >= startIdx) {
                Stamp argStamp = ((ValueNode) arg).stamp(NodeView.DEFAULT);
                if (argStamp instanceof ObjectStamp) {
                    ObjectStamp objectStamp = (ObjectStamp) argStamp;
                    ResolvedJavaType argStampType = objectStamp.type();
                    if (!(argStampType.equals(constantNodeType))) {
                        StackTraceElement e = caller.asStackTraceElement(bciCaller);
                        throw new VerificationError("%s:Parameter %d in call to %s (which has effects on the graph) is not a " +
                                        "constant and thus not safe to apply during speculative virtualization.", e, i, callee.format("%H.%n(%p)"));
                    }
                }
            }
            i++;
        }
    }

}
