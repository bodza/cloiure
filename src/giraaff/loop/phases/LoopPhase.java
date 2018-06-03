package giraaff.loop.phases;

import giraaff.loop.LoopPolicies;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.PhaseContext;

// @class LoopPhase
public abstract class LoopPhase<P extends LoopPolicies> extends BasePhase<PhaseContext>
{
    // @field
    private P ___policies;

    // @cons
    public LoopPhase(P __policies)
    {
        super();
        this.___policies = __policies;
    }

    protected P getPolicies()
    {
        return this.___policies;
    }
}
