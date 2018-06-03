package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

// @class G1PostWriteBarrier
public final class G1PostWriteBarrier extends ObjectWriteBarrier
{
    // @def
    public static final NodeClass<G1PostWriteBarrier> TYPE = NodeClass.create(G1PostWriteBarrier.class);

    // @field
    protected final boolean alwaysNull;

    // @cons
    public G1PostWriteBarrier(AddressNode __address, ValueNode __value, boolean __precise, boolean __alwaysNull)
    {
        this(TYPE, __address, __value, __precise, __alwaysNull);
    }

    // @cons
    protected G1PostWriteBarrier(NodeClass<? extends G1PostWriteBarrier> __c, AddressNode __address, ValueNode __value, boolean __precise, boolean __alwaysNull)
    {
        super(__c, __address, __value, __precise);
        this.alwaysNull = __alwaysNull;
    }

    public boolean alwaysNull()
    {
        return alwaysNull;
    }
}
