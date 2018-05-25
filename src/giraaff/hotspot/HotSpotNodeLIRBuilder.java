package giraaff.hotspot;

import giraaff.lir.gen.LIRGenerator;
import giraaff.nodes.CompressionNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * This interface defines the contract a HotSpot backend LIR generator needs to fulfill in addition
 * to abstract methods from {@link LIRGenerator} and {@link NodeLIRBuilderTool}.
 */
public interface HotSpotNodeLIRBuilder
{
    void emitPatchReturnAddress(ValueNode address);

    default void emitJumpToExceptionHandler(ValueNode address)
    {
        emitPatchReturnAddress(address);
    }

    void emitJumpToExceptionHandlerInCaller(ValueNode handlerInCallerPc, ValueNode exception, ValueNode exceptionPc);
}
