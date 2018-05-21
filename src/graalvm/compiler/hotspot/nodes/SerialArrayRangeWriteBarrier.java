package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

public final class SerialArrayRangeWriteBarrier extends ArrayRangeWriteBarrier
{
    public static final NodeClass<SerialArrayRangeWriteBarrier> TYPE = NodeClass.create(SerialArrayRangeWriteBarrier.class);

    public SerialArrayRangeWriteBarrier(AddressNode address, ValueNode length, int elementStride)
    {
        super(TYPE, address, length, elementStride);
    }
}
