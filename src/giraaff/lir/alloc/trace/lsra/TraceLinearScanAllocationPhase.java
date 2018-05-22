package giraaff.lir.alloc.trace.lsra;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.alloc.Trace;
import giraaff.core.common.alloc.TraceBuilderResult;
import giraaff.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;
import giraaff.lir.phases.LIRPhase;

abstract class TraceLinearScanAllocationPhase
{
    final CharSequence getName()
    {
        return LIRPhase.createName(getClass());
    }

    @Override
    public final String toString()
    {
        return getName().toString();
    }

    final void apply(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        run(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, allocator);
    }

    abstract void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator);
}
