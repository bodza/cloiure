package graalvm.compiler.truffle.runtime.debug;

import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleCompilation;
import static graalvm.compiler.truffle.common.TruffleCompilerOptions.TraceTruffleCompilationDetails;

import java.util.LinkedHashMap;
import java.util.Map;

import graalvm.compiler.truffle.common.TruffleCompilerListener.CompilationResultInfo;
import graalvm.compiler.truffle.common.TruffleCompilerListener.GraphInfo;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.runtime.AbstractGraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;
import graalvm.compiler.truffle.runtime.OptimizedDirectCallNode;
import graalvm.compiler.truffle.runtime.TruffleInlining;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Traces AST-level compilation events with a detailed log message sent to the Truffle log stream
 * for each event.
 */
public final class TraceCompilationListener extends AbstractGraalTruffleRuntimeListener {

    private final ThreadLocal<Times> currentCompilation = new ThreadLocal<>();

    private TraceCompilationListener(GraalTruffleRuntime runtime) {
        super(runtime);
    }

    public static void install(GraalTruffleRuntime runtime) {
        if (TruffleCompilerOptions.getValue(TraceTruffleCompilation) || TruffleCompilerOptions.getValue(TraceTruffleCompilationDetails)) {
            runtime.addListener(new TraceCompilationListener(runtime));
        }
    }

    @Override
    public void onCompilationQueued(OptimizedCallTarget target) {
        if (TruffleCompilerOptions.getValue(TraceTruffleCompilationDetails)) {
            runtime.logEvent(0, "opt queued", target.toString(), target.getDebugProperties(null));
        }
    }

    @Override
    public void onCompilationDequeued(OptimizedCallTarget target, Object source, CharSequence reason) {
        if (TruffleCompilerOptions.getValue(TraceTruffleCompilationDetails)) {
            Map<String, Object> properties = new LinkedHashMap<>();
            addSourceInfo(properties, source);
            properties.put("Reason", reason);
            runtime.logEvent(0, "opt unqueued", target.toString(), properties);
        }
    }

    @Override
    public void onCompilationFailed(OptimizedCallTarget target, String reason, boolean bailout, boolean permanentBailout) {
        if (!TraceCompilationFailureListener.isPermanentFailure(bailout, permanentBailout)) {
            onCompilationDequeued(target, null, "Non permanent bailout: " + reason);
        }
        currentCompilation.set(null);

    }

    @Override
    public void onCompilationStarted(OptimizedCallTarget target) {
        if (TruffleCompilerOptions.getValue(TraceTruffleCompilationDetails)) {
            runtime.logEvent(0, "opt start", target.toString(), target.getDebugProperties(null));
        }
        currentCompilation.set(new Times());
    }

    @Override
    public void onCompilationDeoptimized(OptimizedCallTarget target, Frame frame) {
        runtime.logEvent(0, "opt deopt", target.toString(), target.getDebugProperties(null));
    }

    @Override
    public void onCompilationTruffleTierFinished(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graph) {
        final Times current = currentCompilation.get();
        current.timePartialEvaluationFinished = System.nanoTime();
        current.nodeCountPartialEval = graph.getNodeCount();
    }

    @Override
    public void onCompilationSuccess(OptimizedCallTarget target, TruffleInlining inliningDecision, GraphInfo graph, CompilationResultInfo result) {
        long timeCompilationFinished = System.nanoTime();
        int nodeCountLowered = graph.getNodeCount();
        Times compilation = currentCompilation.get();

        int calls = 0;
        int inlinedCalls;
        if (inliningDecision == null) {

            for (Node node : target.nodeIterable(null)) {
                if (node instanceof OptimizedDirectCallNode) {
                    calls++;
                }
            }

            inlinedCalls = 0;
        } else {
            calls = inliningDecision.countCalls();
            inlinedCalls = inliningDecision.countInlinedCalls();
        }

        int dispatchedCalls = calls - inlinedCalls;
        Map<String, Object> properties = new LinkedHashMap<>();
        GraalTruffleRuntimeListener.addASTSizeProperty(target, inliningDecision, properties);
        properties.put("Time", String.format("%5.0f(%4.0f+%-4.0f)ms", //
                        (timeCompilationFinished - compilation.timeCompilationStarted) / 1e6, //
                        (compilation.timePartialEvaluationFinished - compilation.timeCompilationStarted) / 1e6, //
                        (timeCompilationFinished - compilation.timePartialEvaluationFinished) / 1e6));
        properties.put("DirectCallNodes", String.format("I %4d/D %4d", inlinedCalls, dispatchedCalls));
        properties.put("GraalNodes", String.format("%5d/%5d", compilation.nodeCountPartialEval, nodeCountLowered));
        properties.put("CodeSize", result.getTargetCodeSize());
        properties.put("CodeAddress", "0x" + Long.toHexString(target.getCodeAddress()));
        properties.put("Source", formatSourceSection(target.getRootNode().getSourceSection()));

        runtime.logEvent(0, "opt done", target.toString(), properties);

        currentCompilation.set(null);
    }

    private static String formatSourceSection(SourceSection sourceSection) {
        if (sourceSection == null || sourceSection.getSource() == null) {
            return "n/a";
        }
        return String.format("%s:%d", sourceSection.getSource().getName(), sourceSection.getStartLine());
    }

    @Override
    public void onCompilationInvalidated(OptimizedCallTarget target, Object source, CharSequence reason) {
        Map<String, Object> properties = new LinkedHashMap<>();
        addSourceInfo(properties, source);
        properties.put("Reason", reason);
        runtime.logEvent(0, "opt invalidated", target.toString(), properties);
    }

    private static void addSourceInfo(Map<String, Object> properties, Object source) {
        if (source != null) {
            properties.put("SourceClass", source.getClass().getSimpleName());
            properties.put("Source", source);
        }
    }

    private static final class Times {
        final long timeCompilationStarted = System.nanoTime();
        long timePartialEvaluationFinished;
        long nodeCountPartialEval;
    }
}
