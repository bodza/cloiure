package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.JumpToExceptionHandlerNode;
import giraaff.hotspot.nodes.PatchReturnAddressNode;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.stubs.StubUtil;
import giraaff.util.GraalError;
import giraaff.word.Word;

///
// Stub called by the {@linkplain HotSpotRuntime#exceptionHandlerEntryMark exception
// handler entry point} in a compiled method. This entry point is used when returning to a method to
// handle an exception thrown by a callee. It is not used for routing implicit exceptions.
// Therefore, it does not need to save any registers as HotSpot uses a caller save convention.
//
// The descriptor for a call to this stub is {@link HotSpotBackend#EXCEPTION_HANDLER}.
///
// @class ExceptionHandlerStub
public final class ExceptionHandlerStub extends SnippetStub
{
    // @cons
    public ExceptionHandlerStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("exceptionHandler", __providers, __linkage);
    }

    ///
    // This stub is called when returning to a method to handle an exception thrown by a callee.
    // It is not used for routing implicit exceptions. Therefore, it does not need to save any
    // registers as HotSpot uses a caller save convention.
    ///
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
            return this.___providers.getRegisters().getThreadRegister();
        }
        throw GraalError.shouldNotReachHere("unknown parameter " + __name + " at index " + __index);
    }

    @Snippet
    private static void exceptionHandler(Object __exception, Word __exceptionPc, @ConstantParameter Register __threadRegister)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        HotSpotReplacementsUtil.writeExceptionOop(__thread, __exception);
        HotSpotReplacementsUtil.writeExceptionPc(__thread, __exceptionPc);

        // patch throwing pc into return address so that deoptimization finds the right debug info
        PatchReturnAddressNode.patchReturnAddress(__exceptionPc);

        Word __handlerPc = exceptionHandlerForPc(EXCEPTION_HANDLER_FOR_PC, __thread);

        // patch the return address so that this stub returns to the exception handler
        JumpToExceptionHandlerNode.jumpToExceptionHandler(__handlerPc);
    }

    // @def
    public static final ForeignCallDescriptor EXCEPTION_HANDLER_FOR_PC = StubUtil.newDescriptor(ExceptionHandlerStub.class, "exceptionHandlerForPc", Word.class, Word.class);

    @NodeIntrinsic(value = StubForeignCallNode.class)
    public static native Word exceptionHandlerForPc(@ConstantNodeParameter ForeignCallDescriptor __exceptionHandlerForPc, Word __thread);
}
