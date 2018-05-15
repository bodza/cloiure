package graalvm.compiler.truffle.runtime.debug;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleCompilationAST;

import java.util.List;

import graalvm.compiler.truffle.common.TruffleCompilerListener.CompilationResultInfo;
import graalvm.compiler.truffle.common.TruffleCompilerListener.GraphInfo;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.runtime.AbstractGraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.TruffleInlining;
import graalvm.compiler.truffle.runtime.TruffleInlining.CallTreeNodeVisitor;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeClass;

/**
 * Traces all polymorphic and generic nodes after each successful Truffle compilation.
 */
public final class TraceASTCompilationListener extends AbstractGraalTruffleRuntimeListener {

    private TraceASTCompilationListener(GraalTruffleRuntime runtime) {
        super(runtime);
    }

    public static void install(GraalTruffleRuntime runtime) {
        if (TruffleCompilerOptions.getValue(TraceTruffleCompilationAST)) {
            runtime.addListener(new TraceASTCompilationListener(runtime));
        }
    }

    @Override
    public void onCompilationSuccess(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graphInfo, CompilationResultInfo compilationResultInfo) {
        runtime.logEvent(0, "opt AST", target.toString(), target.getDebugProperties(inliningDecision));
        printCompactTree(target, inliningDecision);
    }

    private void printCompactTree(OptimizedCallTarget target, TruffleInlining inliningDecision) {
        target.accept(new CallTreeNodeVisitor() {

            @Override
            public boolean visit(List<TruffleInlining> decisionStack, Node node) {
                if (node == null) {
                    return true;
                }
                int level = CallTreeNodeVisitor.getNodeDepth(decisionStack, node);
                StringBuilder indent = new StringBuilder();
                for (int i = 0; i < level; i++) {
                    indent.append("  ");
                }
                Node parent = node.getParent();

                if (parent == null) {
                    runtime.log(String.format("%s%s", indent, node.getClass().getSimpleName()));
                } else {
                    String fieldName = getFieldName(parent, node);
                    runtime.log(String.format("%s%s = %s", indent, fieldName, node.getClass().getSimpleName()));
                }
                return true;
            }

            @SuppressWarnings("deprecation")
            private String getFieldName(Node parent, Node node) {
                for (com.oracle.truffle.api.nodes.NodeFieldAccessor field : NodeClass.get(parent).getFields()) {
                    Object value = field.loadValue(parent);
                    if (value == node) {
                        return field.getName();
                    } else if (value instanceof Node[]) {
                        int index = 0;
                        for (Node arrayNode : (Node[]) value) {
                            if (arrayNode == node) {
                                return field.getName() + "[" + index + "]";
                            }
                            index++;
                        }
                    }
                }
                return "unknownField";
            }

        }, inliningDecision);
    }
}
