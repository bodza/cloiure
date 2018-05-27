package giraaff.core.amd64;

import giraaff.java.DefaultSuitesCreator;
import giraaff.lir.amd64.phases.StackMoveOptimizationPhase;
import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.options.OptionValues;
import giraaff.phases.tiers.CompilerConfiguration;

public class AMD64SuitesCreator extends DefaultSuitesCreator
{
    public AMD64SuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins)
    {
        super(compilerConfiguration, plugins);
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options)
    {
        LIRSuites lirSuites = super.createLIRSuites(options);
        if (StackMoveOptimizationPhase.Options.LIROptStackMoveOptimizer.getValue(options))
        {
            // note: this phase must be inserted *after* RedundantMoveElimination
            lirSuites.getPostAllocationOptimizationStage().appendPhase(new StackMoveOptimizationPhase());
        }
        return lirSuites;
    }
}
