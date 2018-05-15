package graalvm.compiler.lir.alloc.trace.lsra;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.phases.LIRPhase;

import jdk.vm.ci.code.TargetDescription;

abstract class TraceLinearScanAllocationPhase {

    final CharSequence getName() {
        return LIRPhase.createName(getClass());
    }

    @Override
    public final String toString() {
        return getName().toString();
    }

    final void apply(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig,
                    TraceBuilderResult traceBuilderResult, TraceLinearScan allocator) {
        apply(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, allocator, true);
    }

    final void apply(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig,
                    TraceBuilderResult traceBuilderResult, TraceLinearScan allocator, boolean dumpLIR) {
        run(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, allocator);
        if (dumpLIR) {
            DebugContext debug = lirGenRes.getLIR().getDebug();
            if (debug.isDumpEnabled(DebugContext.DETAILED_LEVEL)) {
                debug.dump(DebugContext.DETAILED_LEVEL, trace, "After %s (Trace%s)", getName(), trace.getId());
            }
        }
    }

    abstract void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig,
                    TraceBuilderResult traceBuilderResult, TraceLinearScan allocator);

}
