package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotNodeLIRBuilder;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;

/**
 * Modifies the return address of the current frame.
 */
public final class PatchReturnAddressNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<PatchReturnAddressNode> TYPE = NodeClass.create(PatchReturnAddressNode.class);
    @Input ValueNode address;

    public PatchReturnAddressNode(ValueNode address)
    {
        super(TYPE, StampFactory.forVoid());
        this.address = address;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        ((HotSpotNodeLIRBuilder) gen).emitPatchReturnAddress(address);
    }

    @NodeIntrinsic
    public static native void patchReturnAddress(Word address);
}
