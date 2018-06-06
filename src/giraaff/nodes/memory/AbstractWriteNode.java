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

// @NodeInfo.allowedUsageTypes "InputType.Memory, InputType.Guard"
// @class AbstractWriteNode
public abstract class AbstractWriteNode extends FixedAccessNode implements StateSplit, MemoryCheckpoint.Single, MemoryAccess, GuardingNode
{
    // @def
    public static final NodeClass<AbstractWriteNode> TYPE = NodeClass.create(AbstractWriteNode.class);

    @Node.Input
    // @field
    ValueNode ___value;
    @Node.OptionalInput(InputType.StateI)
    // @field
    FrameState ___stateAfter;
    @Node.OptionalInput(InputType.Memory)
    // @field
    Node ___lastLocationAccess;

    @Override
    public FrameState stateAfter()
    {
        return this.___stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(this.___stateAfter, __x);
        this.___stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    public ValueNode value()
    {
        return this.___value;
    }

    // @cons AbstractWriteNode
    protected AbstractWriteNode(NodeClass<? extends AbstractWriteNode> __c, AddressNode __address, LocationIdentity __location, ValueNode __value, HeapAccess.BarrierType __barrierType)
    {
        super(__c, __address, __location, StampFactory.forVoid(), __barrierType);
        this.___value = __value;
    }

    @Override
    public boolean isAllowedUsageType(InputType __type)
    {
        return (__type == InputType.Guard && getNullCheck()) ? true : super.isAllowedUsageType(__type);
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return (MemoryNode) this.___lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __lla)
    {
        Node __newLla = ValueNodeUtil.asNode(__lla);
        updateUsages(this.___lastLocationAccess, __newLla);
        this.___lastLocationAccess = __newLla;
    }
}
