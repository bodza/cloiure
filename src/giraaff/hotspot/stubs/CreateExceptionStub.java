package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.options.OptionValues;
import giraaff.replacements.nodes.CStringConstant;
import giraaff.word.Word;

/**
 * Base class for stubs that create a runtime exception.
 */
public class CreateExceptionStub extends SnippetStub
{
    protected CreateExceptionStub(String snippetMethodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super(snippetMethodName, options, providers, linkage);
    }

    private static Word classAsCString(Class<?> cls)
    {
        return CStringConstant.cstring(cls.getName().replace('.', '/'));
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception)
    {
        Word message = null;
        return createException(threadRegister, exception, message);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, Word message)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        throwAndPostJvmtiException(THROW_AND_POST_JVMTI_EXCEPTION, thread, classAsCString(exception), message);
        return HotSpotReplacementsUtil.clearPendingException(thread);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, KlassPointer klass)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        throwKlassExternalNameException(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, thread, classAsCString(exception), klass);
        return HotSpotReplacementsUtil.clearPendingException(thread);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, KlassPointer objKlass, KlassPointer targetKlass)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        throwClassCastException(THROW_CLASS_CAST_EXCEPTION, thread, classAsCString(exception), objKlass, targetKlass);
        return HotSpotReplacementsUtil.clearPendingException(thread);
    }

    private static final ForeignCallDescriptor THROW_AND_POST_JVMTI_EXCEPTION = new ForeignCallDescriptor("throw_and_post_jvmti_exception", void.class, Word.class, Word.class, Word.class);
    private static final ForeignCallDescriptor THROW_KLASS_EXTERNAL_NAME_EXCEPTION = new ForeignCallDescriptor("throw_klass_external_name_exception", void.class, Word.class, Word.class, KlassPointer.class);
    private static final ForeignCallDescriptor THROW_CLASS_CAST_EXCEPTION = new ForeignCallDescriptor("throw_class_cast_exception", void.class, Word.class, Word.class, KlassPointer.class, KlassPointer.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwAndPostJvmtiException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, Word message);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwKlassExternalNameException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer klass);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwClassCastException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer objKlass, KlassPointer targetKlass);

    public static void registerForeignCalls(HotSpotForeignCallsProviderImpl foreignCalls)
    {
        foreignCalls.registerForeignCall(THROW_AND_POST_JVMTI_EXCEPTION, GraalHotSpotVMConfig.throwAndPostJvmtiExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, HotSpotForeignCallsProviderImpl.REEXECUTABLE, LocationIdentity.any());
        foreignCalls.registerForeignCall(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, GraalHotSpotVMConfig.throwKlassExternalNameExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, HotSpotForeignCallsProviderImpl.REEXECUTABLE, LocationIdentity.any());
        foreignCalls.registerForeignCall(THROW_CLASS_CAST_EXCEPTION, GraalHotSpotVMConfig.throwClassCastExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, HotSpotForeignCallsProviderImpl.REEXECUTABLE, LocationIdentity.any());
    }
}
