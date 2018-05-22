package graalvm.compiler.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.lir.alloc.RegisterAllocationPhase;
import graalvm.compiler.lir.alloc.lsra.ssa.SSALinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;

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
