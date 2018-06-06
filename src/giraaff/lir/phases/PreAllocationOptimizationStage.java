package giraaff.lir.phases;

import giraaff.core.common.GraalOptions;
import giraaff.lir.alloc.SaveCalleeSaveRegisters;
import giraaff.lir.constopt.ConstantLoadOptimization;
import giraaff.lir.phases.PreAllocationOptimizationPhase;

// @class PreAllocationOptimizationStage
public final class PreAllocationOptimizationStage extends LIRPhaseSuite<PreAllocationOptimizationPhase.PreAllocationOptimizationContext>
{
    // @cons PreAllocationOptimizationStage
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
