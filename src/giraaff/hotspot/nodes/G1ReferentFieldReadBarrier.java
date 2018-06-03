package giraaff.hotspot.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.address.AddressNode;

///
// The {@code G1ReferentFieldReadBarrier} is added when a read access is performed to the referent
// field of a {@link java.lang.ref.Reference} object (through a {@code LoadFieldNode} or an
// {@code UnsafeLoadNode}). The return value of the read is passed to the snippet implementing the
// read barrier and consequently is added to the SATB queue if the concurrent marker is enabled.
///
// @class G1ReferentFieldReadBarrier
public final class G1ReferentFieldReadBarrier extends ObjectWriteBarrier
{
    // @def
    public static final NodeClass<G1ReferentFieldReadBarrier> TYPE = NodeClass.create(G1ReferentFieldReadBarrier.class);

    // @field
    protected final boolean ___doLoad;

    // @cons
    public G1ReferentFieldReadBarrier(AddressNode __address, ValueNode __expectedObject, boolean __doLoad)
    {
        super(TYPE, __address, __expectedObject, true);
        this.___doLoad = __doLoad;
    }

    public ValueNode getExpectedObject()
    {
        return getValue();
    }

    public boolean doLoad()
    {
        return this.___doLoad;
    }
}
