package graalvm.compiler.loop.phases;

import graalvm.compiler.loop.LoopEx;
import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.loop.LoopsData;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.tiers.PhaseContext;

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
