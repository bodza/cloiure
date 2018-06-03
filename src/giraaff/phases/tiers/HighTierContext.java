package giraaff.phases.tiers;

import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.PhaseSuite;
import giraaff.phases.util.Providers;

// @class HighTierContext
public final class HighTierContext extends PhaseContext
{
    // @field
    private final PhaseSuite<HighTierContext> graphBuilderSuite;

    // @field
    private final OptimisticOptimizations optimisticOpts;

    // @cons
    public HighTierContext(Providers __providers, PhaseSuite<HighTierContext> __graphBuilderSuite, OptimisticOptimizations __optimisticOpts)
    {
        super(__providers);
        this.graphBuilderSuite = __graphBuilderSuite;
        this.optimisticOpts = __optimisticOpts;
    }

    public PhaseSuite<HighTierContext> getGraphBuilderSuite()
    {
        return graphBuilderSuite;
    }

    public OptimisticOptimizations getOptimisticOptimizations()
    {
        return optimisticOpts;
    }
}
