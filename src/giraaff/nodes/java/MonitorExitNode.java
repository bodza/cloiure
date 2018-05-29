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

/**
 * The {@code MonitorExitNode} represents a monitor release. If it is the release of the monitor of
 * a synchronized method, then the return value of the method will be referenced via the edge
 * {@link #escapedReturnValue}, so that it will be materialized before releasing the monitor.
 */
// @class MonitorExitNode
public final class MonitorExitNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorExit, MemoryCheckpoint.Single
{
    public static final NodeClass<MonitorExitNode> TYPE = NodeClass.create(MonitorExitNode.class);

    /**
     * Non-null for the monitor exit introduced due to a synchronized root method and null in all
     * other cases.
     */
    @OptionalInput ValueNode escapedReturnValue;

    // @cons
    public MonitorExitNode(ValueNode object, MonitorIdNode monitorId, ValueNode escapedReturnValue)
    {
        super(TYPE, object, monitorId);
        this.escapedReturnValue = escapedReturnValue;
    }

    /**
     * Return value is cleared when a synchronized method graph is inlined.
     */
    public void clearEscapedReturnValue()
    {
        updateUsages(escapedReturnValue, null);
        this.escapedReturnValue = null;
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
                MonitorIdNode removedLock = tool.removeLock(virtual);
                tool.delete();
            }
        }
    }
}
