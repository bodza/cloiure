package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_64;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_64;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

@NodeInfo(cycles = CYCLES_64, size = SIZE_64)
public class G1ArrayRangePostWriteBarrier extends ArrayRangeWriteBarrier {
    public static final NodeClass<G1ArrayRangePostWriteBarrier> TYPE = NodeClass.create(G1ArrayRangePostWriteBarrier.class);

    public G1ArrayRangePostWriteBarrier(AddressNode address, ValueNode length, int elementStride) {
        super(TYPE, address, length, elementStride);
    }

}
