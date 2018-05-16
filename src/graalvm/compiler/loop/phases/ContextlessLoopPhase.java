package graalvm.compiler.loop.phases;

import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.tiers.PhaseContext;

public abstract class ContextlessLoopPhase<P extends LoopPolicies> extends LoopPhase<P>
{
    public ContextlessLoopPhase(P policies)
    {
        super(policies);
    }

    public final void apply(final StructuredGraph graph)
    {
        apply(graph, true);
    }

    public final void apply(final StructuredGraph graph, final boolean dumpGraph)
    {
        apply(graph, null, dumpGraph);
    }

    protected abstract void run(StructuredGraph graph);

    @Override
    protected final void run(StructuredGraph graph, PhaseContext context)
    {
        run(graph);
    }
}
