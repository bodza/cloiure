package giraaff.loop.phases;

import giraaff.loop.LoopEx;
import giraaff.loop.LoopPolicies;
import giraaff.loop.LoopsData;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.PhaseContext;

// @class LoopFullUnrollPhase
public final class LoopFullUnrollPhase extends LoopPhase<LoopPolicies>
{
    private final CanonicalizerPhase canonicalizer;

    // @cons
    public LoopFullUnrollPhase(CanonicalizerPhase canonicalizer, LoopPolicies policies)
    {
        super(policies);
        this.canonicalizer = canonicalizer;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        if (graph.hasLoops())
        {
            boolean peeled;
            do
            {
                peeled = false;
                final LoopsData dataCounted = new LoopsData(graph);
                dataCounted.detectedCountedLoops();
                for (LoopEx loop : dataCounted.countedLoops())
                {
                    if (getPolicies().shouldFullUnroll(loop))
                    {
                        LoopTransformations.fullUnroll(loop, context, canonicalizer);
                        peeled = true;
                        break;
                    }
                }
                dataCounted.deleteUnusedNodes();
            } while (peeled);
        }
    }
}
