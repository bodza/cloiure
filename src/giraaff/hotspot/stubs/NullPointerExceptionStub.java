package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.options.OptionValues;
import giraaff.util.GraalError;

/**
 * Stub to allocate a {@link NullPointerException} thrown by a bytecode.
 */
// @class NullPointerExceptionStub
public final class NullPointerExceptionStub extends CreateExceptionStub
{
    // @cons
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
