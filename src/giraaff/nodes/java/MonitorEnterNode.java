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

///
// The {@code MonitorEnterNode} represents the acquisition of a monitor.
///
// @class MonitorEnterNode
public class MonitorEnterNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorEnter, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<MonitorEnterNode> TYPE = NodeClass.create(MonitorEnterNode.class);

    // @cons MonitorEnterNode
    public MonitorEnterNode(ValueNode __object, MonitorIdNode __monitorId)
    {
        this(TYPE, __object, __monitorId);
    }

    // @cons MonitorEnterNode
    public MonitorEnterNode(NodeClass<? extends MonitorEnterNode> __c, ValueNode __object, MonitorIdNode __monitorId)
    {
        super(__c, __object, __monitorId);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(object());
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualObjectNode __virtual = (VirtualObjectNode) __alias;
            if (__virtual.hasIdentity())
            {
                __tool.addLock(__virtual, getMonitorId());
                __tool.delete();
            }
        }
    }
}
