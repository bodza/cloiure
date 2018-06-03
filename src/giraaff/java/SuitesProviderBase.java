package giraaff.java;

import giraaff.lir.phases.LIRSuites;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.SuitesCreator;

// @class SuitesProviderBase
public abstract class SuitesProviderBase implements SuitesCreator
{
    // @field
    protected PhaseSuite<HighTierContext> defaultGraphBuilderSuite;

    @Override
    public final Suites getDefaultSuites()
    {
        return createSuites();
    }

    @Override
    public PhaseSuite<HighTierContext> getDefaultGraphBuilderSuite()
    {
        return defaultGraphBuilderSuite;
    }

    @Override
    public final LIRSuites getDefaultLIRSuites()
    {
        return createLIRSuites();
    }

    @Override
    public abstract LIRSuites createLIRSuites();

    @Override
    public abstract Suites createSuites();
}
