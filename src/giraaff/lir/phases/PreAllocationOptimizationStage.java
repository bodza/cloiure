package giraaff.lir.phases;

import giraaff.lir.alloc.SaveCalleeSaveRegisters;
import giraaff.lir.constopt.ConstantLoadOptimization;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.options.OptionValues;

public class PreAllocationOptimizationStage extends LIRPhaseSuite<PreAllocationOptimizationContext>
{
    public PreAllocationOptimizationStage(OptionValues options)
    {
        if (ConstantLoadOptimization.Options.LIROptConstantLoadOptimization.getValue(options))
        {
            appendPhase(new ConstantLoadOptimization());
        }
        appendPhase(new SaveCalleeSaveRegisters());
    }
}
