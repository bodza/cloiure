package giraaff.phases.tiers;

import jdk.vm.ci.code.TargetDescription;

import giraaff.phases.util.Providers;

// @class LowTierContext
public final class LowTierContext extends PhaseContext
{
    private final TargetProvider target;

    // @cons
    public LowTierContext(Providers copyFrom, TargetProvider target)
    {
        super(copyFrom);
        this.target = target;
    }

    public TargetDescription getTarget()
    {
        return target.getTarget();
    }

    public TargetProvider getTargetProvider()
    {
        return target;
    }
}
