package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.address.AddressNode;

/**
 * The {@code G1ReferentFieldReadBarrier} is added when a read access is performed to the referent
 * field of a {@link java.lang.ref.Reference} object (through a {@code LoadFieldNode} or an
 * {@code UnsafeLoadNode}). The return value of the read is passed to the snippet implementing the
 * read barrier and consequently is added to the SATB queue if the concurrent marker is enabled.
 */
public final class G1ReferentFieldReadBarrier extends ObjectWriteBarrier
{
    public static final NodeClass<G1ReferentFieldReadBarrier> TYPE = NodeClass.create(G1ReferentFieldReadBarrier.class);

    protected final boolean doLoad;

    public G1ReferentFieldReadBarrier(AddressNode address, ValueNode expectedObject, boolean doLoad)
    {
        super(TYPE, address, expectedObject, true);
        this.doLoad = doLoad;
    }

    public ValueNode getExpectedObject()
    {
        return getValue();
    }

    public boolean doLoad()
    {
        return doLoad;
    }
}
