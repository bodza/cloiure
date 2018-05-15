package graalvm.compiler.hotspot.stubs;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.Register;

/**
 */
public class ArrayStoreExceptionStub extends CreateExceptionStub {

    public ArrayStoreExceptionStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage) {
        super("createArrayStoreException", options, providers, linkage);
    }

    @Override
    protected Object getConstantParameterValue(int index, String name) {
        GraalError.guarantee(index == 1, "unknown parameter %s at index %d", name, index);
        return providers.getRegisters().getThreadRegister();
    }

    @Snippet
    private static Object createArrayStoreException(@Snippet.NonNullParameter Object object, @ConstantParameter Register threadRegister) {
        KlassPointer klass = HotSpotReplacementsUtil.loadHub(object);
        return createException(threadRegister, ArrayStoreException.class, klass);
    }
}
