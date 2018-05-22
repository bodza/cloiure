package graalvm.compiler.phases.tiers;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.phases.util.Providers;

public class LowTierContext extends PhaseContext
{
    private final TargetProvider target;

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
