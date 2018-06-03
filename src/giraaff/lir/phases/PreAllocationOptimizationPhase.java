package giraaff.lir.phases;

import giraaff.lir.gen.LIRGeneratorTool;

// @class PreAllocationOptimizationPhase
public abstract class PreAllocationOptimizationPhase extends LIRPhase<PreAllocationOptimizationPhase.PreAllocationOptimizationContext>
{
    // @class PreAllocationOptimizationPhase.PreAllocationOptimizationContext
    public static final class PreAllocationOptimizationContext
    {
        // @field
        public final LIRGeneratorTool ___lirGen;

        // @cons
        public PreAllocationOptimizationContext(LIRGeneratorTool __lirGen)
        {
            super();
            this.___lirGen = __lirGen;
        }
    }
}
