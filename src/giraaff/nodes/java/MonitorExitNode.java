package giraaff.nodes.java;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.MonitorExit;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualObjectNode;

///
// The {@code MonitorExitNode} represents a monitor release. If it is the release of the monitor of
// a synchronized method, then the return value of the method will be referenced via the edge
// {@link #escapedReturnValue}, so that it will be materialized before releasing the monitor.
///
// @class MonitorExitNode
public final class MonitorExitNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorExit, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<MonitorExitNode> TYPE = NodeClass.create(MonitorExitNode.class);

    ///
    // Non-null for the monitor exit introduced due to a synchronized root method and null in all
    // other cases.
    ///
    @OptionalInput
    // @field
    ValueNode ___escapedReturnValue;

    // @cons
    public MonitorExitNode(ValueNode __object, MonitorIdNode __monitorId, ValueNode __escapedReturnValue)
    {
        super(TYPE, __object, __monitorId);
        this.___escapedReturnValue = __escapedReturnValue;
    }

    ///
    // Return value is cleared when a synchronized method graph is inlined.
    ///
    public void clearEscapedReturnValue()
    {
        updateUsages(this.___escapedReturnValue, null);
        this.___escapedReturnValue = null;
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
                MonitorIdNode __removedLock = __tool.removeLock(__virtual);
                __tool.delete();
            }
        }
    }
}
