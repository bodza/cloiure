package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import org.graalvm.word.Pointer;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.JumpToExceptionHandlerInCallerNode;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.stubs.StubUtil;
import giraaff.nodes.UnwindNode;
import giraaff.util.GraalError;
import giraaff.word.Word;

/**
 * Stub called by an {@link UnwindNode}. This stub executes in the frame of the method throwing an
 * exception and completes by jumping to the exception handler in the calling frame.
 */
// @class UnwindExceptionToCallerStub
public final class UnwindExceptionToCallerStub extends SnippetStub
{
    // @cons
    public UnwindExceptionToCallerStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("unwindExceptionToCaller", __providers, __linkage);
    }

    /**
     * The current frame is unwound by this stub. Therefore, it does not need to save any registers
     * as HotSpot uses a caller save convention.
     */
    @Override
    public boolean preservesRegisters()
    {
        return false;
    }

    @Override
    protected Object getConstantParameterValue(int __index, String __name)
    {
        if (__index == 2)
        {
            return providers.getRegisters().getThreadRegister();
        }
        throw GraalError.shouldNotReachHere("unknown parameter " + __name + " at __index " + __index);
    }

    @Snippet
    private static void unwindExceptionToCaller(Object __exception, Word __returnAddress, @ConstantParameter Register __threadRegister)
    {
        Pointer __exceptionOop = Word.objectToTrackedPointer(__exception);
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);

        Word __handlerInCallerPc = exceptionHandlerForReturnAddress(EXCEPTION_HANDLER_FOR_RETURN_ADDRESS, __thread, __returnAddress);

        JumpToExceptionHandlerInCallerNode.jumpToExceptionHandlerInCaller(__handlerInCallerPc, __exception, __returnAddress);
    }

    // @def
    public static final ForeignCallDescriptor EXCEPTION_HANDLER_FOR_RETURN_ADDRESS = StubUtil.newDescriptor(UnwindExceptionToCallerStub.class, "exceptionHandlerForReturnAddress", Word.class, Word.class, Word.class);

    @NodeIntrinsic(value = StubForeignCallNode.class)
    public static native Word exceptionHandlerForReturnAddress(@ConstantNodeParameter ForeignCallDescriptor exceptionHandlerForReturnAddress, Word thread, Word returnAddress);
}
