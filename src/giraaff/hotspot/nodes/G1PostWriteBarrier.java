package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

// @class G1PostWriteBarrier
public final class G1PostWriteBarrier extends ObjectWriteBarrier
{
    public static final NodeClass<G1PostWriteBarrier> TYPE = NodeClass.create(G1PostWriteBarrier.class);

    protected final boolean alwaysNull;

    // @cons
    public G1PostWriteBarrier(AddressNode address, ValueNode value, boolean precise, boolean alwaysNull)
    {
        this(TYPE, address, value, precise, alwaysNull);
    }

    // @cons
    protected G1PostWriteBarrier(NodeClass<? extends G1PostWriteBarrier> c, AddressNode address, ValueNode value, boolean precise, boolean alwaysNull)
    {
        super(c, address, value, precise);
        this.alwaysNull = alwaysNull;
    }

    public boolean alwaysNull()
    {
        return alwaysNull;
    }
}
