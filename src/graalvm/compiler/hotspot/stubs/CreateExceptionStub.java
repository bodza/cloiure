package graalvm.compiler.hotspot.stubs;

import static jdk.vm.ci.hotspot.HotSpotCallingConventionType.NativeCall;
import static graalvm.compiler.hotspot.HotSpotForeignCallLinkage.RegisterEffect.DESTROYS_REGISTERS;
import static graalvm.compiler.hotspot.HotSpotForeignCallLinkage.Transition.SAFEPOINT;
import static graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl.REEXECUTABLE;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.clearPendingException;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.registerAsWord;
import static org.graalvm.word.LocationIdentity.any;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotForeignCallLinkage;
import graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.StubForeignCallNode;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.nodes.CStringConstant;
import graalvm.compiler.word.Word;

import jdk.vm.ci.code.Register;

/**
 * Base class for stubs that create a runtime exception.
 */
public class CreateExceptionStub extends SnippetStub {

    protected CreateExceptionStub(String snippetMethodName, OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage) {
        super(snippetMethodName, options, providers, linkage);
    }

    @Fold
    static String getInternalClassName(Class<?> cls) {
        return cls.getName().replace('.', '/');
    }

    private static Word classAsCString(Class<?> cls) {
        return CStringConstant.cstring(getInternalClassName(cls));
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception) {
        Word message = null;
        return createException(threadRegister, exception, message);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, Word message) {
        Word thread = registerAsWord(threadRegister);
        throwAndPostJvmtiException(THROW_AND_POST_JVMTI_EXCEPTION, thread, classAsCString(exception), message);
        return clearPendingException(thread);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, KlassPointer klass) {
        Word thread = registerAsWord(threadRegister);
        throwKlassExternalNameException(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, thread, classAsCString(exception), klass);
        return clearPendingException(thread);
    }

    protected static Object createException(Register threadRegister, Class<? extends Throwable> exception, KlassPointer objKlass, KlassPointer targetKlass) {
        Word thread = registerAsWord(threadRegister);
        throwClassCastException(THROW_CLASS_CAST_EXCEPTION, thread, classAsCString(exception), objKlass, targetKlass);
        return clearPendingException(thread);
    }

    private static final ForeignCallDescriptor THROW_AND_POST_JVMTI_EXCEPTION = new ForeignCallDescriptor("throw_and_post_jvmti_exception", void.class, Word.class, Word.class, Word.class);
    private static final ForeignCallDescriptor THROW_KLASS_EXTERNAL_NAME_EXCEPTION = new ForeignCallDescriptor("throw_klass_external_name_exception", void.class, Word.class, Word.class,
                    KlassPointer.class);
    private static final ForeignCallDescriptor THROW_CLASS_CAST_EXCEPTION = new ForeignCallDescriptor("throw_class_cast_exception", void.class, Word.class, Word.class, KlassPointer.class,
                    KlassPointer.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwAndPostJvmtiException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, Word message);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwKlassExternalNameException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer klass);

    @NodeIntrinsic(StubForeignCallNode.class)
    private static native void throwClassCastException(@ConstantNodeParameter ForeignCallDescriptor d, Word thread, Word type, KlassPointer objKlass, KlassPointer targetKlass);

    public static void registerForeignCalls(GraalHotSpotVMConfig c, HotSpotForeignCallsProviderImpl foreignCalls) {
        foreignCalls.registerForeignCall(THROW_AND_POST_JVMTI_EXCEPTION, c.throwAndPostJvmtiExceptionAddress, NativeCall, DESTROYS_REGISTERS, SAFEPOINT, REEXECUTABLE, any());
        foreignCalls.registerForeignCall(THROW_KLASS_EXTERNAL_NAME_EXCEPTION, c.throwKlassExternalNameExceptionAddress, NativeCall, DESTROYS_REGISTERS, SAFEPOINT, REEXECUTABLE, any());
        foreignCalls.registerForeignCall(THROW_CLASS_CAST_EXCEPTION, c.throwClassCastExceptionAddress, NativeCall, DESTROYS_REGISTERS, SAFEPOINT, REEXECUTABLE, any());
    }
}
