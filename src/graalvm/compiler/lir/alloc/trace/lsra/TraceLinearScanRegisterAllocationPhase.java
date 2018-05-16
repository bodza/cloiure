package graalvm.compiler.lir.alloc.trace.lsra;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;

import jdk.vm.ci.code.TargetDescription;

final class TraceLinearScanRegisterAllocationPhase extends TraceLinearScanAllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        allocateRegisters(allocator);
    }

    @SuppressWarnings("try")
    private static void allocateRegisters(TraceLinearScan allocator)
    {
        DebugContext debug = allocator.getDebug();
        try (Indent indent = debug.logAndIndent("allocate registers"))
        {
            FixedInterval precoloredIntervals = allocator.createFixedUnhandledList();
            TraceInterval notPrecoloredIntervals = allocator.createUnhandledListByFrom(TraceLinearScanPhase.IS_VARIABLE_INTERVAL);

            // allocate cpu registers
            TraceLinearScanWalker lsw = new TraceLinearScanWalker(allocator, precoloredIntervals, notPrecoloredIntervals);
            lsw.walk();
            lsw.finishAllocation();
        }
    }
}
