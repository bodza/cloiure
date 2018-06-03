package giraaff.loop.phases;

import giraaff.loop.LoopPolicies;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.tiers.PhaseContext;

// @class ContextlessLoopPhase
public abstract class ContextlessLoopPhase<P extends LoopPolicies> extends LoopPhase<P>
{
    // @cons
    public ContextlessLoopPhase(P __policies)
    {
        super(__policies);
    }

    public final void apply(final StructuredGraph __graph)
    {
        apply(__graph, null);
    }

    protected abstract void run(StructuredGraph graph);

    @Override
    protected final void run(StructuredGraph __graph, PhaseContext __context)
    {
        run(__graph);
    }
}
