package graalvm.compiler.nodes.java;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.DeoptimizingNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;

import jdk.vm.ci.code.BailoutException;

/**
 * The {@code AccessMonitorNode} is the base class of both monitor acquisition and release.
 *
 * The Java bytecode specification allows non-balanced locking. Graal does not handle such cases and
 * throws a {@link BailoutException} instead during graph building.
 */
public abstract class AccessMonitorNode extends AbstractMemoryCheckpoint implements MemoryCheckpoint, DeoptimizingNode.DeoptBefore, DeoptimizingNode.DeoptAfter
{
    public static final NodeClass<AccessMonitorNode> TYPE = NodeClass.create(AccessMonitorNode.class);
    @OptionalInput(InputType.State) FrameState stateBefore;
    @Input ValueNode object;
    @Input(InputType.Association) MonitorIdNode monitorId;

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
    public void setStateBefore(FrameState f)
    {
        updateUsages(stateBefore, f);
        stateBefore = f;
    }

    public ValueNode object()
    {
        return object;
    }

    public void setObject(ValueNode lockedObject)
    {
        updateUsages(this.object, lockedObject);
        this.object = lockedObject;
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
    protected AccessMonitorNode(NodeClass<? extends AccessMonitorNode> c, ValueNode object, MonitorIdNode monitorId)
    {
        super(c, StampFactory.forVoid());
        this.object = object;
        this.monitorId = monitorId;
    }
}
