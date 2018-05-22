package graalvm.compiler.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase;
import graalvm.compiler.lir.phases.LIRPhase;

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
