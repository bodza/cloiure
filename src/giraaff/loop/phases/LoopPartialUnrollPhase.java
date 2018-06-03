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
    // @field
    private final CanonicalizerPhase ___canonicalizer;

    // @cons
    public LoopPartialUnrollPhase(LoopPolicies __policies, CanonicalizerPhase __canonicalizer)
    {
        super(__policies);
        this.___canonicalizer = __canonicalizer;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        if (__graph.hasLoops())
        {
            HashSetNodeEventListener __listener = new HashSetNodeEventListener();
            boolean __changed = true;
            while (__changed)
            {
                __changed = false;
                try (Graph.NodeEventScope __nes = __graph.trackNodeEvents(__listener))
                {
                    LoopsData __dataCounted = new LoopsData(__graph);
                    __dataCounted.detectedCountedLoops();
                    for (LoopEx __loop : __dataCounted.countedLoops())
                    {
                        if (LoopTransformations.isUnrollableLoop(__loop) && getPolicies().shouldPartiallyUnroll(__loop))
                        {
                            if (__loop.loopBegin().isSimpleLoop())
                            {
                                // First perform the pre/post transformation and do the partial unroll
                                // when we come around again.
                                LoopTransformations.insertPrePostLoops(__loop);
                            }
                            else
                            {
                                LoopTransformations.partialUnroll(__loop);
                            }
                            __changed = true;
                        }
                    }
                    __dataCounted.deleteUnusedNodes();

                    if (!__listener.getNodes().isEmpty())
                    {
                        this.___canonicalizer.applyIncremental(__graph, __context, __listener.getNodes());
                        __listener.getNodes().clear();
                    }
                }
            }
        }
    }
}
