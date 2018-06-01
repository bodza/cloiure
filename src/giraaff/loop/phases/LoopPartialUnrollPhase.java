package giraaff.loop.phases;

import giraaff.graph.Graph;
import giraaff.loop.LoopEx;
import giraaff.loop.LoopPolicies;
import giraaff.loop.LoopsData;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.tiers.PhaseContext;

// @class LoopPartialUnrollPhase
public final class LoopPartialUnrollPhase extends LoopPhase<LoopPolicies>
{
    private final CanonicalizerPhase canonicalizer;

    // @cons
    public LoopPartialUnrollPhase(LoopPolicies policies, CanonicalizerPhase canonicalizer)
    {
        super(policies);
        this.canonicalizer = canonicalizer;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        if (graph.hasLoops())
        {
            HashSetNodeEventListener listener = new HashSetNodeEventListener();
            boolean changed = true;
            while (changed)
            {
                changed = false;
                try (Graph.NodeEventScope nes = graph.trackNodeEvents(listener))
                {
                    LoopsData dataCounted = new LoopsData(graph);
                    dataCounted.detectedCountedLoops();
                    for (LoopEx loop : dataCounted.countedLoops())
                    {
                        if (LoopTransformations.isUnrollableLoop(loop) && getPolicies().shouldPartiallyUnroll(loop))
                        {
                            if (loop.loopBegin().isSimpleLoop())
                            {
                                // First perform the pre/post transformation and do the partial unroll
                                // when we come around again.
                                LoopTransformations.insertPrePostLoops(loop);
                            }
                            else
                            {
                                LoopTransformations.partialUnroll(loop);
                            }
                            changed = true;
                        }
                    }
                    dataCounted.deleteUnusedNodes();

                    if (!listener.getNodes().isEmpty())
                    {
                        canonicalizer.applyIncremental(graph, context, listener.getNodes());
                        listener.getNodes().clear();
                    }
                }
            }
        }
    }
}
