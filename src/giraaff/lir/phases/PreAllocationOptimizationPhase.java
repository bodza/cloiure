package giraaff.lir.phases;

import giraaff.lir.gen.LIRGeneratorTool;

// @class PreAllocationOptimizationPhase
public abstract class PreAllocationOptimizationPhase extends LIRPhase<PreAllocationOptimizationPhase.PreAllocationOptimizationContext>
{
    // @class PreAllocationOptimizationPhase.PreAllocationOptimizationContext
    public static final class PreAllocationOptimizationContext
    {
        public final LIRGeneratorTool lirGen;

        // @cons
        public PreAllocationOptimizationContext(LIRGeneratorTool lirGen)
        {
            super();
            this.lirGen = lirGen;
        }
    }
}
