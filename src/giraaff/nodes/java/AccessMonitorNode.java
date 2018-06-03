package giraaff.nodes.java;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;

/**
 * The {@code AccessMonitorNode} is the base class of both monitor acquisition and release.
 *
 * The Java bytecode specification allows non-balanced locking. Graal does not handle such cases
 * and throws a {@link BailoutException} instead during graph building.
 */
// @NodeInfo.allowedUsageTypes "Memory"
// @class AccessMonitorNode
public abstract class AccessMonitorNode extends AbstractMemoryCheckpoint implements MemoryCheckpoint, DeoptimizingNode.DeoptBefore, DeoptimizingNode.DeoptAfter
{
    // @def
    public static final NodeClass<AccessMonitorNode> TYPE = NodeClass.create(AccessMonitorNode.class);

    @OptionalInput(InputType.State)
    // @field
    FrameState stateBefore;
    @Input
    // @field
    ValueNode object;
    @Input(InputType.Association)
    // @field
    MonitorIdNode monitorId;

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public FrameState stateBefore()
    {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __f)
    {
        updateUsages(stateBefore, __f);
        stateBefore = __f;
    }

    public ValueNode object()
    {
        return object;
    }

    public void setObject(ValueNode __lockedObject)
    {
        updateUsages(this.object, __lockedObject);
        this.object = __lockedObject;
    }

    public MonitorIdNode getMonitorId()
    {
        return monitorId;
    }

    /**
     * Creates a new AccessMonitor instruction.
     *
     * @param object the instruction producing the object
     */
    // @cons
    protected AccessMonitorNode(NodeClass<? extends AccessMonitorNode> __c, ValueNode __object, MonitorIdNode __monitorId)
    {
        super(__c, StampFactory.forVoid());
        this.object = __object;
        this.monitorId = __monitorId;
    }
}
