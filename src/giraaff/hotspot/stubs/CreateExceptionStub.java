package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.replacements.nodes.CStringConstant;
import giraaff.word.Word;

/**
 * Base class for stubs that create a runtime exception.
 */
// @class CreateExceptionStub
public class CreateExceptionStub extends SnippetStub
{
    // @cons
    protected CreateExceptionStub(String __snippetMethodName, HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super(__snippetMethodName, __providers, __linkage);
    }

    private static Word classAsCString(Class<?> __cls)
    {
        return CStringConstant.cstring(__cls.getName().replace('.', '/'));
    }

    protected static Object createException(Register __threadRegister, Class<? extends Throwable> __exception)
    {
        return createException(__threadRegister, __exception, (Word) null);
    }

    protected static Object createException(Register __threadRegister, Class<? extends Throwable> __exception, Word __message)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        throwAndPostJvmtiException(THROW_AND_POST_JVMTI_EXCEPTION, __thread, classAsCString(__exception), __message);
        return HotSpotReplacementsUtil.clearPendingException(__thread);
    }

    protected static Object createException(Register __threadRegister, Class<? extends Throwable> __exception, KlassPointer __klass)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        throwKlassExternalNameException(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, __thread, classAsCString(__exception), __klass);
        return HotSpotReplacementsUtil.clearPendingException(__thread);
    }

    protected static Object createException(Register __threadRegister, Class<? extends Throwable> __exception, KlassPointer __objKlass, KlassPointer __targetKlass)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        throwClassCastException(THROW_CLASS_CAST_EXCEPTION, __thread, classAsCString(__exception), __objKlass, __targetKlass);
        return HotSpotReplacementsUtil.clearPendingException(__thread);
    }

    // @def
    private static final ForeignCallDescriptor THROW_AND_POST_JVMTI_EXCEPTION = new ForeignCallDescriptor("throw_and_post_jvmti_exception", void.class, Word.class, Word.class, Word.class);
    // @def
    private static final ForeignCallDescriptor THROW_KLASS_EXTERNAL_NAME_EXCEPTION = new ForeignCallDescriptor("throw_klass_external_name_exception", void.class, Word.class, Word.class, KlassPointer.class);
    // @def
    private static final ForeignCallDescriptor THROW_CLASS_CAST_EXCEPTION = new ForeignCallDescriptor("throw_class_cast_exception", void.class, Word.class, Word.class, KlassPointer.class, KlassPointer.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwAndPostJvmtiException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, Word message);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwKlassExternalNameException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer klass);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwClassCastException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer objKlass, KlassPointer targetKlass);

    public static void registerForeignCalls(HotSpotForeignCallsProviderImpl __foreignCalls)
    {
        __foreignCalls.registerForeignCall(THROW_AND_POST_JVMTI_EXCEPTION, HotSpotRuntime.throwAndPostJvmtiExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, HotSpotForeignCallsProviderImpl.REEXECUTABLE, LocationIdentity.any());
        __foreignCalls.registerForeignCall(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, HotSpotRuntime.throwKlassExternalNameExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, HotSpotForeignCallsProviderImpl.REEXECUTABLE, LocationIdentity.any());
        __foreignCalls.registerForeignCall(THROW_CLASS_CAST_EXCEPTION, HotSpotRuntime.throwClassCastExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, HotSpotForeignCallsProviderImpl.REEXECUTABLE, LocationIdentity.any());
    }
}
