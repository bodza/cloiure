package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

// @class G1ArrayRangePreWriteBarrier
public final class G1ArrayRangePreWriteBarrier extends ArrayRangeWriteBarrier
{
    // @def
    public static final NodeClass<G1ArrayRangePreWriteBarrier> TYPE = NodeClass.create(G1ArrayRangePreWriteBarrier.class);

    // @cons G1ArrayRangePreWriteBarrier
    public G1ArrayRangePreWriteBarrier(AddressNode __address, ValueNode __length, int __elementStride)
    {
        super(TYPE, __address, __length, __elementStride);
    }
}
