package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.util.GraalError;

// @class ClassCastExceptionStub
public final class ClassCastExceptionStub extends CreateExceptionStub
{
    // @cons ClassCastExceptionStub
    public ClassCastExceptionStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("createClassCastException", __providers, __linkage);
    }

    @Override
    protected Object getConstantParameterValue(int __index, String __name)
    {
        GraalError.guarantee(__index == 2, "unknown parameter %s at index %d", __name, __index);
        return this.___providers.getRegisters().getThreadRegister();
    }

    @Snippet
    private static Object createClassCastException(@Snippet.NonNullParameter Object __object, KlassPointer __targetKlass, @Snippet.ConstantParameter Register __threadRegister)
    {
        KlassPointer __objKlass = HotSpotReplacementsUtil.loadHub(__object);
        return createException(__threadRegister, ClassCastException.class, __objKlass, __targetKlass);
    }
}
