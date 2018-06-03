package giraaff.phases.common;

import giraaff.nodes.FrameState;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.ProxyNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;

// @class RemoveValueProxyPhase
public final class RemoveValueProxyPhase extends Phase
{
    @Override
    protected void run(StructuredGraph __graph)
    {
        for (LoopExitNode __exit : __graph.getNodes(LoopExitNode.TYPE))
        {
            for (ProxyNode __vpn : __exit.proxies().snapshot())
            {
                __vpn.replaceAtUsagesAndDelete(__vpn.value());
            }
            FrameState __stateAfter = __exit.stateAfter();
            if (__stateAfter != null)
            {
                __exit.setStateAfter(null);
                if (__stateAfter.hasNoUsages())
                {
                    GraphUtil.killWithUnusedFloatingInputs(__stateAfter);
                }
            }
        }
        __graph.setHasValueProxies(false);
    }
}
