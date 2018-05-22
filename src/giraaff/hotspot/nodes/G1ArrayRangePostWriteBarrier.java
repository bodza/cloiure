package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

public class G1ArrayRangePostWriteBarrier extends ArrayRangeWriteBarrier
{
    public static final NodeClass<G1ArrayRangePostWriteBarrier> TYPE = NodeClass.create(G1ArrayRangePostWriteBarrier.class);

    public G1ArrayRangePostWriteBarrier(AddressNode address, ValueNode length, int elementStride)
    {
        super(TYPE, address, length, elementStride);
    }
}
