package giraaff.hotspot.meta;

import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.phases.WriteBarrierAdditionPhase;
import giraaff.java.SuitesProviderBase;
import giraaff.lir.phases.LIRSuites;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.SuitesCreator;

/**
 * HotSpot implementation of {@link SuitesCreator}.
 */
public class HotSpotSuitesProvider extends SuitesProviderBase
{
    protected final HotSpotGraalRuntimeProvider runtime;

    private final SuitesCreator defaultSuitesCreator;

    public HotSpotSuitesProvider(SuitesCreator defaultSuitesCreator, HotSpotGraalRuntimeProvider runtime)
    {
        this.defaultSuitesCreator = defaultSuitesCreator;
        this.runtime = runtime;
        this.defaultGraphBuilderSuite = createGraphBuilderSuite();
    }

    @Override
    public Suites createSuites(OptionValues options)
    {
        Suites ret = defaultSuitesCreator.createSuites(options);

        ret.getMidTier().appendPhase(new WriteBarrierAdditionPhase());

        return ret;
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite()
    {
        return defaultSuitesCreator.getDefaultGraphBuilderSuite().copy();
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options)
    {
        return defaultSuitesCreator.createLIRSuites(options);
    }
}
