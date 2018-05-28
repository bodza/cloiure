package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

public abstract class ObjectWriteBarrier extends WriteBarrier
{
    public static final NodeClass<ObjectWriteBarrier> TYPE = NodeClass.create(ObjectWriteBarrier.class);

    @Input(InputType.Association) protected AddressNode address;
    @OptionalInput protected ValueNode value;
    protected final boolean precise;

    protected ObjectWriteBarrier(NodeClass<? extends ObjectWriteBarrier> c, AddressNode address, ValueNode value, boolean precise)
    {
        super(c);
        this.address = address;
        this.value = value;
        this.precise = precise;
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
