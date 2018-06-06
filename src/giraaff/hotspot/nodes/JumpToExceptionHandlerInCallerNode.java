package giraaff.hotspot.nodes;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotNodeLIRBuilder;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.word.Word;

///
// Sets up the {@linkplain HotSpotBackend#EXCEPTION_HANDLER_IN_CALLER arguments} expected by an
// exception handler in the caller's frame, removes the current frame and jumps to said handler.
///
// @class JumpToExceptionHandlerInCallerNode
public final class JumpToExceptionHandlerInCallerNode extends ControlSinkNode implements LIRLowerable
{
    // @def
    public static final NodeClass<JumpToExceptionHandlerInCallerNode> TYPE = NodeClass.create(JumpToExceptionHandlerInCallerNode.class);

    @Node.Input
    // @field
    ValueNode ___handlerInCallerPc;
    @Node.Input
    // @field
    ValueNode ___exception;
    @Node.Input
    // @field
    ValueNode ___exceptionPc;

    // @cons JumpToExceptionHandlerInCallerNode
    public JumpToExceptionHandlerInCallerNode(ValueNode __handlerInCallerPc, ValueNode __exception, ValueNode __exceptionPc)
    {
        super(TYPE, StampFactory.forVoid());
        this.___handlerInCallerPc = __handlerInCallerPc;
        this.___exception = __exception;
        this.___exceptionPc = __exceptionPc;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        ((HotSpotNodeLIRBuilder) __gen).emitJumpToExceptionHandlerInCaller(this.___handlerInCallerPc, this.___exception, this.___exceptionPc);
    }

    @Node.NodeIntrinsic
    public static native void jumpToExceptionHandlerInCaller(Word __handlerInCallerPc, Object __exception, Word __exceptionPc);
}
