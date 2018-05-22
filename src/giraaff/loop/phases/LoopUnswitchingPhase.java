package giraaff.loop.phases;

import java.util.List;

import giraaff.loop.LoopEx;
import giraaff.loop.LoopPolicies;
import giraaff.loop.LoopsData;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.StructuredGraph;

public class LoopUnswitchingPhase extends ContextlessLoopPhase<LoopPolicies>
{
    public LoopUnswitchingPhase(LoopPolicies policies)
    {
        super(policies);
    }

    @Override
    protected void run(StructuredGraph graph)
    {
        if (graph.hasLoops())
        {
            boolean unswitched;
            do
            {
                unswitched = false;
                final LoopsData dataUnswitch = new LoopsData(graph);
                for (LoopEx loop : dataUnswitch.outerFirst())
                {
                    if (getPolicies().shouldTryUnswitch(loop))
                    {
                        List<ControlSplitNode> controlSplits = LoopTransformations.findUnswitchable(loop);
                        if (controlSplits != null)
                        {
                            if (getPolicies().shouldUnswitch(loop, controlSplits))
                            {
                                LoopTransformations.unswitch(loop, controlSplits);
                                unswitched = true;
                                break;
                            }
                        }
                    }
                }
            } while (unswitched);
        }
    }
}
