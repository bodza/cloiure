package giraaff.loop.phases;

import java.util.List;

import giraaff.loop.LoopEx;
import giraaff.loop.LoopPolicies;
import giraaff.loop.LoopsData;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.StructuredGraph;

// @class LoopUnswitchingPhase
public final class LoopUnswitchingPhase extends ContextlessLoopPhase<LoopPolicies>
{
    // @cons
    public LoopUnswitchingPhase(LoopPolicies __policies)
    {
        super(__policies);
    }

    @Override
    protected void run(StructuredGraph __graph)
    {
        if (__graph.hasLoops())
        {
            boolean __unswitched;
            do
            {
                __unswitched = false;
                final LoopsData __dataUnswitch = new LoopsData(__graph);
                for (LoopEx __loop : __dataUnswitch.outerFirst())
                {
                    if (getPolicies().shouldTryUnswitch(__loop))
                    {
                        List<ControlSplitNode> __controlSplits = LoopTransformations.findUnswitchable(__loop);
                        if (__controlSplits != null)
                        {
                            if (getPolicies().shouldUnswitch(__loop, __controlSplits))
                            {
                                LoopTransformations.unswitch(__loop, __controlSplits);
                                __unswitched = true;
                                break;
                            }
                        }
                    }
                }
            } while (__unswitched);
        }
    }
}
