package graalvm.compiler.hotspot.stubs;

import jdk.vm.ci.code.Register;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.options.OptionValues;

/**
 */
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
