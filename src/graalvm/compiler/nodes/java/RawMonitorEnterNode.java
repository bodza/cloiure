package graalvm.compiler.nodes.java;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_64;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_64;

import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.MonitorEnter;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import org.graalvm.word.LocationIdentity;

/**
 * The {@code RawMonitorEnterNode} represents the acquisition of a monitor. The object needs to
 * already be non-null and the hub is an additional parameter to the node.
 */
@NodeInfo(cycles = CYCLES_64,
          cyclesRationale = "Rough estimation of the enter operation",
          size = SIZE_64)
public final class RawMonitorEnterNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorEnter, MemoryCheckpoint.Single
{
    public static final NodeClass<RawMonitorEnterNode> TYPE = NodeClass.create(RawMonitorEnterNode.class);

    @Input ValueNode hub;

    public RawMonitorEnterNode(ValueNode object, ValueNode hub, MonitorIdNode monitorId)
    {
        super(TYPE, object, monitorId);
        this.hub = hub;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(object());
        if (alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode virtual = (VirtualObjectNode) alias;
            if (virtual.hasIdentity())
            {
                tool.addLock(virtual, getMonitorId());
                tool.delete();
            }
        }
    }

    public ValueNode getHub()
    {
        return hub;
    }
}
