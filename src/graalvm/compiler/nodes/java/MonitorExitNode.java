package graalvm.compiler.nodes.java;

import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.MonitorExit;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.Virtualizable;
import graalvm.compiler.nodes.spi.VirtualizerTool;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import org.graalvm.word.LocationIdentity;

/**
 * The {@code MonitorExitNode} represents a monitor release. If it is the release of the monitor of
 * a synchronized method, then the return value of the method will be referenced via the edge
 * {@link #escapedReturnValue}, so that it will be materialized before releasing the monitor.
 */
public final class MonitorExitNode extends AccessMonitorNode implements Virtualizable, Lowerable, IterableNodeType, MonitorExit, MemoryCheckpoint.Single
{
    public static final NodeClass<MonitorExitNode> TYPE = NodeClass.create(MonitorExitNode.class);

    /**
     * Non-null for the monitor exit introduced due to a synchronized root method and null in all
     * other cases.
     */
    @OptionalInput ValueNode escapedReturnValue;

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
