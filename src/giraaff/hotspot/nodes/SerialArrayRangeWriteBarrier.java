package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

public final class SerialArrayRangeWriteBarrier extends ArrayRangeWriteBarrier
{
    public static final NodeClass<SerialArrayRangeWriteBarrier> TYPE = NodeClass.create(SerialArrayRangeWriteBarrier.class);

    public SerialArrayRangeWriteBarrier(AddressNode address, ValueNode length, int elementStride)
    {
        super(TYPE, address, length, elementStride);
    }
}
