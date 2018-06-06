package giraaff.hotspot.nodes;

import giraaff.graph.Node;
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

    @Node.Input(InputType.Association)
    // @field
    AddressNode ___address;
    @Node.Input
    // @field
    ValueNode ___length;

    // @field
    private final int ___elementStride;

    // @cons ArrayRangeWriteBarrier
    protected ArrayRangeWriteBarrier(NodeClass<? extends ArrayRangeWriteBarrier> __c, AddressNode __address, ValueNode __length, int __elementStride)
    {
        super(__c);
        this.___address = __address;
        this.___length = __length;
        this.___elementStride = __elementStride;
    }

    public AddressNode getAddress()
    {
        return this.___address;
    }

    public ValueNode getLength()
    {
        return this.___length;
    }

    public int getElementStride()
    {
        return this.___elementStride;
    }
}
