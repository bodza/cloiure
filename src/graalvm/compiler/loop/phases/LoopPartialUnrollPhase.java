package graalvm.compiler.loop.phases;

import graalvm.compiler.graph.Graph;
import graalvm.compiler.loop.LoopEx;
import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.loop.LoopsData;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.util.HashSetNodeEventListener;
import graalvm.compiler.phases.tiers.PhaseContext;

public class LoopPartialUnrollPhase extends LoopPhase<LoopPolicies>
{
    private final CanonicalizerPhase canonicalizer;

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
                    Graph.Mark mark = graph.getMark();
                    boolean prePostInserted = false;
                    for (LoopEx loop : dataCounted.countedLoops())
                    {
                        if (!LoopTransformations.isUnrollableLoop(loop))
                        {
                            continue;
                        }
                        if (getPolicies().shouldPartiallyUnroll(loop))
                        {
                            if (loop.loopBegin().isSimpleLoop())
                            {
                                // First perform the pre/post transformation and do the partial
                                // unroll when we come around again.
                                LoopTransformations.insertPrePostLoops(loop);
                                prePostInserted = true;
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
