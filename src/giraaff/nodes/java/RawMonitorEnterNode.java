package giraaff.nodes.java;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
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
// The {@code RawMonitorEnterNode} represents the acquisition of a monitor. The object needs to
// already be non-null and the hub is an additional parameter to the node.
///
// @class RawMonitorEnterNode
public final class RawMonitorEnterNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorEnter, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<RawMonitorEnterNode> TYPE = NodeClass.create(RawMonitorEnterNode.class);

    @Node.Input
    // @field
    ValueNode ___hub;

    // @cons RawMonitorEnterNode
    public RawMonitorEnterNode(ValueNode __object, ValueNode __hub, MonitorIdNode __monitorId)
    {
        super(TYPE, __object, __monitorId);
        this.___hub = __hub;
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

    public ValueNode getHub()
    {
        return this.___hub;
    }
}
