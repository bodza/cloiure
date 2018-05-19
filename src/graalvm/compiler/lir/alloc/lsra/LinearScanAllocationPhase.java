package graalvm.compiler.lir.alloc.lsra;

import graalvm.compiler.lir.gen.LIRGenerationResult;
import static graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.phases.LIRPhase;

import jdk.vm.ci.code.TargetDescription;

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

    public final void apply(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        run(target, lirGenRes, context);
    }

    protected abstract void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context);
}
