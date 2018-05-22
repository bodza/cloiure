package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

public final class G1ArrayRangePreWriteBarrier extends ArrayRangeWriteBarrier
{
    public static final NodeClass<G1ArrayRangePreWriteBarrier> TYPE = NodeClass.create(G1ArrayRangePreWriteBarrier.class);

    public G1ArrayRangePreWriteBarrier(AddressNode address, ValueNode length, int elementStride)
    {
        super(TYPE, address, length, elementStride);
    }
}
