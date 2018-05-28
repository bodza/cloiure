package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.options.OptionValues;
import giraaff.util.GraalError;

public class ClassCastExceptionStub extends CreateExceptionStub
{
    public ClassCastExceptionStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("createClassCastException", options, providers, linkage);
    }

    @Override
    protected Object getConstantParameterValue(int index, String name)
    {
        GraalError.guarantee(index == 2, "unknown parameter %s at index %d", name, index);
        return providers.getRegisters().getThreadRegister();
    }

    @Snippet
    private static Object createClassCastException(@Snippet.NonNullParameter Object object, KlassPointer targetKlass, @ConstantParameter Register threadRegister)
    {
        KlassPointer objKlass = HotSpotReplacementsUtil.loadHub(object);
        return createException(threadRegister, ClassCastException.class, objKlass, targetKlass);
    }
}
