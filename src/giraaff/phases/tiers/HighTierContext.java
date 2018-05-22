package giraaff.phases.tiers;

import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.PhaseSuite;
import giraaff.phases.util.Providers;

public class HighTierContext extends PhaseContext
{
    private final PhaseSuite<HighTierContext> graphBuilderSuite;

    private final OptimisticOptimizations optimisticOpts;

    public HighTierContext(Providers providers, PhaseSuite<HighTierContext> graphBuilderSuite, OptimisticOptimizations optimisticOpts)
    {
        super(providers);
        this.graphBuilderSuite = graphBuilderSuite;
        this.optimisticOpts = optimisticOpts;
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
