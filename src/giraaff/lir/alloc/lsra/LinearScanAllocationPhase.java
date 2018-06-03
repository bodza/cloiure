package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.LIRPhase;

// @class LinearScanAllocationPhase
abstract class LinearScanAllocationPhase
{
    final CharSequence getName()
    {
        return LIRPhase.createName(getClass());
    }

    public final void apply(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationPhase.AllocationContext __context)
    {
        run(__target, __lirGenRes, __context);
    }

    protected abstract void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationPhase.AllocationContext __context);
}
