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
 * The {@code MonitorEnterNode} represents the acquisition of a monitor.
 */
public class MonitorEnterNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorEnter, MemoryCheckpoint.Single
{
    public static final NodeClass<MonitorEnterNode> TYPE = NodeClass.create(MonitorEnterNode.class);

    public MonitorEnterNode(ValueNode object, MonitorIdNode monitorId)
    {
        this(TYPE, object, monitorId);
    }

    public MonitorEnterNode(NodeClass<? extends MonitorEnterNode> c, ValueNode object, MonitorIdNode monitorId)
    {
        super(c, object, monitorId);
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
}
