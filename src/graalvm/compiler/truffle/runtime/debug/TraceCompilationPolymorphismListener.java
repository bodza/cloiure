package graalvm.compiler.truffle.runtime.debug;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleCompilationPolymorphism;

import java.util.LinkedHashMap;
import java.util.Map;

import graalvm.compiler.truffle.common.TruffleCompilerListener.CompilationResultInfo;
import graalvm.compiler.truffle.common.TruffleCompilerListener.GraphInfo;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.runtime.AbstractGraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.TruffleInlining;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeUtil;

public final class TraceCompilationPolymorphismListener extends AbstractGraalTruffleRuntimeListener {

    private TraceCompilationPolymorphismListener(GraalTruffleRuntime runtime) {
        super(runtime);
    }

    public static void install(GraalTruffleRuntime runtime) {
        if (TruffleCompilerOptions.getValue(TraceTruffleCompilationPolymorphism)) {
            runtime.addListener(new TraceCompilationPolymorphismListener(runtime));
        }
    }

    @Override
    public void onCompilationSuccess(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graph, CompilationResultInfo result) {
        for (Node node : target.nodeIterable(inliningDecision)) {
            if (node != null && (node.getCost() == NodeCost.MEGAMORPHIC || node.getCost() == NodeCost.POLYMORPHIC)) {
                NodeCost cost = node.getCost();
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("simpleName", node.getClass().getSimpleName());
                props.put("subtree", "\n" + NodeUtil.printCompactTreeToString(node));
                String msg = cost == NodeCost.MEGAMORPHIC ? "megamorphic" : "polymorphic";
                runtime.logEvent(0, msg, node.toString(), props);
            }
        }
    }

}
