package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.lir.alloc.RegisterAllocationPhase;
import giraaff.lir.alloc.lsra.ssa.SSALinearScan;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

// @class LinearScanPhase
public final class LinearScanPhase extends RegisterAllocationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationContext __context)
    {
        MoveFactory __spillMoveFactory = __context.spillMoveFactory;
        RegisterAllocationConfig __registerAllocationConfig = __context.registerAllocationConfig;
        final LinearScan __allocator = new SSALinearScan(__target, __lirGenRes, __spillMoveFactory, __registerAllocationConfig, __lirGenRes.getLIR().linearScanOrder(), getNeverSpillConstants());
        __allocator.allocate(__target, __lirGenRes, __context);
    }
}
