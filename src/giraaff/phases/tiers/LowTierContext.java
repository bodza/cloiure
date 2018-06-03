package giraaff.phases.tiers;

import jdk.vm.ci.code.TargetDescription;

import giraaff.phases.util.Providers;

// @class LowTierContext
public final class LowTierContext extends PhaseContext
{
    // @field
    private final TargetProvider target;

    // @cons
    public LowTierContext(Providers __copyFrom, TargetProvider __target)
    {
        super(__copyFrom);
        this.target = __target;
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
