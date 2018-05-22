package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.lir.alloc.RegisterAllocationPhase;
import giraaff.lir.alloc.lsra.ssa.SSALinearScan;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

public final class LinearScanPhase extends RegisterAllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        MoveFactory spillMoveFactory = context.spillMoveFactory;
        RegisterAllocationConfig registerAllocationConfig = context.registerAllocationConfig;
        final LinearScan allocator = new SSALinearScan(target, lirGenRes, spillMoveFactory, registerAllocationConfig, lirGenRes.getLIR().linearScanOrder(), getNeverSpillConstants());
        allocator.allocate(target, lirGenRes, context);
    }
}
