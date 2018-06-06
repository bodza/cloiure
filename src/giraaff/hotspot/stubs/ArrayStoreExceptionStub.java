package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.util.GraalError;

// @class ArrayStoreExceptionStub
public final class ArrayStoreExceptionStub extends CreateExceptionStub
{
    // @cons ArrayStoreExceptionStub
    public ArrayStoreExceptionStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("createArrayStoreException", __providers, __linkage);
    }

    @Override
    protected Object getConstantParameterValue(int __index, String __name)
    {
        GraalError.guarantee(__index == 1, "unknown parameter %s at index %d", __name, __index);
        return this.___providers.getRegisters().getThreadRegister();
    }

    @Snippet
    private static Object createArrayStoreException(@Snippet.NonNullParameter Object __object, @Snippet.ConstantParameter Register __threadRegister)
    {
        KlassPointer __klass = HotSpotReplacementsUtil.loadHub(__object);
        return createException(__threadRegister, ArrayStoreException.class, __klass);
    }
}
