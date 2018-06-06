package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

// @class G1ArrayRangePostWriteBarrier
public final class G1ArrayRangePostWriteBarrier extends ArrayRangeWriteBarrier
{
    // @def
    public static final NodeClass<G1ArrayRangePostWriteBarrier> TYPE = NodeClass.create(G1ArrayRangePostWriteBarrier.class);

    // @cons G1ArrayRangePostWriteBarrier
    public G1ArrayRangePostWriteBarrier(AddressNode __address, ValueNode __length, int __elementStride)
    {
        super(TYPE, __address, __length, __elementStride);
    }
}
