package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotNodeLIRBuilder;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;

///
// Modifies the return address of the current frame.
///
// @class PatchReturnAddressNode
public final class PatchReturnAddressNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<PatchReturnAddressNode> TYPE = NodeClass.create(PatchReturnAddressNode.class);

    @Node.Input
    // @field
    ValueNode ___address;

    // @cons PatchReturnAddressNode
    public PatchReturnAddressNode(ValueNode __address)
    {
        super(TYPE, StampFactory.forVoid());
        this.___address = __address;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        ((HotSpotNodeLIRBuilder) __gen).emitPatchReturnAddress(this.___address);
    }

    @Node.NodeIntrinsic
    public static native void patchReturnAddress(Word __address);
}
