package giraaff.loop.phases;

import giraaff.loop.LoopEx;
import giraaff.loop.LoopPolicies;
import giraaff.loop.LoopsData;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.tiers.PhaseContext;

// @class LoopPeelingPhase
public final class LoopPeelingPhase extends LoopPhase<LoopPolicies>
{
    // @cons
    public LoopPeelingPhase(LoopPolicies __policies)
    {
        super(__policies);
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        if (__graph.hasLoops())
        {
            LoopsData __data = new LoopsData(__graph);
            for (LoopEx __loop : __data.outerFirst())
            {
                if (getPolicies().shouldPeel(__loop, __data.getCFG(), __context.getMetaAccess()))
                {
                    LoopTransformations.peel(__loop);
                }
            }
            __data.deleteUnusedNodes();
        }
    }
}
