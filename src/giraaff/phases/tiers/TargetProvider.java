package giraaff.phases.tiers;

import jdk.vm.ci.code.TargetDescription;

// @iface TargetProvider
public interface TargetProvider
{
    TargetDescription getTarget();
}
