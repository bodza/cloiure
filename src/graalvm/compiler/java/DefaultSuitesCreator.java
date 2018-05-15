package graalvm.compiler.java;

import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.Suites;

public class DefaultSuitesCreator extends SuitesProviderBase {

    private final CompilerConfiguration compilerConfiguration;

    public DefaultSuitesCreator(CompilerConfiguration compilerConfiguration, Plugins plugins) {
        super();
        this.defaultGraphBuilderSuite = createGraphBuilderSuite(plugins);
        this.compilerConfiguration = compilerConfiguration;
    }

    @Override
    public Suites createSuites(OptionValues options) {
        return Suites.createSuites(compilerConfiguration, options);
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite(Plugins plugins) {
        PhaseSuite<HighTierContext> suite = new PhaseSuite<>();
        suite.appendPhase(new GraphBuilderPhase(GraphBuilderConfiguration.getDefault(plugins)));
        return suite;
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options) {
        return Suites.createLIRSuites(compilerConfiguration, options);
    }
}
