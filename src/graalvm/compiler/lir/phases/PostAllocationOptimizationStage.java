package graalvm.compiler.lir.phases;

import static graalvm.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import graalvm.compiler.lir.ControlFlowOptimizer;
import graalvm.compiler.lir.EdgeMoveOptimizer;
import graalvm.compiler.lir.NullCheckOptimizer;
import graalvm.compiler.lir.RedundantMoveElimination;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import graalvm.compiler.options.NestedBooleanOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;

public class PostAllocationOptimizationStage extends LIRPhaseSuite<PostAllocationOptimizationContext>
{
    public static class Options
    {
        @Option(help = "", type = OptionType.Debug)
        public static final NestedBooleanOptionKey LIROptEdgeMoveOptimizer = new NestedBooleanOptionKey(LIROptimization, true);
        @Option(help = "", type = OptionType.Debug)
        public static final NestedBooleanOptionKey LIROptControlFlowOptimizer = new NestedBooleanOptionKey(LIROptimization, true);
        @Option(help = "", type = OptionType.Debug)
        public static final NestedBooleanOptionKey LIROptRedundantMoveElimination = new NestedBooleanOptionKey(LIROptimization, true);
        @Option(help = "", type = OptionType.Debug)
        public static final NestedBooleanOptionKey LIROptNullCheckOptimizer = new NestedBooleanOptionKey(LIROptimization, true);
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
