package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.HotSpotNodeLIRBuilder;
import graalvm.compiler.nodes.ControlSinkNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.word.Word;

/**
 * Sets up the {@linkplain HotSpotBackend#EXCEPTION_HANDLER_IN_CALLER arguments} expected by an
 * exception handler in the caller's frame, removes the current frame and jumps to said handler.
 */
public final class JumpToExceptionHandlerInCallerNode extends ControlSinkNode implements LIRLowerable
{
    public static final NodeClass<JumpToExceptionHandlerInCallerNode> TYPE = NodeClass.create(JumpToExceptionHandlerInCallerNode.class);
    @Input ValueNode handlerInCallerPc;
    @Input ValueNode exception;
    @Input ValueNode exceptionPc;

    public JumpToExceptionHandlerInCallerNode(ValueNode handlerInCallerPc, ValueNode exception, ValueNode exceptionPc)
    {
        super(TYPE, StampFactory.forVoid());
        this.handlerInCallerPc = handlerInCallerPc;
        this.exception = exception;
        this.exceptionPc = exceptionPc;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        ((HotSpotNodeLIRBuilder) gen).emitJumpToExceptionHandlerInCaller(handlerInCallerPc, exception, exceptionPc);
    }

    @NodeIntrinsic
    public static native void jumpToExceptionHandlerInCaller(Word handlerInCallerPc, Object exception, Word exceptionPc);
}
