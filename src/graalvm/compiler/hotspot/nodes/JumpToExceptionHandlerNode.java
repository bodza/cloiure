package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotNodeLIRBuilder;
import graalvm.compiler.hotspot.stubs.ExceptionHandlerStub;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;

/**
 * Jumps to the exception handler specified by {@link #address}. This node is specific for the
 * {@link ExceptionHandlerStub} and should not be used elswhere.
 */
public final class JumpToExceptionHandlerNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<JumpToExceptionHandlerNode> TYPE = NodeClass.create(JumpToExceptionHandlerNode.class);
    @Input ValueNode address;

    public JumpToExceptionHandlerNode(ValueNode address)
    {
        super(TYPE, StampFactory.forVoid());
        this.address = address;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        ((HotSpotNodeLIRBuilder) gen).emitJumpToExceptionHandler(address);
    }

    @NodeIntrinsic
    public static native void jumpToExceptionHandler(Word address);
}
