package giraaff.phases.common;

import giraaff.nodes.FrameState;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;

public class RemoveValueProxyPhase extends Phase
{
    @Override
    protected void run(StructuredGraph graph)
    {
        for (LoopExitNode exit : graph.getNodes(LoopExitNode.TYPE))
        {
            for (ProxyNode vpn : exit.proxies().snapshot())
            {
                vpn.replaceAtUsagesAndDelete(vpn.value());
            }
            FrameState stateAfter = exit.stateAfter();
            if (stateAfter != null)
            {
                exit.setStateAfter(null);
                if (stateAfter.hasNoUsages())
                {
                    GraphUtil.killWithUnusedFloatingInputs(stateAfter);
                }
            }
        }
        graph.setHasValueProxies(false);
    }
}
