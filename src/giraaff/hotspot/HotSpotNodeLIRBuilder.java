package giraaff.hotspot;

import giraaff.lir.gen.LIRGenerator;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// This interface defines the contract a HotSpot backend LIR generator needs to fulfill in addition
// to abstract methods from {@link LIRGenerator} and {@link NodeLIRBuilderTool}.
///
// @iface HotSpotNodeLIRBuilder
public interface HotSpotNodeLIRBuilder
{
    void emitPatchReturnAddress(ValueNode __address);

    default void emitJumpToExceptionHandler(ValueNode __address)
    {
        emitPatchReturnAddress(__address);
    }

    void emitJumpToExceptionHandlerInCaller(ValueNode __handlerInCallerPc, ValueNode __exception, ValueNode __exceptionPc);
}
