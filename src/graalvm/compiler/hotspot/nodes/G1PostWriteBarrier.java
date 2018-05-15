package graalvm.compiler.hotspot.nodes;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_64;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_64;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

@NodeInfo(cycles = CYCLES_64, size = SIZE_64)
public class G1PostWriteBarrier extends ObjectWriteBarrier {

    public static final NodeClass<G1PostWriteBarrier> TYPE = NodeClass.create(G1PostWriteBarrier.class);
    protected final boolean alwaysNull;

    public G1PostWriteBarrier(AddressNode address, ValueNode value, boolean precise, boolean alwaysNull) {
        this(TYPE, address, value, precise, alwaysNull);
    }

    protected G1PostWriteBarrier(NodeClass<? extends G1PostWriteBarrier> c, AddressNode address, ValueNode value, boolean precise, boolean alwaysNull) {
        super(c, address, value, precise);
        this.alwaysNull = alwaysNull;
    }

    public boolean alwaysNull() {
        return alwaysNull;
    }
}
