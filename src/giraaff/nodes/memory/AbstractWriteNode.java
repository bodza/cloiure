package giraaff.nodes.memory;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueNodeUtil;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.memory.address.AddressNode;

// @NodeInfo.allowedUsageTypes "Memory, Guard"
public abstract class AbstractWriteNode extends FixedAccessNode implements StateSplit, MemoryCheckpoint.Single, MemoryAccess, GuardingNode
{
    public static final NodeClass<AbstractWriteNode> TYPE = NodeClass.create(AbstractWriteNode.class);
    @Input ValueNode value;
    @OptionalInput(InputType.State) FrameState stateAfter;
    @OptionalInput(InputType.Memory) Node lastLocationAccess;

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    public ValueNode value()
    {
        return value;
    }

    protected AbstractWriteNode(NodeClass<? extends AbstractWriteNode> c, AddressNode address, LocationIdentity location, ValueNode value, BarrierType barrierType)
    {
        super(c, address, location, StampFactory.forVoid(), barrierType);
        this.value = value;
    }

    @Override
    public boolean isAllowedUsageType(InputType type)
    {
        return (type == InputType.Guard && getNullCheck()) ? true : super.isAllowedUsageType(type);
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return (MemoryNode) lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode lla)
    {
        Node newLla = ValueNodeUtil.asNode(lla);
        updateUsages(lastLocationAccess, newLla);
        lastLocationAccess = newLla;
    }
}
