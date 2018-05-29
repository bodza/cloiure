package giraaff.java;

import giraaff.lir.phases.LIRSuites;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.SuitesCreator;

// @class SuitesProviderBase
public abstract class SuitesProviderBase implements SuitesCreator
{
    protected PhaseSuite<HighTierContext> defaultGraphBuilderSuite;

    @Override
    public final Suites getDefaultSuites(OptionValues options)
    {
        return createSuites(options);
    }

    @Override
    public PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite()
    {
        return defaultGraphBuilderSuite;
    }

    @Override
    public final LIRSuites getDefaultLIRSuites(OptionValues options)
    {
        return createLIRSuites(options);
    }

    @Override
    public abstract LIRSuites createLIRSuites(OptionValues options);

    @Override
    public abstract Suites createSuites(OptionValues options);
}
