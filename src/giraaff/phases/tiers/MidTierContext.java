package giraaff.phases.tiers;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ProfilingInfo;

import giraaff.phases.OptimisticOptimizations;
import giraaff.phases.util.Providers;

public class MidTierContext extends PhaseContext
{
    private final TargetProvider target;
    private final OptimisticOptimizations optimisticOpts;
    private final ProfilingInfo profilingInfo;

    public MidTierContext(Providers copyFrom, TargetProvider target, OptimisticOptimizations optimisticOpts, ProfilingInfo profilingInfo)
    {
        super(copyFrom);
        this.target = target;
        this.optimisticOpts = optimisticOpts;
        this.profilingInfo = profilingInfo;
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
