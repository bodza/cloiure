package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotNodeLIRBuilder;
import giraaff.hotspot.stubs.ExceptionHandlerStub;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;

/**
 * Jumps to the exception handler specified by {@link #address}. This node is specific for the
 * {@link ExceptionHandlerStub} and should not be used elswhere.
 */
// @class JumpToExceptionHandlerNode
public final class JumpToExceptionHandlerNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<JumpToExceptionHandlerNode> TYPE = NodeClass.create(JumpToExceptionHandlerNode.class);

    @Input
    // @field
    ValueNode address;

    // @cons
    public JumpToExceptionHandlerNode(ValueNode __address)
    {
        super(TYPE, StampFactory.forVoid());
        this.address = __address;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        ((HotSpotNodeLIRBuilder) __gen).emitJumpToExceptionHandler(address);
    }

    @NodeIntrinsic
    public static native void jumpToExceptionHandler(Word address);
}
