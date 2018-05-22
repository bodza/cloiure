package giraaff.loop.phases;

import giraaff.loop.LoopEx;
import giraaff.loop.LoopPolicies;
import giraaff.loop.LoopsData;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.tiers.PhaseContext;

public class LoopPeelingPhase extends LoopPhase<LoopPolicies>
{
    public LoopPeelingPhase(LoopPolicies policies)
    {
        super(policies);
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        if (graph.hasLoops())
        {
            LoopsData data = new LoopsData(graph);
            for (LoopEx loop : data.outerFirst())
            {
                if (getPolicies().shouldPeel(loop, data.getCFG(), context.getMetaAccess()))
                {
                    LoopTransformations.peel(loop);
                }
            }
            data.deleteUnusedNodes();
        }
    }
}
