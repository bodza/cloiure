package giraaff.hotspot.meta;

import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.phases.WriteBarrierAdditionPhase;
import giraaff.java.SuitesProviderBase;
import giraaff.lir.phases.LIRSuites;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.SuitesCreator;

/**
 * HotSpot implementation of {@link SuitesCreator}.
 */
// @class HotSpotSuitesProvider
public class HotSpotSuitesProvider extends SuitesProviderBase
{
    protected final HotSpotGraalRuntime runtime;

    private final SuitesCreator defaultSuitesCreator;

    // @cons
    public HotSpotSuitesProvider(SuitesCreator defaultSuitesCreator, HotSpotGraalRuntime runtime)
    {
        super();
        this.defaultSuitesCreator = defaultSuitesCreator;
        this.runtime = runtime;
        this.defaultGraphBuilderSuite = createGraphBuilderSuite();
    }

    @Override
    public Suites createSuites()
    {
        Suites ret = defaultSuitesCreator.createSuites();

        ret.getMidTier().appendPhase(new WriteBarrierAdditionPhase());

        return ret;
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite()
    {
        return defaultSuitesCreator.getDefaultGraphBuilderSuite().copy();
    }

    @Override
    public LIRSuites createLIRSuites()
    {
        return defaultSuitesCreator.createLIRSuites();
    }
}
