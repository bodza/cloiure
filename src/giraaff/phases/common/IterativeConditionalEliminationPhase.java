package giraaff.phases.common;

import jdk.vm.ci.code.BailoutException;

import giraaff.graph.Graph.NodeEvent;
import giraaff.graph.Graph.NodeEventScope;
import giraaff.graph.Node;
import giraaff.graph.spi.Simplifiable;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.tiers.PhaseContext;

// @class IterativeConditionalEliminationPhase
public final class IterativeConditionalEliminationPhase extends BasePhase<PhaseContext>
{
    private static final int MAX_ITERATIONS = 256;

    private final CanonicalizerPhase canonicalizer;
    private final boolean fullSchedule;

    // @cons
    public IterativeConditionalEliminationPhase(CanonicalizerPhase canonicalizer, boolean fullSchedule)
    {
        super();
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
                throw new BailoutException(false, "number of iterations in ConditionalEliminationPhase phase exceeds %d", MAX_ITERATIONS);
            }
        }
    }
}
