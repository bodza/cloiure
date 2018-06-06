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
    // @field
    private final CanonicalizerPhase ___canonicalizer;

    // @cons LoopFullUnrollPhase
    public LoopFullUnrollPhase(CanonicalizerPhase __canonicalizer, LoopPolicies __policies)
    {
        super(__policies);
        this.___canonicalizer = __canonicalizer;
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        if (__graph.hasLoops())
        {
            boolean __peeled;
            do
            {
                __peeled = false;
                final LoopsData __dataCounted = new LoopsData(__graph);
                __dataCounted.detectedCountedLoops();
                for (LoopEx __loop : __dataCounted.countedLoops())
                {
                    if (getPolicies().shouldFullUnroll(__loop))
                    {
                        LoopTransformations.fullUnroll(__loop, __context, this.___canonicalizer);
                        __peeled = true;
                        break;
                    }
                }
                __dataCounted.deleteUnusedNodes();
            } while (__peeled);
        }
    }
}
