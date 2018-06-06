package giraaff.phases.common;

import jdk.vm.ci.code.BailoutException;

import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.spi.Simplifiable;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.tiers.PhaseContext;

// @class IterativeConditionalEliminationPhase
public final class IterativeConditionalEliminationPhase extends BasePhase<PhaseContext>
{
    // @def
    private static final int MAX_ITERATIONS = 256;

    // @field
    private final CanonicalizerPhase ___canonicalizer;
    // @field
    private final boolean ___fullSchedule;

    // @cons IterativeConditionalEliminationPhase
    public IterativeConditionalEliminationPhase(CanonicalizerPhase __canonicalizer, boolean __fullSchedule)
    {
        super();
        this.___canonicalizer = __canonicalizer;
        this.___fullSchedule = __fullSchedule;
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        HashSetNodeEventListener __listener = new HashSetNodeEventListener().exclude(Graph.NodeEvent.NODE_ADDED);
        int __count = 0;
        while (true)
        {
            try (Graph.NodeEventScope __nes = __graph.trackNodeEvents(__listener))
            {
                new ConditionalEliminationPhase(this.___fullSchedule).apply(__graph, __context);
            }
            if (__listener.getNodes().isEmpty())
            {
                break;
            }
            for (Node __node : __graph.getNodes())
            {
                if (__node instanceof Simplifiable)
                {
                    __listener.getNodes().add(__node);
                }
            }
            this.___canonicalizer.applyIncremental(__graph, __context, __listener.getNodes());
            __listener.getNodes().clear();
            __count++;
            if (__count > MAX_ITERATIONS)
            {
                throw new BailoutException(false, "number of iterations in ConditionalEliminationPhase phase exceeds %d", MAX_ITERATIONS);
            }
        }
    }
}
