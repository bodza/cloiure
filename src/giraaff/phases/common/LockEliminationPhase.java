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

// @class LockEliminationPhase
public final class LockEliminationPhase extends Phase
{
    @Override
    protected void run(StructuredGraph __graph)
    {
        for (MonitorExitNode __monitorExitNode : __graph.getNodes(MonitorExitNode.TYPE))
        {
            FixedNode __next = __monitorExitNode.next();
            if ((__next instanceof MonitorEnterNode || __next instanceof RawMonitorEnterNode))
            {
                // should never happen, osr monitor enters are always direct successors of the graph
                // start
                AccessMonitorNode __monitorEnterNode = (AccessMonitorNode) __next;
                if (GraphUtil.unproxify(__monitorEnterNode.object()) == GraphUtil.unproxify(__monitorExitNode.object()))
                {
                    // We've coarsened the lock so use the same monitor id for the whole region,
                    // otherwise the monitor operations appear to be unrelated.
                    MonitorIdNode __enterId = __monitorEnterNode.getMonitorId();
                    MonitorIdNode __exitId = __monitorExitNode.getMonitorId();
                    if (__enterId != __exitId)
                    {
                        __enterId.replaceAndDelete(__exitId);
                    }
                    GraphUtil.removeFixedWithUnusedInputs(__monitorEnterNode);
                    GraphUtil.removeFixedWithUnusedInputs(__monitorExitNode);
                }
            }
        }
    }
}
