package giraaff.hotspot.meta;

import java.util.EnumMap;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.collections.EconomicMap;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.target.Backend;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.HotSpotHostBackend;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.DefaultHotSpotLoweringProvider.RuntimeCalls;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.MonitorSnippets;
import giraaff.hotspot.replacements.NewObjectSnippets;
import giraaff.hotspot.replacements.ThreadSubstitutions;
import giraaff.hotspot.replacements.WriteBarrierSnippets;
import giraaff.hotspot.stubs.ArrayStoreExceptionStub;
import giraaff.hotspot.stubs.ClassCastExceptionStub;
import giraaff.hotspot.stubs.CreateExceptionStub;
import giraaff.hotspot.stubs.ExceptionHandlerStub;
import giraaff.hotspot.stubs.NewArrayStub;
import giraaff.hotspot.stubs.NewInstanceStub;
import giraaff.hotspot.stubs.NullPointerExceptionStub;
import giraaff.hotspot.stubs.OutOfBoundsExceptionStub;
import giraaff.hotspot.stubs.Stub;
import giraaff.hotspot.stubs.UnwindExceptionToCallerStub;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.java.ForeignCallDescriptors;
import giraaff.word.Word;
import giraaff.word.WordTypes;

///
// HotSpot implementation of {@link ForeignCallsProvider}.
///
// @class HotSpotHostForeignCallsProvider
public abstract class HotSpotHostForeignCallsProvider extends HotSpotForeignCallsProviderImpl
{
    // @def
    public static final ForeignCallDescriptor JAVA_TIME_MILLIS = new ForeignCallDescriptor("javaTimeMillis", long.class);
    // @def
    public static final ForeignCallDescriptor JAVA_TIME_NANOS = new ForeignCallDescriptor("javaTimeNanos", long.class);

    // @cons
    public HotSpotHostForeignCallsProvider(HotSpotGraalRuntime __runtime, MetaAccessProvider __metaAccess, CodeCacheProvider __codeCache, WordTypes __wordTypes)
    {
        super(__runtime, __metaAccess, __codeCache, __wordTypes);
    }

    protected static void link(Stub __stub)
    {
        __stub.getLinkage().setCompiledStub(__stub);
    }

    public static ForeignCallDescriptor lookupCheckcastArraycopyDescriptor(boolean __uninit)
    {
        return checkcastArraycopyDescriptors[__uninit ? 1 : 0];
    }

    public static ForeignCallDescriptor lookupArraycopyDescriptor(JavaKind __kind, boolean __aligned, boolean __disjoint, boolean __uninit, boolean __killAny)
    {
        if (__uninit)
        {
            return uninitObjectArraycopyDescriptors[__aligned ? 1 : 0][__disjoint ? 1 : 0];
        }
        if (__killAny)
        {
            return objectArraycopyDescriptorsKillAny[__aligned ? 1 : 0][__disjoint ? 1 : 0];
        }
        return arraycopyDescriptors[__aligned ? 1 : 0][__disjoint ? 1 : 0].get(__kind);
    }

    @SuppressWarnings({"unchecked"})
    // @def
    private static final EnumMap<JavaKind, ForeignCallDescriptor>[][] arraycopyDescriptors = (EnumMap<JavaKind, ForeignCallDescriptor>[][]) new EnumMap<?, ?>[2][2];

    // @def
    private static final ForeignCallDescriptor[][] uninitObjectArraycopyDescriptors = new ForeignCallDescriptor[2][2];
    // @def
    private static final ForeignCallDescriptor[] checkcastArraycopyDescriptors = new ForeignCallDescriptor[2];
    // @def
    private static ForeignCallDescriptor[][] objectArraycopyDescriptorsKillAny = new ForeignCallDescriptor[2][2];

    static
    {
        // populate the EnumMap instances
        for (int __i = 0; __i < arraycopyDescriptors.length; __i++)
        {
            for (int __j = 0; __j < arraycopyDescriptors[__i].length; __j++)
            {
                arraycopyDescriptors[__i][__j] = new EnumMap<>(JavaKind.class);
            }
        }
    }

    private void registerArraycopyDescriptor(EconomicMap<Long, ForeignCallDescriptor> __descMap, JavaKind __kind, boolean __aligned, boolean __disjoint, boolean __uninit, boolean __killAny, long __routine)
    {
        ForeignCallDescriptor __desc = __descMap.get(__routine);
        if (__desc == null)
        {
            __desc = buildDescriptor(__kind, __aligned, __disjoint, __uninit, __killAny, __routine);
            __descMap.put(__routine, __desc);
        }
        if (__uninit)
        {
            uninitObjectArraycopyDescriptors[__aligned ? 1 : 0][__disjoint ? 1 : 0] = __desc;
        }
        else
        {
            arraycopyDescriptors[__aligned ? 1 : 0][__disjoint ? 1 : 0].put(__kind, __desc);
        }
    }

    private ForeignCallDescriptor buildDescriptor(JavaKind __kind, boolean __aligned, boolean __disjoint, boolean __uninit, boolean __killAny, long __routine)
    {
        String __name = __kind + (__aligned ? "Aligned" : "") + (__disjoint ? "Disjoint" : "") + (__uninit ? "Uninit" : "") + "Arraycopy" + (__killAny ? "KillAny" : "");
        ForeignCallDescriptor __desc = new ForeignCallDescriptor(__name, void.class, Word.class, Word.class, Word.class);
        LocationIdentity __killed = __killAny ? LocationIdentity.any() : NamedLocationIdentity.getArrayLocation(__kind);
        registerForeignCall(__desc, __routine, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, __killed);
        return __desc;
    }

    private void registerCheckcastArraycopyDescriptor(boolean __uninit, long __routine)
    {
        String __name = "Object" + (__uninit ? "Uninit" : "") + "Checkcast";
        // Input:
        // c_rarg0 - source array address
        // c_rarg1 - destination array address
        // c_rarg2 - element count, treated as ssize_t, can be zero
        // c_rarg3 - size_t ckoff (super_check_offset)
        // c_rarg4 - oop ckval (super_klass)
        // Return:
        // 0 = success, n = number of copied elements xor'd with -1.
        ForeignCallDescriptor __desc = new ForeignCallDescriptor(__name, int.class, Word.class, Word.class, Word.class, Word.class, Word.class);
        LocationIdentity __killed = NamedLocationIdentity.any();
        registerForeignCall(__desc, __routine, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, __killed);
        checkcastArraycopyDescriptors[__uninit ? 1 : 0] = __desc;
    }

    private void registerArrayCopy(JavaKind __kind, long __routine, long __alignedRoutine, long __disjointRoutine, long __alignedDisjointRoutine)
    {
        registerArrayCopy(__kind, __routine, __alignedRoutine, __disjointRoutine, __alignedDisjointRoutine, false);
    }

    private void registerArrayCopy(JavaKind __kind, long __routine, long __alignedRoutine, long __disjointRoutine, long __alignedDisjointRoutine, boolean __uninit)
    {
        // Sometimes the same function is used for multiple cases so share them when that's the case
        // but only within the same Kind. For instance short and char are the same copy routines but
        // they kill different memory so they still have to be distinct.
        EconomicMap<Long, ForeignCallDescriptor> __descMap = EconomicMap.create();
        registerArraycopyDescriptor(__descMap, __kind, false, false, __uninit, false, __routine);
        registerArraycopyDescriptor(__descMap, __kind, true, false, __uninit, false, __alignedRoutine);
        registerArraycopyDescriptor(__descMap, __kind, false, true, __uninit, false, __disjointRoutine);
        registerArraycopyDescriptor(__descMap, __kind, true, true, __uninit, false, __alignedDisjointRoutine);

        if (__kind == JavaKind.Object && !__uninit)
        {
            objectArraycopyDescriptorsKillAny[0][0] = buildDescriptor(__kind, false, false, __uninit, true, __routine);
            objectArraycopyDescriptorsKillAny[1][0] = buildDescriptor(__kind, true, false, __uninit, true, __alignedRoutine);
            objectArraycopyDescriptorsKillAny[0][1] = buildDescriptor(__kind, false, true, __uninit, true, __disjointRoutine);
            objectArraycopyDescriptorsKillAny[1][1] = buildDescriptor(__kind, true, true, __uninit, true, __alignedDisjointRoutine);
        }
    }

    public void initialize(HotSpotProviders __providers)
    {
        registerForeignCall(HotSpotHostBackend.DEOPTIMIZATION_HANDLER, HotSpotRuntime.handleDeoptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        registerForeignCall(HotSpotHostBackend.UNCOMMON_TRAP_HANDLER, HotSpotRuntime.uncommonTrapStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        registerForeignCall(HotSpotBackend.IC_MISS_HANDLER, HotSpotRuntime.inlineCacheMissStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);

        registerForeignCall(JAVA_TIME_MILLIS, HotSpotRuntime.javaTimeMillisAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        registerForeignCall(JAVA_TIME_NANOS, HotSpotRuntime.javaTimeNanosAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);

        registerForeignCall(LOAD_AND_CLEAR_EXCEPTION, HotSpotRuntime.loadAndClearExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, LocationIdentity.any());

        registerForeignCall(ExceptionHandlerStub.EXCEPTION_HANDLER_FOR_PC, HotSpotRuntime.exceptionHandlerForPcAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());
        registerForeignCall(UnwindExceptionToCallerStub.EXCEPTION_HANDLER_FOR_RETURN_ADDRESS, HotSpotRuntime.exceptionHandlerForReturnAddressAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());
        registerForeignCall(NewArrayStub.NEW_ARRAY_C, HotSpotRuntime.newArrayAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());
        registerForeignCall(NewInstanceStub.NEW_INSTANCE_C, HotSpotRuntime.newInstanceAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());

        CreateExceptionStub.registerForeignCalls(this);

        link(new NewInstanceStub(__providers, registerStubCall(HotSpotBackend.NEW_INSTANCE, REEXECUTABLE, Transition.SAFEPOINT, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION)));
        link(new NewArrayStub(__providers, registerStubCall(HotSpotBackend.NEW_ARRAY, REEXECUTABLE, Transition.SAFEPOINT, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION)));
        link(new ExceptionHandlerStub(__providers, this.___foreignCalls.get(HotSpotBackend.EXCEPTION_HANDLER)));
        link(new UnwindExceptionToCallerStub(__providers, registerStubCall(HotSpotBackend.UNWIND_EXCEPTION_TO_CALLER, NOT_REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new ArrayStoreExceptionStub(__providers, registerStubCall(RuntimeCalls.CREATE_ARRAY_STORE_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new ClassCastExceptionStub(__providers, registerStubCall(RuntimeCalls.CREATE_CLASS_CAST_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new NullPointerExceptionStub(__providers, registerStubCall(RuntimeCalls.CREATE_NULL_POINTER_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new OutOfBoundsExceptionStub(__providers, registerStubCall(RuntimeCalls.CREATE_OUT_OF_BOUNDS_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));

        linkForeignCall(__providers, IDENTITY_HASHCODE, HotSpotRuntime.identityHashCodeAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, HotSpotReplacementsUtil.MARK_WORD_LOCATION);
        linkForeignCall(__providers, ForeignCallDescriptors.REGISTER_FINALIZER, HotSpotRuntime.registerFinalizerAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, LocationIdentity.any());
        linkForeignCall(__providers, MonitorSnippets.MONITORENTER, HotSpotRuntime.monitorenterAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, LocationIdentity.any());
        linkForeignCall(__providers, MonitorSnippets.MONITOREXIT, HotSpotRuntime.monitorexitAddress, PREPEND_THREAD, Transition.STACK_INSPECTABLE_LEAF, NOT_REEXECUTABLE, LocationIdentity.any());
        linkForeignCall(__providers, HotSpotBackend.NEW_MULTI_ARRAY, HotSpotRuntime.newMultiArrayAddress, PREPEND_THREAD, Transition.SAFEPOINT, REEXECUTABLE, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        linkForeignCall(__providers, NewObjectSnippets.DYNAMIC_NEW_ARRAY, HotSpotRuntime.dynamicNewArrayAddress, PREPEND_THREAD, Transition.SAFEPOINT, REEXECUTABLE);
        linkForeignCall(__providers, NewObjectSnippets.DYNAMIC_NEW_INSTANCE, HotSpotRuntime.dynamicNewInstanceAddress, PREPEND_THREAD, Transition.SAFEPOINT, REEXECUTABLE);
        linkForeignCall(__providers, OSR_MIGRATION_END, HotSpotRuntime.osrMigrationEndAddress, DONT_PREPEND_THREAD, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NO_LOCATIONS);
        linkForeignCall(__providers, WriteBarrierSnippets.G1WBPRECALL, HotSpotRuntime.writeBarrierPreAddress, PREPEND_THREAD, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        linkForeignCall(__providers, WriteBarrierSnippets.G1WBPOSTCALL, HotSpotRuntime.writeBarrierPostAddress, PREPEND_THREAD, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);

        // cannot be a leaf, as VM acquires Thread_lock which requires thread_in_vm state
        linkForeignCall(__providers, ThreadSubstitutions.THREAD_IS_INTERRUPTED, HotSpotRuntime.threadIsInterruptedAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, LocationIdentity.any());

        registerArrayCopy(JavaKind.Byte, HotSpotRuntime.jbyteArraycopy, HotSpotRuntime.jbyteAlignedArraycopy, HotSpotRuntime.jbyteDisjointArraycopy, HotSpotRuntime.jbyteAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Boolean, HotSpotRuntime.jbyteArraycopy, HotSpotRuntime.jbyteAlignedArraycopy, HotSpotRuntime.jbyteDisjointArraycopy, HotSpotRuntime.jbyteAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Char, HotSpotRuntime.jshortArraycopy, HotSpotRuntime.jshortAlignedArraycopy, HotSpotRuntime.jshortDisjointArraycopy, HotSpotRuntime.jshortAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Short, HotSpotRuntime.jshortArraycopy, HotSpotRuntime.jshortAlignedArraycopy, HotSpotRuntime.jshortDisjointArraycopy, HotSpotRuntime.jshortAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Int, HotSpotRuntime.jintArraycopy, HotSpotRuntime.jintAlignedArraycopy, HotSpotRuntime.jintDisjointArraycopy, HotSpotRuntime.jintAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Long, HotSpotRuntime.jlongArraycopy, HotSpotRuntime.jlongAlignedArraycopy, HotSpotRuntime.jlongDisjointArraycopy, HotSpotRuntime.jlongAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Object, HotSpotRuntime.oopArraycopy, HotSpotRuntime.oopAlignedArraycopy, HotSpotRuntime.oopDisjointArraycopy, HotSpotRuntime.oopAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Object, HotSpotRuntime.oopArraycopyUninit, HotSpotRuntime.oopAlignedArraycopyUninit, HotSpotRuntime.oopDisjointArraycopyUninit, HotSpotRuntime.oopAlignedDisjointArraycopyUninit, true);

        registerCheckcastArraycopyDescriptor(true, HotSpotRuntime.checkcastArraycopyUninit);
        registerCheckcastArraycopyDescriptor(false, HotSpotRuntime.checkcastArraycopy);

        registerForeignCall(HotSpotBackend.GENERIC_ARRAYCOPY, HotSpotRuntime.genericArraycopy, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());
        registerForeignCall(HotSpotBackend.UNSAFE_ARRAYCOPY, HotSpotRuntime.unsafeArraycopy, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());

        if (HotSpotRuntime.useSHA256Intrinsics)
        {
            registerForeignCall(HotSpotBackend.SHA2_IMPL_COMPRESS, HotSpotRuntime.sha256ImplCompress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());
        }
        if (HotSpotRuntime.useSHA512Intrinsics)
        {
            registerForeignCall(HotSpotBackend.SHA5_IMPL_COMPRESS, HotSpotRuntime.sha512ImplCompress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());
        }

        if (HotSpotRuntime.useMulAddIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MUL_ADD, HotSpotRuntime.mulAdd, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (HotSpotRuntime.useMultiplyToLenIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MULTIPLY_TO_LEN, HotSpotRuntime.multiplyToLen, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (HotSpotRuntime.useSquareToLenIntrinsic)
        {
            registerForeignCall(HotSpotBackend.SQUARE_TO_LEN, HotSpotRuntime.squareToLen, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (HotSpotRuntime.useMontgomeryMultiplyIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MONTGOMERY_MULTIPLY, HotSpotRuntime.montgomeryMultiply, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (HotSpotRuntime.useMontgomerySquareIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MONTGOMERY_SQUARE, HotSpotRuntime.montgomerySquare, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }

        if (HotSpotRuntime.useAESIntrinsics)
        {
            registerForeignCall(HotSpotBackend.ENCRYPT_BLOCK, HotSpotRuntime.aescryptEncryptBlockStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT_BLOCK, HotSpotRuntime.aescryptDecryptBlockStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT_BLOCK_WITH_ORIGINAL_KEY, HotSpotRuntime.aescryptDecryptBlockStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));

            registerForeignCall(HotSpotBackend.ENCRYPT, HotSpotRuntime.cipherBlockChainingEncryptAESCryptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT, HotSpotRuntime.cipherBlockChainingDecryptAESCryptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT_WITH_ORIGINAL_KEY, HotSpotRuntime.cipherBlockChainingDecryptAESCryptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
        }
    }

    public HotSpotForeignCallLinkage getForeignCall(ForeignCallDescriptor __descriptor)
    {
        return this.___foreignCalls.get(__descriptor);
    }
}
