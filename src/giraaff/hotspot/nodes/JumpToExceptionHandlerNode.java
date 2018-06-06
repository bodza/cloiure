package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotNodeLIRBuilder;
import giraaff.hotspot.stubs.ExceptionHandlerStub;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;

///
// Jumps to the exception handler specified by {@link #address}. This node is specific for the
// {@link ExceptionHandlerStub} and should not be used elswhere.
///
// @class JumpToExceptionHandlerNode
public final class JumpToExceptionHandlerNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<JumpToExceptionHandlerNode> TYPE = NodeClass.create(JumpToExceptionHandlerNode.class);

    @Node.Input
    // @field
    ValueNode ___address;

    // @cons JumpToExceptionHandlerNode
    public JumpToExceptionHandlerNode(ValueNode __address)
    {
        super(TYPE, StampFactory.forVoid());
        this.___address = __address;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        ((HotSpotNodeLIRBuilder) __gen).emitJumpToExceptionHandler(this.___address);
    }

    @Node.NodeIntrinsic
    public static native void jumpToExceptionHandler(Word __address);
}
