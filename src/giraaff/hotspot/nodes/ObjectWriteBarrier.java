package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

// @class ObjectWriteBarrier
public abstract class ObjectWriteBarrier extends WriteBarrier
{
    // @def
    public static final NodeClass<ObjectWriteBarrier> TYPE = NodeClass.create(ObjectWriteBarrier.class);

    @Input(InputType.Association)
    // @field
    protected AddressNode address;
    @OptionalInput
    // @field
    protected ValueNode value;
    // @field
    protected final boolean precise;

    // @cons
    protected ObjectWriteBarrier(NodeClass<? extends ObjectWriteBarrier> __c, AddressNode __address, ValueNode __value, boolean __precise)
    {
        super(__c);
        this.address = __address;
        this.value = __value;
        this.precise = __precise;
    }

    public ValueNode getValue()
    {
        return value;
    }

    public AddressNode getAddress()
    {
        return address;
    }

    public boolean usePrecise()
    {
        return precise;
    }
}
