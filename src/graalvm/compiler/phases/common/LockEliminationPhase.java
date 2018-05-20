package graalvm.compiler.phases.common;

import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.java.AccessMonitorNode;
import graalvm.compiler.nodes.java.MonitorEnterNode;
import graalvm.compiler.nodes.java.MonitorExitNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.java.RawMonitorEnterNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.phases.Phase;

public class LockEliminationPhase extends Phase
{
    @Override
    protected void run(StructuredGraph graph)
    {
        for (MonitorExitNode monitorExitNode : graph.getNodes(MonitorExitNode.TYPE))
        {
            FixedNode next = monitorExitNode.next();
            if ((next instanceof MonitorEnterNode || next instanceof RawMonitorEnterNode))
            {
                // should never happen, osr monitor enters are always direct successors of the graph
                // start
                AccessMonitorNode monitorEnterNode = (AccessMonitorNode) next;
                if (GraphUtil.unproxify(monitorEnterNode.object()) == GraphUtil.unproxify(monitorExitNode.object()))
                {
                    /*
                     * We've coarsened the lock so use the same monitor id for the whole region,
                     * otherwise the monitor operations appear to be unrelated.
                     */
                    MonitorIdNode enterId = monitorEnterNode.getMonitorId();
                    MonitorIdNode exitId = monitorExitNode.getMonitorId();
                    if (enterId != exitId)
                    {
                        enterId.replaceAndDelete(exitId);
                    }
                    GraphUtil.removeFixedWithUnusedInputs(monitorEnterNode);
                    GraphUtil.removeFixedWithUnusedInputs(monitorExitNode);
                }
            }
        }
    }
}
