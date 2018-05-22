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
import giraaff.options.OptionValues;
import giraaff.word.Word;

/**
 * Stub called by an {@link UnwindNode}. This stub executes in the frame of the method throwing an
 * exception and completes by jumping to the exception handler in the calling frame.
 */
public class UnwindExceptionToCallerStub extends SnippetStub
{
    public UnwindExceptionToCallerStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("unwindExceptionToCaller", options, providers, linkage);
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
    protected Object getConstantParameterValue(int index, String name)
    {
        if (index == 2)
        {
            return providers.getRegisters().getThreadRegister();
        }
        return options;
    }

    @Snippet
    private static void unwindExceptionToCaller(Object exception, Word returnAddress, @ConstantParameter Register threadRegister, @ConstantParameter OptionValues options)
    {
        Pointer exceptionOop = Word.objectToTrackedPointer(exception);
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);

        Word handlerInCallerPc = exceptionHandlerForReturnAddress(EXCEPTION_HANDLER_FOR_RETURN_ADDRESS, thread, returnAddress);

        JumpToExceptionHandlerInCallerNode.jumpToExceptionHandlerInCaller(handlerInCallerPc, exception, returnAddress);
    }

    public static final ForeignCallDescriptor EXCEPTION_HANDLER_FOR_RETURN_ADDRESS = StubUtil.newDescriptor(UnwindExceptionToCallerStub.class, "exceptionHandlerForReturnAddress", Word.class, Word.class, Word.class);

    @NodeIntrinsic(value = StubForeignCallNode.class)
    public static native Word exceptionHandlerForReturnAddress(@ConstantNodeParameter ForeignCallDescriptor exceptionHandlerForReturnAddress, Word thread, Word returnAddress);
}
