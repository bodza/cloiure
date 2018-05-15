package graalvm.compiler.truffle.runtime.debug;

import static graalvm.compiler.core.GraalCompilerOptions.CompilationBailoutAction;

import java.util.LinkedHashMap;
import java.util.Map;

import graalvm.compiler.core.CompilationWrapper.ExceptionAction;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.truffle.common.TruffleCompilerOptions;
import graalvm.compiler.truffle.runtime.AbstractGraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntime;
import graalvm.compiler.truffle.runtime.GraalTruffleRuntimeListener;
import graalvm.compiler.truffle.runtime.OptimizedCallTarget;

/**
 * Traces Truffle compilation failures.
 */
public final class TraceCompilationFailureListener extends AbstractGraalTruffleRuntimeListener {

    private TraceCompilationFailureListener(GraalTruffleRuntime runtime) {
        super(runtime);
    }

    public static void install(GraalTruffleRuntime runtime) {
        runtime.addListener(new TraceCompilationFailureListener(runtime));
    }

    @Override
    public void onCompilationFailed(OptimizedCallTarget target, String reason, boolean bailout, boolean permanentBailout) {
        if (isPermanentFailure(bailout, permanentBailout) || bailoutActionIsPrintOrGreater()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("Reason", reason);
            runtime.logEvent(0, "opt fail", target.toString(), properties);
        }
    }

    private static boolean bailoutActionIsPrintOrGreater() {
        OptionValues options = TruffleCompilerOptions.getOptions();
        return CompilationBailoutAction.getValue(options).ordinal() >= ExceptionAction.Print.ordinal();
    }

    /**
     * Determines if a failure is permanent.
     *
     * @see GraalTruffleRuntimeListener#onCompilationFailed(OptimizedCallTarget, String, boolean,
     *      boolean)
     */
    public static boolean isPermanentFailure(boolean bailout, boolean permanentBailout) {
        return !bailout || permanentBailout;
    }

}
