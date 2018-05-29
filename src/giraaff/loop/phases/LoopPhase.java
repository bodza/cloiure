package giraaff.loop.phases;

import giraaff.loop.LoopPolicies;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.PhaseContext;

// @class LoopPhase
public abstract class LoopPhase<P extends LoopPolicies> extends BasePhase<PhaseContext>
{
    private P policies;

    // @cons
    public LoopPhase(P policies)
    {
        super();
        this.policies = policies;
    }

    protected P getPolicies()
    {
        return policies;
    }
}
