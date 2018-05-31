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

/**
 * Stub called by the {@linkplain HotSpotRuntime#exceptionHandlerEntryMark exception
 * handler entry point} in a compiled method. This entry point is used when returning to a method to
 * handle an exception thrown by a callee. It is not used for routing implicit exceptions.
 * Therefore, it does not need to save any registers as HotSpot uses a caller save convention.
 *
 * The descriptor for a call to this stub is {@link HotSpotBackend#EXCEPTION_HANDLER}.
 */
// @class ExceptionHandlerStub
public final class ExceptionHandlerStub extends SnippetStub
{
    // @cons
    public ExceptionHandlerStub(HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("exceptionHandler", providers, linkage);
    }

    /**
     * This stub is called when returning to a method to handle an exception thrown by a callee.
     * It is not used for routing implicit exceptions. Therefore, it does not need to save any
     * registers as HotSpot uses a caller save convention.
     */
    @Override
    public boolean preservesRegisters()
    {
        return false;
    }

    @Override
    protected Object getConstantParameterValue(int index, String name)
    {
        if (index == 2)
        {
            return providers.getRegisters().getThreadRegister();
        }
        throw GraalError.shouldNotReachHere("unknown parameter " + name + " at index " + index);
    }

    @Snippet
    private static void exceptionHandler(Object exception, Word exceptionPc, @ConstantParameter Register threadRegister)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        HotSpotReplacementsUtil.writeExceptionOop(thread, exception);
        HotSpotReplacementsUtil.writeExceptionPc(thread, exceptionPc);

        // patch throwing pc into return address so that deoptimization finds the right debug info
        PatchReturnAddressNode.patchReturnAddress(exceptionPc);

        Word handlerPc = exceptionHandlerForPc(EXCEPTION_HANDLER_FOR_PC, thread);

        // patch the return address so that this stub returns to the exception handler
        JumpToExceptionHandlerNode.jumpToExceptionHandler(handlerPc);
    }

    public static final ForeignCallDescriptor EXCEPTION_HANDLER_FOR_PC = StubUtil.newDescriptor(ExceptionHandlerStub.class, "exceptionHandlerForPc", Word.class, Word.class);

    @NodeIntrinsic(value = StubForeignCallNode.class)
    public static native Word exceptionHandlerForPc(@ConstantNodeParameter ForeignCallDescriptor exceptionHandlerForPc, Word thread);
}
