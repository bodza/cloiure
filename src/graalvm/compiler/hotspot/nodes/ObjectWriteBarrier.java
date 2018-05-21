package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

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
