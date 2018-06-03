package giraaff.phases.tiers;

import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.PhaseSuite;
import giraaff.phases.util.Providers;

// @class HighTierContext
public final class HighTierContext extends PhaseContext
{
    // @field
    private final PhaseSuite<HighTierContext> ___graphBuilderSuite;

    // @field
    private final OptimisticOptimizations ___optimisticOpts;

    // @cons
    public HighTierContext(Providers __providers, PhaseSuite<HighTierContext> __graphBuilderSuite, OptimisticOptimizations __optimisticOpts)
    {
        super(__providers);
        this.___graphBuilderSuite = __graphBuilderSuite;
        this.___optimisticOpts = __optimisticOpts;
    }

    public PhaseSuite<HighTierContext> getGraphBuilderSuite()
    {
        return this.___graphBuilderSuite;
    }

    public OptimisticOptimizations getOptimisticOptimizations()
    {
        return this.___optimisticOpts;
    }
}
