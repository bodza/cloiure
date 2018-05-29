package giraaff.lir.phases;

import giraaff.lir.alloc.SaveCalleeSaveRegisters;
import giraaff.lir.constopt.ConstantLoadOptimization;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.options.OptionValues;

// @class PreAllocationOptimizationStage
public final class PreAllocationOptimizationStage extends LIRPhaseSuite<PreAllocationOptimizationContext>
{
    // @cons
    public PreAllocationOptimizationStage(OptionValues options)
    {
        super();
        if (ConstantLoadOptimization.Options.LIROptConstantLoadOptimization.getValue(options))
        {
            appendPhase(new ConstantLoadOptimization());
        }
        appendPhase(new SaveCalleeSaveRegisters());
    }
}
