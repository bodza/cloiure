package giraaff.phases.common;

import giraaff.nodes.FixedNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.java.AccessMonitorNode;
import giraaff.nodes.java.MonitorEnterNode;
import giraaff.nodes.java.MonitorExitNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.java.RawMonitorEnterNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.Phase;

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
