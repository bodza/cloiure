package giraaff.phases.common;

import giraaff.graph.Graph.NodeEventScope;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.tiers.PhaseContext;

/**
 * A phase suite that applies {@linkplain CanonicalizerPhase canonicalization} to a graph after all
 * phases in the suite have been applied if any of the phases changed the graph.
 */
// @class IncrementalCanonicalizerPhase
public final class IncrementalCanonicalizerPhase<C extends PhaseContext> extends PhaseSuite<C>
{
    private final CanonicalizerPhase canonicalizer;

    // @cons
    public IncrementalCanonicalizerPhase(CanonicalizerPhase canonicalizer)
    {
        super();
        this.canonicalizer = canonicalizer;
    }

    // @cons
    public IncrementalCanonicalizerPhase(CanonicalizerPhase canonicalizer, BasePhase<? super C> phase)
    {
        super();
        this.canonicalizer = canonicalizer;
        appendPhase(phase);
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, C context)
    {
        HashSetNodeEventListener listener = new HashSetNodeEventListener();
        try (NodeEventScope nes = graph.trackNodeEvents(listener))
        {
            super.run(graph, context);
        }

        if (!listener.getNodes().isEmpty())
        {
            canonicalizer.applyIncremental(graph, context, listener.getNodes(), null);
        }
    }
}
