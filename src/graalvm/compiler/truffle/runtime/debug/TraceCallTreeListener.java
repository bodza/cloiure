package graalvm.compiler.truffle.runtime.debug;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleCompilationCallTree;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import graalvm.compiler.truffle.common.TruffleCompilerListener.CompilationResultInfo;
import graalvm.compiler.truffle.common.TruffleCompilerListener.GraphInfo;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.runtime.AbstractGraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.OptimizedDirectCallNode;
import graalvm.compiler.truffle.runtime.OptimizedIndirectCallNode;
import graalvm.compiler.truffle.runtime.TruffleInlining;
import graalvm.compiler.truffle.runtime.TruffleInlining.CallTreeNodeVisitor;
import graalvm.compiler.truffle.runtime.TruffleInliningDecision;

import com.oracle.truffle.api.nodes.Node;

/**
 * Traces the inlined Truffle call tree after each successful Truffle compilation.
 */
public final class TraceCallTreeListener extends AbstractGraalTruffleRuntimeListener {

    private TraceCallTreeListener(GraalTruffleRuntime runtime) {
        super(runtime);
    }

    public static void install(GraalTruffleRuntime runtime) {
        if (TruffleCompilerOptions.getValue(TraceTruffleCompilationCallTree)) {
            runtime.addListener(new TraceCallTreeListener(runtime));
        }
    }

    @Override
    public void onCompilationSuccess(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graphInfo, CompilationResultInfo compilationResultInfo) {
        runtime.logEvent(0, "opt call tree", target.toString(), target.getDebugProperties(inliningDecision));
        logTruffleCallTree(target, inliningDecision);
    }

    private void logTruffleCallTree(OptimizedCallTarget compilable, TruffleInlining inliningDecision) {
        CallTreeNodeVisitor visitor = new CallTreeNodeVisitor() {

            @Override
            public boolean visit(List<TruffleInlining> decisionStack, Node node) {
                if (node instanceof OptimizedDirectCallNode) {
                    OptimizedDirectCallNode callNode = ((OptimizedDirectCallNode) node);
                    int depth = decisionStack == null ? 0 : decisionStack.size() - 1;
                    TruffleInliningDecision inlining = CallTreeNodeVisitor.getCurrentInliningDecision(decisionStack);
                    String dispatched = "<dispatched>";
                    if (inlining != null && inlining.shouldInline()) {
                        dispatched = "";
                    }
                    Map<String, Object> properties = new LinkedHashMap<>();
                    GraalTruffleRuntimeListener.addASTSizeProperty(callNode.getCurrentCallTarget(), inliningDecision, properties);
                    properties.putAll(callNode.getCurrentCallTarget().getDebugProperties(inliningDecision));
                    runtime.logEvent(depth, "opt call tree", callNode.getCurrentCallTarget().toString() + dispatched, properties);
                } else if (node instanceof OptimizedIndirectCallNode) {
                    int depth = decisionStack == null ? 0 : decisionStack.size() - 1;
                    runtime.logEvent(depth, "opt call tree", "<indirect>", new LinkedHashMap<>());
                }
                return true;
            }

        };
        compilable.accept(visitor, inliningDecision);
    }

}
