package graalvm.compiler.lir.phases;

import graalvm.compiler.lir.gen.LIRGeneratorTool;

public abstract class PreAllocationOptimizationPhase extends LIRPhase<PreAllocationOptimizationPhase.PreAllocationOptimizationContext> {

    public static final class PreAllocationOptimizationContext {
        public final LIRGeneratorTool lirGen;

        public PreAllocationOptimizationContext(LIRGeneratorTool lirGen) {
            this.lirGen = lirGen;
        }

    }

}
