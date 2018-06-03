package giraaff.phases.common;

import giraaff.graph.Graph.NodeEventScope;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.tiers.PhaseContext;

///
// A phase suite that applies {@linkplain CanonicalizerPhase canonicalization} to a graph after all
// phases in the suite have been applied if any of the phases changed the graph.
///
// @class IncrementalCanonicalizerPhase
public final class IncrementalCanonicalizerPhase<C extends PhaseContext> extends PhaseSuite<C>
{
    // @field
    private final CanonicalizerPhase ___canonicalizer;

    // @cons
    public IncrementalCanonicalizerPhase(CanonicalizerPhase __canonicalizer)
    {
        super();
        this.___canonicalizer = __canonicalizer;
    }

    // @cons
    public IncrementalCanonicalizerPhase(CanonicalizerPhase __canonicalizer, BasePhase<? super C> __phase)
    {
        super();
        this.___canonicalizer = __canonicalizer;
        appendPhase(__phase);
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph __graph, C __context)
    {
        HashSetNodeEventListener __listener = new HashSetNodeEventListener();
        try (NodeEventScope __nes = __graph.trackNodeEvents(__listener))
        {
            super.run(__graph, __context);
        }

        if (!__listener.getNodes().isEmpty())
        {
            this.___canonicalizer.applyIncremental(__graph, __context, __listener.getNodes(), null);
        }
    }
}
