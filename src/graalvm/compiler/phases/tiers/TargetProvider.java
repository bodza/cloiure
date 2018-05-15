package graalvm.compiler.phases.tiers;

import jdk.vm.ci.code.TargetDescription;

public interface TargetProvider {

    TargetDescription getTarget();
}
