package giraaff.lir.phases;

import giraaff.lir.ControlFlowOptimizer;
import giraaff.lir.EdgeMoveOptimizer;
import giraaff.lir.NullCheckOptimizer;
import giraaff.lir.RedundantMoveElimination;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.options.NestedBooleanOptionKey;
import giraaff.options.OptionValues;

public class PostAllocationOptimizationStage extends LIRPhaseSuite<PostAllocationOptimizationContext>
{
    public static class Options
    {
        public static final NestedBooleanOptionKey LIROptEdgeMoveOptimizer = new NestedBooleanOptionKey(LIRPhase.Options.LIROptimization, true);
        public static final NestedBooleanOptionKey LIROptControlFlowOptimizer = new NestedBooleanOptionKey(LIRPhase.Options.LIROptimization, true);
        public static final NestedBooleanOptionKey LIROptRedundantMoveElimination = new NestedBooleanOptionKey(LIRPhase.Options.LIROptimization, true);
        public static final NestedBooleanOptionKey LIROptNullCheckOptimizer = new NestedBooleanOptionKey(LIRPhase.Options.LIROptimization, true);
    }

    public PostAllocationOptimizationStage(OptionValues options)
    {
        if (Options.LIROptEdgeMoveOptimizer.getValue(options))
        {
            appendPhase(new EdgeMoveOptimizer());
        }
        if (Options.LIROptControlFlowOptimizer.getValue(options))
        {
            appendPhase(new ControlFlowOptimizer());
        }
        if (Options.LIROptRedundantMoveElimination.getValue(options))
        {
            appendPhase(new RedundantMoveElimination());
        }
        if (Options.LIROptNullCheckOptimizer.getValue(options))
        {
            appendPhase(new NullCheckOptimizer());
        }
    }
}
