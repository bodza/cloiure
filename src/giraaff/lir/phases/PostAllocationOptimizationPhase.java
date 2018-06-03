package giraaff.lir.phases;

import giraaff.lir.gen.DiagnosticLIRGeneratorTool;

// @class PostAllocationOptimizationPhase
public abstract class PostAllocationOptimizationPhase extends LIRPhase<PostAllocationOptimizationPhase.PostAllocationOptimizationContext>
{
    // @class PostAllocationOptimizationPhase.PostAllocationOptimizationContext
    public static final class PostAllocationOptimizationContext
    {
        // @field
        public final DiagnosticLIRGeneratorTool diagnosticLirGenTool;

        // @cons
        public PostAllocationOptimizationContext(DiagnosticLIRGeneratorTool __diagnosticTool)
        {
            super();
            this.diagnosticLirGenTool = __diagnosticTool;
        }
    }
}
