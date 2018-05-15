package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_4;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.memory.address.AddressNode;

@NodeInfo(cycles = CYCLES_8, size = SIZE_4)
public class SerialWriteBarrier extends ObjectWriteBarrier {

    public static final NodeClass<SerialWriteBarrier> TYPE = NodeClass.create(SerialWriteBarrier.class);

    public SerialWriteBarrier(AddressNode address, boolean precise) {
        this(TYPE, address, precise);
    }

    protected SerialWriteBarrier(NodeClass<? extends SerialWriteBarrier> c, AddressNode address, boolean precise) {
        super(c, address, null, precise);
    }
}
