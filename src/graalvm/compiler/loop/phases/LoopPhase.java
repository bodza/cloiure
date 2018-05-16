package graalvm.compiler.loop.phases;

import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.tiers.PhaseContext;

public abstract class LoopPhase<P extends LoopPolicies> extends BasePhase<PhaseContext>
{
    private P policies;

    public LoopPhase(P policies)
    {
        this.policies = policies;
    }

    protected P getPolicies()
    {
        return policies;
    }
}
