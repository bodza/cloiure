package giraaff.java;

import giraaff.lir.phases.LIRSuites;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration;
import giraaff.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;

// @class DefaultSuitesCreator
public class DefaultSuitesCreator extends SuitesProviderBase
{
    private final CompilerConfiguration compilerConfiguration;

    // @cons
    public DefaultSuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins)
    {
        super();
        this.defaultGraphBuilderSuite = createGraphBuilderSuite(plugins);
        this.compilerConfiguration = compilerConfiguration;
    }

    @Override
    public Suites createSuites(OptionValues options)
    {
        return Suites.createSuites(compilerConfiguration, options);
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite(Plugins plugins)
    {
        PhaseSuite<HighTierContext> suite = new PhaseSuite<>();
        suite.appendPhase(new GraphBuilderPhase(GraphBuilderConfiguration.getDefault(plugins)));
        return suite;
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options)
    {
        return Suites.createLIRSuites(compilerConfiguration, options);
    }
}
