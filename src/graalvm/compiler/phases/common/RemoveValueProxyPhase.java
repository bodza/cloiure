package graalvm.compiler.phases.common;

import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.ProxyNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.Phase;

public class RemoveValueProxyPhase extends Phase {

    @Override
    protected void run(StructuredGraph graph) {
        for (LoopExitNode exit : graph.getNodes(LoopExitNode.TYPE)) {
            for (ProxyNode vpn : exit.proxies().snapshot()) {
                vpn.replaceAtUsagesAndDelete(vpn.value());
            }
            FrameState stateAfter = exit.stateAfter();
            if (stateAfter != null) {
                exit.setStateAfter(null);
                if (stateAfter.hasNoUsages()) {
                    GraphUtil.killWithUnusedFloatingInputs(stateAfter);
                }
            }
        }
        graph.setHasValueProxies(false);
    }
}
