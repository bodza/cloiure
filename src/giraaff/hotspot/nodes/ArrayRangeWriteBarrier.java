package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.Lowerable;

// @class ArrayRangeWriteBarrier
public abstract class ArrayRangeWriteBarrier extends WriteBarrier implements Lowerable
{
    // @def
    public static final NodeClass<ArrayRangeWriteBarrier> TYPE = NodeClass.create(ArrayRangeWriteBarrier.class);

    @Input(InputType.Association)
    // @field
    AddressNode address;
    @Input
    // @field
    ValueNode length;

    // @field
    private final int elementStride;

    // @cons
    protected ArrayRangeWriteBarrier(NodeClass<? extends ArrayRangeWriteBarrier> __c, AddressNode __address, ValueNode __length, int __elementStride)
    {
        super(__c);
        this.address = __address;
        this.length = __length;
        this.elementStride = __elementStride;
    }

    public AddressNode getAddress()
    {
        return address;
    }

    public ValueNode getLength()
    {
        return length;
    }

    public int getElementStride()
    {
        return elementStride;
    }
}
