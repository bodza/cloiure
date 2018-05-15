package graalvm.compiler.truffle.compiler.phases;

import graalvm.compiler.graph.VerificationError;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.Phase;
import graalvm.compiler.truffle.compiler.nodes.frame.NewFrameNode;

/**
 * Compiler phase for verifying that the Truffle virtual frame does not escape and can therefore be
 * escape analyzed.
 */
public class VerifyFrameDoesNotEscapePhase extends Phase {

    @Override
    protected void run(StructuredGraph graph) {
        for (NewFrameNode virtualFrame : graph.getNodes(NewFrameNode.TYPE)) {
            for (MethodCallTargetNode callTarget : virtualFrame.usages().filter(MethodCallTargetNode.class)) {
                if (callTarget.invoke() != null) {
                    String properties = callTarget.getDebugProperties().toString();
                    String arguments = callTarget.arguments().toString();
                    Throwable exception = new VerificationError("Frame escapes at: %s#%s\nproperties:%s\narguments: %s", callTarget, callTarget.targetMethod(), properties, arguments);
                    throw GraphUtil.approxSourceException(callTarget, exception);
                }
            }
        }
    }
}
