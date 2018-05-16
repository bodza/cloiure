package graalvm.compiler.hotspot.stubs;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.Register;

/**
 * Stub to allocate a {@link NullPointerException} thrown by a bytecode.
 */
public class NullPointerExceptionStub extends CreateExceptionStub
{
    public NullPointerExceptionStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("createNullPointerException", options, providers, linkage);
    }

    @Override
    protected Object getConstantParameterValue(int index, String name)
    {
        GraalError.guarantee(index == 0, "unknown parameter %s at index %d", name, index);
        return providers.getRegisters().getThreadRegister();
    }

    @Snippet
    private static Object createNullPointerException(@ConstantParameter Register threadRegister)
    {
        return createException(threadRegister, NullPointerException.class);
    }
}
