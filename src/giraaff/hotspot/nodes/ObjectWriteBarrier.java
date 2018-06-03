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
    protected AddressNode ___address;
    @OptionalInput
    // @field
    protected ValueNode ___value;
    // @field
    protected final boolean ___precise;

    // @cons
    protected ObjectWriteBarrier(NodeClass<? extends ObjectWriteBarrier> __c, AddressNode __address, ValueNode __value, boolean __precise)
    {
        super(__c);
        this.___address = __address;
        this.___value = __value;
        this.___precise = __precise;
    }

    public ValueNode getValue()
    {
        return this.___value;
    }

    public AddressNode getAddress()
    {
        return this.___address;
    }

    public boolean usePrecise()
    {
        return this.___precise;
    }
}
