package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.util.GraalError;

///
// Stub to allocate a {@link NullPointerException} thrown by a bytecode.
///
// @class NullPointerExceptionStub
public final class NullPointerExceptionStub extends CreateExceptionStub
{
    // @cons NullPointerExceptionStub
    public NullPointerExceptionStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("createNullPointerException", __providers, __linkage);
    }

    @Override
    protected Object getConstantParameterValue(int __index, String __name)
    {
        GraalError.guarantee(__index == 0, "unknown parameter %s at index %d", __name, __index);
        return this.___providers.getRegisters().getThreadRegister();
    }

    @Snippet
    private static Object createNullPointerException(@Snippet.ConstantParameter Register __threadRegister)
    {
        return createException(__threadRegister, NullPointerException.class);
    }
}
