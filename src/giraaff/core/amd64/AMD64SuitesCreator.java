package giraaff.core.amd64;

import giraaff.core.common.GraalOptions;
import giraaff.java.DefaultSuitesCreator;
import giraaff.lir.amd64.phases.StackMoveOptimizationPhase;
import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.phases.tiers.CompilerConfiguration;

// @class AMD64SuitesCreator
public class AMD64SuitesCreator extends DefaultSuitesCreator
{
    // @cons
    public AMD64SuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins)
    {
        super(compilerConfiguration, plugins);
    }

    @Override
    public LIRSuites createLIRSuites()
    {
        LIRSuites lirSuites = super.createLIRSuites();
        if (GraalOptions.lirOptStackMoveOptimizer)
        {
            // note: this phase must be inserted *after* RedundantMoveElimination
            lirSuites.getPostAllocationOptimizationStage().appendPhase(new StackMoveOptimizationPhase());
        }
        return lirSuites;
    }
}
