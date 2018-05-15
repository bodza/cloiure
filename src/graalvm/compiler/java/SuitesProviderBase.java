package graalvm.compiler.java;

import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.Suites;
import graalvm.compiler.phases.tiers.SuitesCreator;

public abstract class SuitesProviderBase implements SuitesCreator {

    protected PhaseSuite<HighTierContext> defaultGraphBuilderSuite;

    @Override
    public final Suites getDefaultSuites(OptionValues options) {
        return createSuites(options);
    }

    @Override
    public PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite() {
        return defaultGraphBuilderSuite;
    }

    @Override
    public final LIRSuites getDefaultLIRSuites(OptionValues options) {
        return createLIRSuites(options);
    }

    @Override
    public abstract LIRSuites createLIRSuites(OptionValues options);

    @Override
    public abstract Suites createSuites(OptionValues options);
}
