package giraaff.java;

import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;

// @class DefaultSuitesCreator
public class DefaultSuitesCreator extends SuitesProviderBase
{
    // @field
    private final CompilerConfiguration ___compilerConfiguration;

    // @cons DefaultSuitesCreator
    public DefaultSuitesCreator(CompilerConfiguration __compilerConfiguration, GraphBuilderConfiguration.Plugins __plugins)
    {
        super();
        this.___defaultGraphBuilderSuite = createGraphBuilderSuite(__plugins);
        this.___compilerConfiguration = __compilerConfiguration;
    }

    @Override
    public Suites createSuites()
    {
        return Suites.createSuites(this.___compilerConfiguration);
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite(GraphBuilderConfiguration.Plugins __plugins)
    {
        PhaseSuite<HighTierContext> __suite = new PhaseSuite<>();
        __suite.appendPhase(new GraphBuilderPhase(GraphBuilderConfiguration.getDefault(__plugins)));
        return __suite;
    }

    @Override
    public LIRSuites createLIRSuites()
    {
        return Suites.createLIRSuites(this.___compilerConfiguration);
    }
}
