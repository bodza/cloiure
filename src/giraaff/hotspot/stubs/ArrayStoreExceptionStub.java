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

public class ArrayStoreExceptionStub extends CreateExceptionStub
{
    public ArrayStoreExceptionStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("createArrayStoreException", options, providers, linkage);
    }

    @Override
    protected Object getConstantParameterValue(int index, String name)
    {
        GraalError.guarantee(index == 1, "unknown parameter %s at index %d", name, index);
        return providers.getRegisters().getThreadRegister();
    }

    @Snippet
    private static Object createArrayStoreException(@Snippet.NonNullParameter Object object, @ConstantParameter Register threadRegister)
    {
        KlassPointer klass = HotSpotReplacementsUtil.loadHub(object);
        return createException(threadRegister, ArrayStoreException.class, klass);
    }
}
