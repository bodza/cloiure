package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.LIRPhase;

abstract class LinearScanAllocationPhase
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

    public final void apply(TargetDescription target, LIRGenerationResult lirGenRes, AllocationPhase.AllocationContext context)
    {
        run(target, lirGenRes, context);
    }

    protected abstract void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationPhase.AllocationContext context);
}
