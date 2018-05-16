package graalvm.compiler.lir.phases;

import graalvm.compiler.lir.gen.DiagnosticLIRGeneratorTool;

public abstract class PostAllocationOptimizationPhase extends LIRPhase<PostAllocationOptimizationPhase.PostAllocationOptimizationContext>
{
    public static final class PostAllocationOptimizationContext
    {
        public final DiagnosticLIRGeneratorTool diagnosticLirGenTool;

        public PostAllocationOptimizationContext(DiagnosticLIRGeneratorTool diagnosticTool)
        {
            this.diagnosticLirGenTool = diagnosticTool;
        }
    }
}
