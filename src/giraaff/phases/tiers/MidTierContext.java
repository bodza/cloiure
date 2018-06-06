package giraaff.phases.tiers;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ProfilingInfo;

import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.util.Providers;

// @class MidTierContext
public final class MidTierContext extends PhaseContext
{
    // @field
    private final TargetProvider ___target;
    // @field
    private final OptimisticOptimizations ___optimisticOpts;
    // @field
    private final ProfilingInfo ___profilingInfo;

    // @cons MidTierContext
    public MidTierContext(Providers __copyFrom, TargetProvider __target, OptimisticOptimizations __optimisticOpts, ProfilingInfo __profilingInfo)
    {
        super(__copyFrom);
        this.___target = __target;
        this.___optimisticOpts = __optimisticOpts;
        this.___profilingInfo = __profilingInfo;
    }

    public TargetDescription getTarget()
    {
        return this.___target.getTarget();
    }

    public TargetProvider getTargetProvider()
    {
        return this.___target;
    }

    public OptimisticOptimizations getOptimisticOptimizations()
    {
        return this.___optimisticOpts;
    }

    public ProfilingInfo getProfilingInfo()
    {
        return this.___profilingInfo;
    }
}
