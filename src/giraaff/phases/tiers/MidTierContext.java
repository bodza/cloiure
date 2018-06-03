package giraaff.phases.tiers;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ProfilingInfo;

import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.util.Providers;

// @class MidTierContext
public final class MidTierContext extends PhaseContext
{
    // @field
    private final TargetProvider target;
    // @field
    private final OptimisticOptimizations optimisticOpts;
    // @field
    private final ProfilingInfo profilingInfo;

    // @cons
    public MidTierContext(Providers __copyFrom, TargetProvider __target, OptimisticOptimizations __optimisticOpts, ProfilingInfo __profilingInfo)
    {
        super(__copyFrom);
        this.target = __target;
        this.optimisticOpts = __optimisticOpts;
        this.profilingInfo = __profilingInfo;
    }

    public TargetDescription getTarget()
    {
        return target.getTarget();
    }

    public TargetProvider getTargetProvider()
    {
        return target;
    }

    public OptimisticOptimizations getOptimisticOptimizations()
    {
        return optimisticOpts;
    }

    public ProfilingInfo getProfilingInfo()
    {
        return profilingInfo;
    }
}
