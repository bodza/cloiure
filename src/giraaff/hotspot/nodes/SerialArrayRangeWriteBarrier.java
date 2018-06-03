package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

// @class SerialArrayRangeWriteBarrier
public final class SerialArrayRangeWriteBarrier extends ArrayRangeWriteBarrier
{
    // @def
    public static final NodeClass<SerialArrayRangeWriteBarrier> TYPE = NodeClass.create(SerialArrayRangeWriteBarrier.class);

    // @cons
    public SerialArrayRangeWriteBarrier(AddressNode __address, ValueNode __length, int __elementStride)
    {
        super(TYPE, __address, __length, __elementStride);
    }
}
