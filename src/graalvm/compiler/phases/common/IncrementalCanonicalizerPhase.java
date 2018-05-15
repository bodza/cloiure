package graalvm.compiler.phases.common;

import graalvm.compiler.graph.Graph.NodeEventScope;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.util.HashSetNodeEventListener;
import graalvm.compiler.phases.tiers.PhaseContext;

/**
 * A phase suite that applies {@linkplain CanonicalizerPhase canonicalization} to a graph after all
 * phases in the suite have been applied if any of the phases changed the graph.
 */
public class IncrementalCanonicalizerPhase<C extends PhaseContext> extends PhaseSuite<C> {

    private final CanonicalizerPhase canonicalizer;

    public IncrementalCanonicalizerPhase(CanonicalizerPhase canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    public IncrementalCanonicalizerPhase(CanonicalizerPhase canonicalizer, BasePhase<? super C> phase) {
        this.canonicalizer = canonicalizer;
        appendPhase(phase);
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, C context) {
        HashSetNodeEventListener listener = new HashSetNodeEventListener();
        try (NodeEventScope nes = graph.trackNodeEvents(listener)) {
            super.run(graph, context);
        }

        if (!listener.getNodes().isEmpty()) {
            canonicalizer.applyIncremental(graph, context, listener.getNodes(), null, false);
        }
    }
}
