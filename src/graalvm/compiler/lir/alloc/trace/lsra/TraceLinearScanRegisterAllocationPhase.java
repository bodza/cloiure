package graalvm.compiler.lir.alloc.trace.lsra;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;

final class TraceLinearScanRegisterAllocationPhase extends TraceLinearScanAllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        allocateRegisters(allocator);
    }

    private static void allocateRegisters(TraceLinearScan allocator)
    {
        FixedInterval precoloredIntervals = allocator.createFixedUnhandledList();
        TraceInterval notPrecoloredIntervals = allocator.createUnhandledListByFrom(TraceLinearScanPhase.IS_VARIABLE_INTERVAL);

        // allocate cpu registers
        TraceLinearScanWalker lsw = new TraceLinearScanWalker(allocator, precoloredIntervals, notPrecoloredIntervals);
        lsw.walk();
        lsw.finishAllocation();
    }
}
