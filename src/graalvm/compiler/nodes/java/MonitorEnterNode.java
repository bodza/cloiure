package graalvm.compiler.nodes.java;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.MonitorEnter;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;

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
