package graalvm.compiler.hotspot;

import graalvm.compiler.core.match.MatchableNode;
import graalvm.compiler.lir.gen.LIRGenerator;
import graalvm.compiler.nodes.CompressionNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * This interface defines the contract a HotSpot backend LIR generator needs to fulfill in addition
 * to abstract methods from {@link LIRGenerator} and {@link NodeLIRBuilderTool}.
 */
@MatchableNode(nodeClass = CompressionNode.class, inputs = {"value"})
public interface HotSpotNodeLIRBuilder
{
    void emitPatchReturnAddress(ValueNode address);

    default void emitJumpToExceptionHandler(ValueNode address)
    {
        emitPatchReturnAddress(address);
    }

    void emitJumpToExceptionHandlerInCaller(ValueNode handlerInCallerPc, ValueNode exception, ValueNode exceptionPc);
}
