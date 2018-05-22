package giraaff.lir.alloc.trace.lsra;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.alloc.Trace;
import giraaff.core.common.alloc.TraceBuilderResult;
import giraaff.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

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
