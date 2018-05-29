package giraaff.hotspot.meta;

import java.util.EnumMap;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import org.graalvm.collections.EconomicMap;
import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.target.Backend;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotForeignCallLinkage.RegisterEffect;
import giraaff.hotspot.HotSpotForeignCallLinkage.Transition;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.HotSpotHostBackend;
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
import giraaff.options.OptionValues;
import giraaff.word.Word;
import giraaff.word.WordTypes;

/**
 * HotSpot implementation of {@link ForeignCallsProvider}.
 */
// @class HotSpotHostForeignCallsProvider
public abstract class HotSpotHostForeignCallsProvider extends HotSpotForeignCallsProviderImpl
{
    public static final ForeignCallDescriptor JAVA_TIME_MILLIS = new ForeignCallDescriptor("javaTimeMillis", long.class);
    public static final ForeignCallDescriptor JAVA_TIME_NANOS = new ForeignCallDescriptor("javaTimeNanos", long.class);

    // @cons
    public HotSpotHostForeignCallsProvider(HotSpotJVMCIRuntimeProvider jvmciRuntime, HotSpotGraalRuntimeProvider runtime, MetaAccessProvider metaAccess, CodeCacheProvider codeCache, WordTypes wordTypes)
    {
        super(jvmciRuntime, runtime, metaAccess, codeCache, wordTypes);
    }

    protected static void link(Stub stub)
    {
        stub.getLinkage().setCompiledStub(stub);
    }

    public static ForeignCallDescriptor lookupCheckcastArraycopyDescriptor(boolean uninit)
    {
        return checkcastArraycopyDescriptors[uninit ? 1 : 0];
    }

    public static ForeignCallDescriptor lookupArraycopyDescriptor(JavaKind kind, boolean aligned, boolean disjoint, boolean uninit, boolean killAny)
    {
        if (uninit)
        {
            return uninitObjectArraycopyDescriptors[aligned ? 1 : 0][disjoint ? 1 : 0];
        }
        if (killAny)
        {
            return objectArraycopyDescriptorsKillAny[aligned ? 1 : 0][disjoint ? 1 : 0];
        }
        return arraycopyDescriptors[aligned ? 1 : 0][disjoint ? 1 : 0].get(kind);
    }

    @SuppressWarnings({"unchecked"}) private static final EnumMap<JavaKind, ForeignCallDescriptor>[][] arraycopyDescriptors = (EnumMap<JavaKind, ForeignCallDescriptor>[][]) new EnumMap<?, ?>[2][2];

    private static final ForeignCallDescriptor[][] uninitObjectArraycopyDescriptors = new ForeignCallDescriptor[2][2];
    private static final ForeignCallDescriptor[] checkcastArraycopyDescriptors = new ForeignCallDescriptor[2];
    private static ForeignCallDescriptor[][] objectArraycopyDescriptorsKillAny = new ForeignCallDescriptor[2][2];

    static
    {
        // populate the EnumMap instances
        for (int i = 0; i < arraycopyDescriptors.length; i++)
        {
            for (int j = 0; j < arraycopyDescriptors[i].length; j++)
            {
                arraycopyDescriptors[i][j] = new EnumMap<>(JavaKind.class);
            }
        }
    }

    private void registerArraycopyDescriptor(EconomicMap<Long, ForeignCallDescriptor> descMap, JavaKind kind, boolean aligned, boolean disjoint, boolean uninit, boolean killAny, long routine)
    {
        ForeignCallDescriptor desc = descMap.get(routine);
        if (desc == null)
        {
            desc = buildDescriptor(kind, aligned, disjoint, uninit, killAny, routine);
            descMap.put(routine, desc);
        }
        if (uninit)
        {
            uninitObjectArraycopyDescriptors[aligned ? 1 : 0][disjoint ? 1 : 0] = desc;
        }
        else
        {
            arraycopyDescriptors[aligned ? 1 : 0][disjoint ? 1 : 0].put(kind, desc);
        }
    }

    private ForeignCallDescriptor buildDescriptor(JavaKind kind, boolean aligned, boolean disjoint, boolean uninit, boolean killAny, long routine)
    {
        String name = kind + (aligned ? "Aligned" : "") + (disjoint ? "Disjoint" : "") + (uninit ? "Uninit" : "") + "Arraycopy" + (killAny ? "KillAny" : "");
        ForeignCallDescriptor desc = new ForeignCallDescriptor(name, void.class, Word.class, Word.class, Word.class);
        LocationIdentity killed = killAny ? LocationIdentity.any() : NamedLocationIdentity.getArrayLocation(kind);
        registerForeignCall(desc, routine, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, killed);
        return desc;
    }

    private void registerCheckcastArraycopyDescriptor(boolean uninit, long routine)
    {
        String name = "Object" + (uninit ? "Uninit" : "") + "Checkcast";
        // Input:
        // c_rarg0 - source array address
        // c_rarg1 - destination array address
        // c_rarg2 - element count, treated as ssize_t, can be zero
        // c_rarg3 - size_t ckoff (super_check_offset)
        // c_rarg4 - oop ckval (super_klass)
        // Return:
        // 0 = success, n = number of copied elements xor'd with -1.
        ForeignCallDescriptor desc = new ForeignCallDescriptor(name, int.class, Word.class, Word.class, Word.class, Word.class, Word.class);
        LocationIdentity killed = NamedLocationIdentity.any();
        registerForeignCall(desc, routine, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, killed);
        checkcastArraycopyDescriptors[uninit ? 1 : 0] = desc;
    }

    private void registerArrayCopy(JavaKind kind, long routine, long alignedRoutine, long disjointRoutine, long alignedDisjointRoutine)
    {
        registerArrayCopy(kind, routine, alignedRoutine, disjointRoutine, alignedDisjointRoutine, false);
    }

    private void registerArrayCopy(JavaKind kind, long routine, long alignedRoutine, long disjointRoutine, long alignedDisjointRoutine, boolean uninit)
    {
        /*
         * Sometimes the same function is used for multiple cases so share them when that's the case
         * but only within the same Kind. For instance short and char are the same copy routines but
         * they kill different memory so they still have to be distinct.
         */
        EconomicMap<Long, ForeignCallDescriptor> descMap = EconomicMap.create();
        registerArraycopyDescriptor(descMap, kind, false, false, uninit, false, routine);
        registerArraycopyDescriptor(descMap, kind, true, false, uninit, false, alignedRoutine);
        registerArraycopyDescriptor(descMap, kind, false, true, uninit, false, disjointRoutine);
        registerArraycopyDescriptor(descMap, kind, true, true, uninit, false, alignedDisjointRoutine);

        if (kind == JavaKind.Object && !uninit)
        {
            objectArraycopyDescriptorsKillAny[0][0] = buildDescriptor(kind, false, false, uninit, true, routine);
            objectArraycopyDescriptorsKillAny[1][0] = buildDescriptor(kind, true, false, uninit, true, alignedRoutine);
            objectArraycopyDescriptorsKillAny[0][1] = buildDescriptor(kind, false, true, uninit, true, disjointRoutine);
            objectArraycopyDescriptorsKillAny[1][1] = buildDescriptor(kind, true, true, uninit, true, alignedDisjointRoutine);
        }
    }

    public void initialize(HotSpotProviders providers, OptionValues options)
    {
        registerForeignCall(HotSpotHostBackend.DEOPTIMIZATION_HANDLER, GraalHotSpotVMConfig.handleDeoptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        registerForeignCall(HotSpotHostBackend.UNCOMMON_TRAP_HANDLER, GraalHotSpotVMConfig.uncommonTrapStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        registerForeignCall(HotSpotBackend.IC_MISS_HANDLER, GraalHotSpotVMConfig.inlineCacheMissStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);

        registerForeignCall(JAVA_TIME_MILLIS, GraalHotSpotVMConfig.javaTimeMillisAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        registerForeignCall(JAVA_TIME_NANOS, GraalHotSpotVMConfig.javaTimeNanosAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);

        registerForeignCall(Backend.ARITHMETIC_FREM, GraalHotSpotVMConfig.fremAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF, REEXECUTABLE, NO_LOCATIONS);
        registerForeignCall(Backend.ARITHMETIC_DREM, GraalHotSpotVMConfig.dremAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF, REEXECUTABLE, NO_LOCATIONS);

        registerForeignCall(LOAD_AND_CLEAR_EXCEPTION, GraalHotSpotVMConfig.loadAndClearExceptionAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, LocationIdentity.any());

        registerForeignCall(ExceptionHandlerStub.EXCEPTION_HANDLER_FOR_PC, GraalHotSpotVMConfig.exceptionHandlerForPcAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());
        registerForeignCall(UnwindExceptionToCallerStub.EXCEPTION_HANDLER_FOR_RETURN_ADDRESS, GraalHotSpotVMConfig.exceptionHandlerForReturnAddressAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());
        registerForeignCall(NewArrayStub.NEW_ARRAY_C, GraalHotSpotVMConfig.newArrayAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());
        registerForeignCall(NewInstanceStub.NEW_INSTANCE_C, GraalHotSpotVMConfig.newInstanceAddress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.SAFEPOINT, REEXECUTABLE, LocationIdentity.any());

        CreateExceptionStub.registerForeignCalls(this);

        link(new NewInstanceStub(options, providers, registerStubCall(HotSpotBackend.NEW_INSTANCE, REEXECUTABLE, Transition.SAFEPOINT, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION)));
        link(new NewArrayStub(options, providers, registerStubCall(HotSpotBackend.NEW_ARRAY, REEXECUTABLE, Transition.SAFEPOINT, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION)));
        link(new ExceptionHandlerStub(options, providers, foreignCalls.get(HotSpotBackend.EXCEPTION_HANDLER)));
        link(new UnwindExceptionToCallerStub(options, providers, registerStubCall(HotSpotBackend.UNWIND_EXCEPTION_TO_CALLER, NOT_REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new ArrayStoreExceptionStub(options, providers, registerStubCall(RuntimeCalls.CREATE_ARRAY_STORE_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new ClassCastExceptionStub(options, providers, registerStubCall(RuntimeCalls.CREATE_CLASS_CAST_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new NullPointerExceptionStub(options, providers, registerStubCall(RuntimeCalls.CREATE_NULL_POINTER_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));
        link(new OutOfBoundsExceptionStub(options, providers, registerStubCall(RuntimeCalls.CREATE_OUT_OF_BOUNDS_EXCEPTION, REEXECUTABLE, Transition.SAFEPOINT, LocationIdentity.any())));

        linkForeignCall(options, providers, IDENTITY_HASHCODE, GraalHotSpotVMConfig.identityHashCodeAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, HotSpotReplacementsUtil.MARK_WORD_LOCATION);
        linkForeignCall(options, providers, ForeignCallDescriptors.REGISTER_FINALIZER, GraalHotSpotVMConfig.registerFinalizerAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, LocationIdentity.any());
        linkForeignCall(options, providers, MonitorSnippets.MONITORENTER, GraalHotSpotVMConfig.monitorenterAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, LocationIdentity.any());
        linkForeignCall(options, providers, MonitorSnippets.MONITOREXIT, GraalHotSpotVMConfig.monitorexitAddress, PREPEND_THREAD, Transition.STACK_INSPECTABLE_LEAF, NOT_REEXECUTABLE, LocationIdentity.any());
        linkForeignCall(options, providers, HotSpotBackend.NEW_MULTI_ARRAY, GraalHotSpotVMConfig.newMultiArrayAddress, PREPEND_THREAD, Transition.SAFEPOINT, REEXECUTABLE, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        linkForeignCall(options, providers, NewObjectSnippets.DYNAMIC_NEW_ARRAY, GraalHotSpotVMConfig.dynamicNewArrayAddress, PREPEND_THREAD, Transition.SAFEPOINT, REEXECUTABLE);
        linkForeignCall(options, providers, NewObjectSnippets.DYNAMIC_NEW_INSTANCE, GraalHotSpotVMConfig.dynamicNewInstanceAddress, PREPEND_THREAD, Transition.SAFEPOINT, REEXECUTABLE);
        linkForeignCall(options, providers, OSR_MIGRATION_END, GraalHotSpotVMConfig.osrMigrationEndAddress, DONT_PREPEND_THREAD, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NO_LOCATIONS);
        linkForeignCall(options, providers, WriteBarrierSnippets.G1WBPRECALL, GraalHotSpotVMConfig.writeBarrierPreAddress, PREPEND_THREAD, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);
        linkForeignCall(options, providers, WriteBarrierSnippets.G1WBPOSTCALL, GraalHotSpotVMConfig.writeBarrierPostAddress, PREPEND_THREAD, Transition.LEAF_NOFP, REEXECUTABLE, NO_LOCATIONS);

        // cannot be a leaf, as VM acquires Thread_lock which requires thread_in_vm state
        linkForeignCall(options, providers, ThreadSubstitutions.THREAD_IS_INTERRUPTED, GraalHotSpotVMConfig.threadIsInterruptedAddress, PREPEND_THREAD, Transition.SAFEPOINT, NOT_REEXECUTABLE, LocationIdentity.any());

        registerArrayCopy(JavaKind.Byte, GraalHotSpotVMConfig.jbyteArraycopy, GraalHotSpotVMConfig.jbyteAlignedArraycopy, GraalHotSpotVMConfig.jbyteDisjointArraycopy, GraalHotSpotVMConfig.jbyteAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Boolean, GraalHotSpotVMConfig.jbyteArraycopy, GraalHotSpotVMConfig.jbyteAlignedArraycopy, GraalHotSpotVMConfig.jbyteDisjointArraycopy, GraalHotSpotVMConfig.jbyteAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Char, GraalHotSpotVMConfig.jshortArraycopy, GraalHotSpotVMConfig.jshortAlignedArraycopy, GraalHotSpotVMConfig.jshortDisjointArraycopy, GraalHotSpotVMConfig.jshortAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Short, GraalHotSpotVMConfig.jshortArraycopy, GraalHotSpotVMConfig.jshortAlignedArraycopy, GraalHotSpotVMConfig.jshortDisjointArraycopy, GraalHotSpotVMConfig.jshortAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Int, GraalHotSpotVMConfig.jintArraycopy, GraalHotSpotVMConfig.jintAlignedArraycopy, GraalHotSpotVMConfig.jintDisjointArraycopy, GraalHotSpotVMConfig.jintAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Float, GraalHotSpotVMConfig.jintArraycopy, GraalHotSpotVMConfig.jintAlignedArraycopy, GraalHotSpotVMConfig.jintDisjointArraycopy, GraalHotSpotVMConfig.jintAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Long, GraalHotSpotVMConfig.jlongArraycopy, GraalHotSpotVMConfig.jlongAlignedArraycopy, GraalHotSpotVMConfig.jlongDisjointArraycopy, GraalHotSpotVMConfig.jlongAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Double, GraalHotSpotVMConfig.jlongArraycopy, GraalHotSpotVMConfig.jlongAlignedArraycopy, GraalHotSpotVMConfig.jlongDisjointArraycopy, GraalHotSpotVMConfig.jlongAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Object, GraalHotSpotVMConfig.oopArraycopy, GraalHotSpotVMConfig.oopAlignedArraycopy, GraalHotSpotVMConfig.oopDisjointArraycopy, GraalHotSpotVMConfig.oopAlignedDisjointArraycopy);
        registerArrayCopy(JavaKind.Object, GraalHotSpotVMConfig.oopArraycopyUninit, GraalHotSpotVMConfig.oopAlignedArraycopyUninit, GraalHotSpotVMConfig.oopDisjointArraycopyUninit, GraalHotSpotVMConfig.oopAlignedDisjointArraycopyUninit, true);

        registerCheckcastArraycopyDescriptor(true, GraalHotSpotVMConfig.checkcastArraycopyUninit);
        registerCheckcastArraycopyDescriptor(false, GraalHotSpotVMConfig.checkcastArraycopy);

        registerForeignCall(HotSpotBackend.GENERIC_ARRAYCOPY, GraalHotSpotVMConfig.genericArraycopy, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());
        registerForeignCall(HotSpotBackend.UNSAFE_ARRAYCOPY, GraalHotSpotVMConfig.unsafeArraycopy, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());

        if (GraalHotSpotVMConfig.useSHA256Intrinsics)
        {
            registerForeignCall(HotSpotBackend.SHA2_IMPL_COMPRESS, GraalHotSpotVMConfig.sha256ImplCompress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());
        }
        if (GraalHotSpotVMConfig.useSHA512Intrinsics)
        {
            registerForeignCall(HotSpotBackend.SHA5_IMPL_COMPRESS, GraalHotSpotVMConfig.sha512ImplCompress, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.any());
        }

        if (GraalHotSpotVMConfig.useMulAddIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MUL_ADD, GraalHotSpotVMConfig.mulAdd, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (GraalHotSpotVMConfig.useMultiplyToLenIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MULTIPLY_TO_LEN, GraalHotSpotVMConfig.multiplyToLen, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (GraalHotSpotVMConfig.useSquareToLenIntrinsic)
        {
            registerForeignCall(HotSpotBackend.SQUARE_TO_LEN, GraalHotSpotVMConfig.squareToLen, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (GraalHotSpotVMConfig.useMontgomeryMultiplyIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MONTGOMERY_MULTIPLY, GraalHotSpotVMConfig.montgomeryMultiply, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }
        if (GraalHotSpotVMConfig.useMontgomerySquareIntrinsic)
        {
            registerForeignCall(HotSpotBackend.MONTGOMERY_SQUARE, GraalHotSpotVMConfig.montgomerySquare, HotSpotCallingConventionType.NativeCall, RegisterEffect.DESTROYS_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Int));
        }

        if (GraalHotSpotVMConfig.useAESIntrinsics)
        {
            registerForeignCall(HotSpotBackend.ENCRYPT_BLOCK, GraalHotSpotVMConfig.aescryptEncryptBlockStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT_BLOCK, GraalHotSpotVMConfig.aescryptDecryptBlockStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT_BLOCK_WITH_ORIGINAL_KEY, GraalHotSpotVMConfig.aescryptDecryptBlockStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));

            registerForeignCall(HotSpotBackend.ENCRYPT, GraalHotSpotVMConfig.cipherBlockChainingEncryptAESCryptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT, GraalHotSpotVMConfig.cipherBlockChainingDecryptAESCryptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
            registerForeignCall(HotSpotBackend.DECRYPT_WITH_ORIGINAL_KEY, GraalHotSpotVMConfig.cipherBlockChainingDecryptAESCryptStub, HotSpotCallingConventionType.NativeCall, RegisterEffect.PRESERVES_REGISTERS, Transition.LEAF_NOFP, NOT_REEXECUTABLE, NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
        }
    }

    public HotSpotForeignCallLinkage getForeignCall(ForeignCallDescriptor descriptor)
    {
        return foreignCalls.get(descriptor);
    }
}
