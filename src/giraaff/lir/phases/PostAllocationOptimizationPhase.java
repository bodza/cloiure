package giraaff.lir.phases;

import giraaff.lir.gen.DiagnosticLIRGeneratorTool;

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
