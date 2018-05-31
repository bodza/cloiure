package giraaff.lir.phases;

import giraaff.core.common.GraalOptions;
import giraaff.lir.alloc.SaveCalleeSaveRegisters;
import giraaff.lir.constopt.ConstantLoadOptimization;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;

// @class PreAllocationOptimizationStage
public final class PreAllocationOptimizationStage extends LIRPhaseSuite<PreAllocationOptimizationContext>
{
    // @cons
    public PreAllocationOptimizationStage()
    {
        super();
        if (GraalOptions.lirOptConstantLoadOptimization)
        {
            appendPhase(new ConstantLoadOptimization());
        }
        appendPhase(new SaveCalleeSaveRegisters());
    }
}
