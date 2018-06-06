package giraaff.core.amd64;

import giraaff.core.common.GraalOptions;
import giraaff.java.DefaultSuitesCreator;
import giraaff.lir.amd64.phases.StackMoveOptimizationPhase;
import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.phases.tiers.CompilerConfiguration;

// @class AMD64SuitesCreator
public class AMD64SuitesCreator extends DefaultSuitesCreator
{
    // @cons AMD64SuitesCreator
    public AMD64SuitesCreator(CompilerConfiguration __compilerConfiguration, GraphBuilderConfiguration.Plugins __plugins)
    {
        super(__compilerConfiguration, __plugins);
    }

    @Override
    public LIRSuites createLIRSuites()
    {
        LIRSuites __lirSuites = super.createLIRSuites();
        if (GraalOptions.lirOptStackMoveOptimizer)
        {
            // note: this phase must be inserted *after* RedundantMoveElimination
            __lirSuites.getPostAllocationOptimizationStage().appendPhase(new StackMoveOptimizationPhase());
        }
        return __lirSuites;
    }
}
