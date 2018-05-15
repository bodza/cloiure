package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_8;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

@NodeInfo(cycles = CYCLES_8, size = SIZE_8)
public final class SerialArrayRangeWriteBarrier extends ArrayRangeWriteBarrier {
    public static final NodeClass<SerialArrayRangeWriteBarrier> TYPE = NodeClass.create(SerialArrayRangeWriteBarrier.class);

    public SerialArrayRangeWriteBarrier(AddressNode address, ValueNode length, int elementStride) {
        super(TYPE, address, length, elementStride);
    }

}
