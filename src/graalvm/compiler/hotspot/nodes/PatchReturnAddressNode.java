package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotNodeLIRBuilder;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;

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
