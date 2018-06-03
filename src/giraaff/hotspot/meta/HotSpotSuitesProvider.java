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
    // @field
    protected final HotSpotGraalRuntime runtime;

    // @field
    private final SuitesCreator defaultSuitesCreator;

    // @cons
    public HotSpotSuitesProvider(SuitesCreator __defaultSuitesCreator, HotSpotGraalRuntime __runtime)
    {
        super();
        this.defaultSuitesCreator = __defaultSuitesCreator;
        this.runtime = __runtime;
        this.defaultGraphBuilderSuite = createGraphBuilderSuite();
    }

    @Override
    public Suites createSuites()
    {
        Suites __ret = defaultSuitesCreator.createSuites();

        __ret.getMidTier().appendPhase(new WriteBarrierAdditionPhase());

        return __ret;
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
