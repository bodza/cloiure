package giraaff.nodes.java;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.MonitorEnter;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * The {@code RawMonitorEnterNode} represents the acquisition of a monitor. The object needs to
 * already be non-null and the hub is an additional parameter to the node.
 */
// @class RawMonitorEnterNode
public final class RawMonitorEnterNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorEnter, MemoryCheckpoint.Single
{
    public static final NodeClass<RawMonitorEnterNode> TYPE = NodeClass.create(RawMonitorEnterNode.class);

    @Input ValueNode hub;

    // @cons
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
