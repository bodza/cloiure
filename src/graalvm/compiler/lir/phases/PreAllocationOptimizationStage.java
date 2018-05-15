package graalvm.compiler.lir.phases;

import graalvm.compiler.lir.alloc.SaveCalleeSaveRegisters;
import graalvm.compiler.lir.constopt.ConstantLoadOptimization;
import graalvm.compiler.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import graalvm.compiler.options.OptionValues;

public class PreAllocationOptimizationStage extends LIRPhaseSuite<PreAllocationOptimizationContext> {
    public PreAllocationOptimizationStage(OptionValues options) {
        if (ConstantLoadOptimization.Options.LIROptConstantLoadOptimization.getValue(options)) {
            appendPhase(new ConstantLoadOptimization());
        }
        appendPhase(new SaveCalleeSaveRegisters());
    }
}
