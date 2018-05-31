package giraaff.lir.phases;

import giraaff.core.common.GraalOptions;
import giraaff.lir.ControlFlowOptimizer;
import giraaff.lir.EdgeMoveOptimizer;
import giraaff.lir.NullCheckOptimizer;
import giraaff.lir.RedundantMoveElimination;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;

// @class PostAllocationOptimizationStage
public final class PostAllocationOptimizationStage extends LIRPhaseSuite<PostAllocationOptimizationContext>
{
    // @cons
    public PostAllocationOptimizationStage()
    {
        super();
        if (GraalOptions.lirOptEdgeMoveOptimizer)
        {
            appendPhase(new EdgeMoveOptimizer());
        }
        if (GraalOptions.lirOptControlFlowOptimizer)
        {
            appendPhase(new ControlFlowOptimizer());
        }
        if (GraalOptions.lirOptRedundantMoveElimination)
        {
            appendPhase(new RedundantMoveElimination());
        }
        if (GraalOptions.lirOptNullCheckOptimizer)
        {
            appendPhase(new NullCheckOptimizer());
        }
    }
}
