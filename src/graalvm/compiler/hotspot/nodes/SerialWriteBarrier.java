package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.memory.address.AddressNode;

public class SerialWriteBarrier extends ObjectWriteBarrier
{
    public static final NodeClass<SerialWriteBarrier> TYPE = NodeClass.create(SerialWriteBarrier.class);

    public SerialWriteBarrier(AddressNode address, boolean precise)
    {
        this(TYPE, address, precise);
    }

    protected SerialWriteBarrier(NodeClass<? extends SerialWriteBarrier> c, AddressNode address, boolean precise)
    {
        super(c, address, null, precise);
    }
}
