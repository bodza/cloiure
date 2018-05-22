package giraaff.lir.alloc.trace;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.alloc.Trace;
import giraaff.core.common.alloc.TraceBuilderResult;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;
import giraaff.lir.phases.LIRPhase;

public abstract class TraceAllocationPhase<C extends TraceAllocationPhase.TraceAllocationContext>
{
    public static class TraceAllocationContext
    {
        public final MoveFactory spillMoveFactory;
        public final RegisterAllocationConfig registerAllocationConfig;
        public final TraceBuilderResult resultTraces;
        public final GlobalLivenessInfo livenessInfo;

        public TraceAllocationContext(MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult resultTraces, GlobalLivenessInfo livenessInfo)
        {
            this.spillMoveFactory = spillMoveFactory;
            this.registerAllocationConfig = registerAllocationConfig;
            this.resultTraces = resultTraces;
            this.livenessInfo = livenessInfo;
        }
    }

    public TraceAllocationPhase()
    {
    }

    public final CharSequence getName()
    {
        return LIRPhase.createName(getClass());
    }

    @Override
    public final String toString()
    {
        return getName().toString();
    }

    public final void apply(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, C context)
    {
        run(target, lirGenRes, trace, context);
    }

    protected abstract void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, C context);
}
