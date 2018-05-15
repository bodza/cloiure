package graalvm.compiler.truffle.runtime.debug;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleInlining;
import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TruffleFunctionInlining;

import graalvm.compiler.truffle.common.TruffleCompilerListener.GraphInfo;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.runtime.AbstractGraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.TruffleInlining;
import graalvm.compiler.truffle.runtime.TruffleInliningDecision;
import graalvm.compiler.truffle.runtime.TruffleInliningProfile;

public final class TraceInliningListener extends AbstractGraalTruffleRuntimeListener {

    private TraceInliningListener(GraalTruffleRuntime runtime) {
        super(runtime);
    }

    public static void install(GraalTruffleRuntime runtime) {
        if (TruffleCompilerOptions.getValue(TraceTruffleInlining)) {
            runtime.addListener(new TraceInliningListener(runtime));
        }
    }

    @Override
    public void onCompilationTruffleTierFinished(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graph) {
        if (inliningDecision == null) {
            return;
        }
        if (TruffleCompilerOptions.getValue(TruffleFunctionInlining)) {
            runtime.logEvent(0, "inline start", target.toString(), target.getDebugProperties(null));
            logInliningDecisionRecursive(target, inliningDecision, 1);
            runtime.logEvent(0, "inline done", target.toString(), target.getDebugProperties(inliningDecision));
        } else {
            runtime.logEvent(0, "TruffleFunctionInlining is set to false", "", null);
            return;
        }
    }

    private void logInliningDecisionRecursive(OptimizedCallTarget target, TruffleInlining inliningDecision, int depth) {
        for (TruffleInliningDecision decision : inliningDecision) {
            TruffleInliningProfile profile = decision.getProfile();
            boolean inlined = decision.shouldInline();
            String msg = inlined ? "inline success" : "inline failed";
            runtime.logEvent(depth, msg, decision.getProfile().getCallNode().getCurrentCallTarget().toString(), profile.getDebugProperties());
            if (inlined) {
                logInliningDecisionRecursive(target, decision, depth + 1);
            }
        }
    }

}
