package giraaff.loop.phases;

import giraaff.loop.LoopPolicies;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.PhaseContext;

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
