package graalvm.compiler.phases.common;

import graalvm.compiler.core.common.RetryableBailoutException;
import graalvm.compiler.graph.Graph.NodeEvent;
import graalvm.compiler.graph.Graph.NodeEventScope;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.common.util.HashSetNodeEventListener;
import graalvm.compiler.phases.tiers.PhaseContext;

public class IterativeConditionalEliminationPhase extends BasePhase<PhaseContext>
{
    private static final int MAX_ITERATIONS = 256;

    private final CanonicalizerPhase canonicalizer;
    private final boolean fullSchedule;

    public IterativeConditionalEliminationPhase(CanonicalizerPhase canonicalizer, boolean fullSchedule)
    {
        this.canonicalizer = canonicalizer;
        this.fullSchedule = fullSchedule;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        HashSetNodeEventListener listener = new HashSetNodeEventListener().exclude(NodeEvent.NODE_ADDED);
        int count = 0;
        while (true)
        {
            try (NodeEventScope nes = graph.trackNodeEvents(listener))
            {
                new ConditionalEliminationPhase(fullSchedule).apply(graph, context);
            }
            if (listener.getNodes().isEmpty())
            {
                break;
            }
            for (Node node : graph.getNodes())
            {
                if (node instanceof Simplifiable)
                {
                    listener.getNodes().add(node);
                }
            }
            canonicalizer.applyIncremental(graph, context, listener.getNodes());
            listener.getNodes().clear();
            if (++count > MAX_ITERATIONS)
            {
                throw new RetryableBailoutException("Number of iterations in ConditionalEliminationPhase phase exceeds %d", MAX_ITERATIONS);
            }
        }
    }
}
