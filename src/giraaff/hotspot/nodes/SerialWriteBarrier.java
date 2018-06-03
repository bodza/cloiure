package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.memory.address.AddressNode;

// @class SerialWriteBarrier
public final class SerialWriteBarrier extends ObjectWriteBarrier
{
    // @def
    public static final NodeClass<SerialWriteBarrier> TYPE = NodeClass.create(SerialWriteBarrier.class);

    // @cons
    public SerialWriteBarrier(AddressNode __address, boolean __precise)
    {
        this(TYPE, __address, __precise);
    }

    // @cons
    protected SerialWriteBarrier(NodeClass<? extends SerialWriteBarrier> __c, AddressNode __address, boolean __precise)
    {
        super(__c, __address, null, __precise);
    }
}
