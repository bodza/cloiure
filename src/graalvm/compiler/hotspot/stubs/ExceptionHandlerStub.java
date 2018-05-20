package graalvm.compiler.hotspot.stubs;

import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.nodes.JumpToExceptionHandlerNode.jumpToExceptionHandler;
import static graalvm.compiler.hotspot.nodes.PatchReturnAddressNode.patchReturnAddress;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.readExceptionOop;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.readExceptionPc;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.registerAsWord;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.writeExceptionOop;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.writeExceptionPc;
import static graalvm.compiler.hotspot.stubs.StubUtil.decipher;
import static graalvm.compiler.hotspot.stubs.StubUtil.fatal;
import static graalvm.compiler.hotspot.stubs.StubUtil.newDescriptor;
import static graalvm.compiler.hotspot.stubs.StubUtil.printf;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Fold.InjectedParameter;
import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.StubForeignCallNode;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.word.Word;
import org.graalvm.word.WordFactory;

import jdk.vm.ci.code.Register;

/**
 * Stub called by the {@linkplain GraalHotSpotVMConfig#MARKID_EXCEPTION_HANDLER_ENTRY exception
 * handler entry point} in a compiled method. This entry point is used when returning to a method to
 * handle an exception thrown by a callee. It is not used for routing implicit exceptions.
 * Therefore, it does not need to save any registers as HotSpot uses a caller save convention.
 *
 * The descriptor for a call to this stub is {@link HotSpotBackend#EXCEPTION_HANDLER}.
 */
public class ExceptionHandlerStub extends SnippetStub
{
    public ExceptionHandlerStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("exceptionHandler", options, providers, linkage);
    }

    /**
     * This stub is called when returning to a method to handle an exception thrown by a callee. It
     * is not used for routing implicit exceptions. Therefore, it does not need to save any
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
        return options;
    }

    @Snippet
    private static void exceptionHandler(Object exception, Word exceptionPc, @ConstantParameter Register threadRegister, @ConstantParameter OptionValues options)
    {
        Word thread = registerAsWord(threadRegister);
        writeExceptionOop(thread, exception);
        writeExceptionPc(thread, exceptionPc);

        // patch throwing pc into return address so that deoptimization finds the right debug info
        patchReturnAddress(exceptionPc);

        Word handlerPc = exceptionHandlerForPc(EXCEPTION_HANDLER_FOR_PC, thread);

        // patch the return address so that this stub returns to the exception handler
        jumpToExceptionHandler(handlerPc);
    }

    public static final ForeignCallDescriptor EXCEPTION_HANDLER_FOR_PC = newDescriptor(ExceptionHandlerStub.class, "exceptionHandlerForPc", Word.class, Word.class);

    @NodeIntrinsic(value = StubForeignCallNode.class)
    public static native Word exceptionHandlerForPc(@ConstantNodeParameter ForeignCallDescriptor exceptionHandlerForPc, Word thread);
}
