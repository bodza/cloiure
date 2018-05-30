package giraaff.hotspot;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotVMConfigAccess;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.CompressEncoding;

/**
 * Native configuration details.
 */
// @class HotSpotRuntime
public final class HotSpotRuntime
{
    // @cons
    private HotSpotRuntime()
    {
        super();
    }

    public static final HotSpotJVMCIRuntime JVMCI = HotSpotJVMCIRuntime.runtime();

    private static final HotSpotVMConfigAccess c = new HotSpotVMConfigAccess(JVMCI.getConfigStore());

    public static final boolean
        inline                         = c.getFlag("Inline",                         Boolean.class),
        useFastLocking                 = c.getFlag("JVMCIUseFastLocking",            Boolean.class),
        foldStableValues               = c.getFlag("FoldStableValues",               Boolean.class),
        useTLAB                        = c.getFlag("UseTLAB",                        Boolean.class),
        useBiasedLocking               = c.getFlag("UseBiasedLocking",               Boolean.class),
        usePopCountInstruction         = c.getFlag("UsePopCountInstruction",         Boolean.class),
        useAESIntrinsics               = c.getFlag("UseAESIntrinsics",               Boolean.class),
        useCRC32Intrinsics             = c.getFlag("UseCRC32Intrinsics",             Boolean.class),
        useCRC32CIntrinsics            = c.getFlag("UseCRC32CIntrinsics",            Boolean.class),
        threadLocalHandshakes          = c.getFlag("ThreadLocalHandshakes",          Boolean.class),
        useSHA256Intrinsics            = c.getFlag("UseSHA256Intrinsics",            Boolean.class),
        useSHA512Intrinsics            = c.getFlag("UseSHA512Intrinsics",            Boolean.class),
        useMulAddIntrinsic             = c.getFlag("UseMulAddIntrinsic",             Boolean.class),
        useMultiplyToLenIntrinsic      = c.getFlag("UseMultiplyToLenIntrinsic",      Boolean.class),
        useSquareToLenIntrinsic        = c.getFlag("UseSquareToLenIntrinsic",        Boolean.class),
        useMontgomeryMultiplyIntrinsic = c.getFlag("UseMontgomeryMultiplyIntrinsic", Boolean.class),
        useMontgomerySquareIntrinsic   = c.getFlag("UseMontgomerySquareIntrinsic",   Boolean.class),
        useG1GC                        = c.getFlag("UseG1GC",                        Boolean.class),
        useDeferredInitBarriers        = c.getFlag("ReduceInitialCardMarks",         Boolean.class);

    public static final int
        allocatePrefetchStyle         = c.getFlag("AllocatePrefetchStyle",         Integer.class),
        allocatePrefetchInstr         = c.getFlag("AllocatePrefetchInstr",         Integer.class),
        allocatePrefetchLines         = c.getFlag("AllocatePrefetchLines",         Integer.class),
        allocateInstancePrefetchLines = c.getFlag("AllocateInstancePrefetchLines", Integer.class),
        allocatePrefetchStepSize      = c.getFlag("AllocatePrefetchStepSize",      Integer.class),
        allocatePrefetchDistance      = c.getFlag("AllocatePrefetchDistance",      Integer.class),
        codeEntryAlignment            = c.getFlag("CodeEntryAlignment",            Integer.class),
        objectAlignment               = c.getFlag("ObjectAlignmentInBytes",        Integer.class),
        heapWordSize                  = c.getConstant("HeapWordSize",              Integer.class);

    // Compressed Oops related values.
    public static final boolean
        useCompressedOops          = c.getFlag("UseCompressedOops",          Boolean.class),
        useCompressedClassPointers = c.getFlag("UseCompressedClassPointers", Boolean.class);

    public static final long
        narrowOopBase   = c.getFieldValue("CompilerToVM::Data::Universe_narrow_oop_base",   Long.class, "address"),
        narrowKlassBase = c.getFieldValue("CompilerToVM::Data::Universe_narrow_klass_base", Long.class, "address");

    public static final int
        narrowOopShift   = c.getFieldValue("CompilerToVM::Data::Universe_narrow_oop_shift",   Integer.class, "int"),
        narrowKlassShift = c.getFieldValue("CompilerToVM::Data::Universe_narrow_klass_shift", Integer.class, "int"),
        narrowKlassSize  = c.getFieldValue("CompilerToVM::Data::sizeof_narrowKlass",          Integer.class, "int"),
        arrayOopDescSize = c.getFieldValue("CompilerToVM::Data::sizeof_arrayOopDesc",         Integer.class, "int"),
        vmPageSize       = c.getFieldValue("CompilerToVM::Data::vm_page_size",                Integer.class, "int");

    public static final CompressEncoding
        oopEncoding   = new CompressEncoding(narrowOopBase, narrowOopShift),
        klassEncoding = new CompressEncoding(narrowKlassBase, narrowKlassShift);

    public static final boolean useStackBanging = c.getFlag("UseStackBanging", Boolean.class);
    public static final int stackShadowPages = c.getFlag("StackShadowPages", Integer.class);
    public static final int stackBias = c.getConstant("STACK_BIAS", Integer.class);

    public static final int
        markOffset = c.getFieldOffset("oopDesc::_mark",            Integer.class, "markOop"),
        hubOffset  = c.getFieldOffset("oopDesc::_metadata._klass", Integer.class, "Klass*");

    public static final int
        prototypeMarkWordOffset   = c.getFieldOffset("Klass::_prototype_header",      Integer.class, "markOop"),
        superCheckOffsetOffset    = c.getFieldOffset("Klass::_super_check_offset",    Integer.class, "juint"),
        secondarySuperCacheOffset = c.getFieldOffset("Klass::_secondary_super_cache", Integer.class, "Klass*"),
        secondarySupersOffset     = c.getFieldOffset("Klass::_secondary_supers",      Integer.class, "Array<Klass*>*"),
        classMirrorOffset         = c.getFieldOffset("Klass::_java_mirror",           Integer.class, "OopHandle"),
        klassSuperKlassOffset     = c.getFieldOffset("Klass::_super",                 Integer.class, "Klass*"),
        klassModifierFlagsOffset  = c.getFieldOffset("Klass::_modifier_flags",        Integer.class, "jint"),
        klassAccessFlagsOffset    = c.getFieldOffset("Klass::_access_flags",          Integer.class, "AccessFlags"),
        klassLayoutHelperOffset   = c.getFieldOffset("Klass::_layout_helper",         Integer.class, "jint");

    public static final int
        klassLayoutHelperNeutralValue    = c.getConstant("Klass::_lh_neutral_value",           Integer.class),
        layoutHelperLog2ElementSizeShift = c.getConstant("Klass::_lh_log2_element_size_shift", Integer.class),
        layoutHelperLog2ElementSizeMask  = c.getConstant("Klass::_lh_log2_element_size_mask",  Integer.class),
        layoutHelperElementTypeShift     = c.getConstant("Klass::_lh_element_type_shift",      Integer.class),
        layoutHelperElementTypeMask      = c.getConstant("Klass::_lh_element_type_mask",       Integer.class),
        layoutHelperHeaderSizeShift      = c.getConstant("Klass::_lh_header_size_shift",       Integer.class),
        layoutHelperHeaderSizeMask       = c.getConstant("Klass::_lh_header_size_mask",        Integer.class);

    public static final int instanceKlassInitStateOffset = c.getFieldOffset("InstanceKlass::_init_state", Integer.class, "u1");
    public static final int instanceKlassConstantsOffset = c.getFieldOffset("InstanceKlass::_constants", Integer.class, "ConstantPool*");
    public static final int instanceKlassStateFullyInitialized = c.getConstant("InstanceKlass::fully_initialized", Integer.class);

    /**
     * The offset of the array length word in an array object's header.
     */
    public static final int arrayLengthOffset = useCompressedClassPointers ? hubOffset + narrowKlassSize : arrayOopDescSize;

    public static final int
        metaspaceArrayBaseOffset        = c.getFieldOffset("Array<Klass*>::_data[0]",       Integer.class, "Klass*"),
        metaspaceArrayLengthOffset      = c.getFieldOffset("Array<Klass*>::_length",        Integer.class, "int"),
        arrayClassElementOffset         = c.getFieldOffset("ObjArrayKlass::_element_klass", Integer.class, "Klass*"),
        arrayKlassComponentMirrorOffset = c.getFieldOffset("ArrayKlass::_component_mirror", Integer.class, "oop");

    public static final int jvmAccWrittenFlags = c.getConstant("JVM_ACC_WRITTEN_FLAGS", Integer.class);

    public static final int
        threadTlabOffset                 = c.getFieldOffset("Thread::_tlab",                        Integer.class, "ThreadLocalAllocBuffer"),
        javaThreadAnchorOffset           = c.getFieldOffset("JavaThread::_anchor",                  Integer.class, "JavaFrameAnchor"),
        threadObjectOffset               = c.getFieldOffset("JavaThread::_threadObj",               Integer.class, "oop"),
        osThreadOffset                   = c.getFieldOffset("JavaThread::_osthread",                Integer.class, "OSThread*"),
        threadIsMethodHandleReturnOffset = c.getFieldOffset("JavaThread::_is_method_handle_return", Integer.class, "int"),
        objectResultOffset               = c.getFieldOffset("JavaThread::_vm_result",               Integer.class, "oop");

    /**
     * This field is used to pass exception objects into and out of the runtime system during exception handling for compiled code.
     */
    public static final int
        threadExceptionOopOffset       = c.getFieldOffset("JavaThread::_exception_oop",              Integer.class, "oop"),
        threadExceptionPcOffset        = c.getFieldOffset("JavaThread::_exception_pc",               Integer.class, "address"),
        pendingExceptionOffset         = c.getFieldOffset("ThreadShadow::_pending_exception",        Integer.class, "oop"),
        pendingDeoptimizationOffset    = c.getFieldOffset("JavaThread::_pending_deoptimization",     Integer.class, "int"),
        pendingFailedSpeculationOffset = c.getFieldOffset("JavaThread::_pending_failed_speculation", Integer.class, "oop"),
        osThreadInterruptedOffset      = c.getFieldOffset("OSThread::_interrupted",                  Integer.class, "jint");

    public static final int
        threadLastJavaSpOffset = javaThreadAnchorOffset + c.getFieldOffset("JavaFrameAnchor::_last_Java_sp", Integer.class, "intptr_t*"),
        threadLastJavaPcOffset = javaThreadAnchorOffset + c.getFieldOffset("JavaFrameAnchor::_last_Java_pc", Integer.class, "address"),
        threadLastJavaFpOffset = javaThreadAnchorOffset + c.getFieldOffset("JavaFrameAnchor::_last_Java_fp", Integer.class, "intptr_t*");

    public static final int
        frameInterpreterFrameSenderSpOffset = c.getConstant("frame::interpreter_frame_sender_sp_offset", Integer.class),
        frameInterpreterFrameLastSpOffset   = c.getConstant("frame::interpreter_frame_last_sp_offset",   Integer.class);

    public static final long
        markOopDescHashShift       = c.getConstant("markOopDesc::hash_shift",         Long.class),
        markOopDescHashMask        = c.getConstant("markOopDesc::hash_mask",          Long.class),
        markOopDescHashMaskInPlace = c.getConstant("markOopDesc::hash_mask_in_place", Long.class);

    /**
     * Mask for a biasable, locked or unlocked mark word.
     *
     * <pre>
     * +----------------------------------+-+-+
     * |                                 1|1|1|
     * +----------------------------------+-+-+
     * </pre>
     */

    /**
     * Pattern for a biasable, unlocked mark word.
     *
     * <pre>
     * +----------------------------------+-+-+
     * |                                 1|0|1|
     * +----------------------------------+-+-+
     * </pre>
     */

    public static final int
        biasedLockMaskInPlace = c.getConstant("markOopDesc::biased_lock_mask_in_place", Integer.class),
        biasedLockPattern     = c.getConstant("markOopDesc::biased_lock_pattern",       Integer.class),
        ageMaskInPlace        = c.getConstant("markOopDesc::age_mask_in_place",         Integer.class),
        epochMaskInPlace      = c.getConstant("markOopDesc::epoch_mask_in_place",       Integer.class),
        unlockedMask          = c.getConstant("markOopDesc::unlocked_value",            Integer.class),
        monitorMask           = c.getConstant("markOopDesc::monitor_value",             Integer.class, -1);

    // this field has no type in vmStructs.cpp
    public static final int
        objectMonitorOwnerOffset      = c.getFieldOffset("ObjectMonitor::_owner",      Integer.class, null,            -1),
        objectMonitorRecursionsOffset = c.getFieldOffset("ObjectMonitor::_recursions", Integer.class, "intptr_t",      -1),
        objectMonitorCxqOffset        = c.getFieldOffset("ObjectMonitor::_cxq",        Integer.class, "ObjectWaiter*", -1),
        objectMonitorEntryListOffset  = c.getFieldOffset("ObjectMonitor::_EntryList",  Integer.class, "ObjectWaiter*", -1);

    public static final int
        markWordNoHashInPlace = c.getConstant("markOopDesc::no_hash_in_place", Integer.class),
        markWordNoLockInPlace = c.getConstant("markOopDesc::no_lock_in_place", Integer.class);

    public static final long
        arrayPrototypeMarkWord = markWordNoHashInPlace | markWordNoLockInPlace,
        tlabIntArrayMarkWord   = (arrayPrototypeMarkWord & (~markOopDescHashMaskInPlace)) | ((0x2 & markOopDescHashMask) << markOopDescHashShift);

    /**
     * Mark word right shift to get identity hash code.
     */
    public static final int identityHashCodeShift = c.getConstant("markOopDesc::hash_shift", Integer.class);

    /**
     * Identity hash code value when uninitialized.
     */
    public static final int uninitializedIdentityHashCodeValue = c.getConstant("markOopDesc::no_hash", Integer.class);

    public static final int methodCompiledEntryOffset = c.getFieldOffset("Method::_from_compiled_entry", Integer.class, "address");

    public static final int compilationLevelFullOptimization = c.getConstant("CompLevel_full_optimization", Integer.class);

    public static final int constantPoolSize = c.getFieldValue("CompilerToVM::Data::sizeof_ConstantPool", Integer.class, "int");
    public static final int constantPoolLengthOffset = c.getFieldOffset("ConstantPool::_length", Integer.class, "int");

    /**
     * Bit pattern that represents a non-oop. Neither the high bits nor the low bits of this value
     * are allowed to look like (respectively) the high or low bits of a real oop.
     */
    public static final long nonOopBits = c.getFieldValue("CompilerToVM::Data::Universe_non_oop_bits", Long.class, "void*");

    public static final int logOfHeapRegionGrainBytes = c.getFieldValue("HeapRegion::LogOfHRGrainBytes", Integer.class, "int");

    public static final long cardTableAddress = c.getFieldValue("CompilerToVM::Data::cardtable_start_address", Long.class, "jbyte*");
    public static final int cardTableShift = c.getFieldValue("CompilerToVM::Data::cardtable_shift", Integer.class, "int");

    public static final long safepointPollingAddress = c.getFieldValue("os::_polling_page", Long.class, "address");

    // G1 Collector Related Values.
    public static final byte
        dirtyCardValue   = c.getConstant("CardTableModRefBS::dirty_card",         Byte.class),
        g1YoungCardValue = c.getConstant("G1SATBCardTableModRefBS::g1_young_gen", Byte.class);

    public static final int
        javaThreadDirtyCardQueueOffset = c.getFieldOffset("JavaThread::_dirty_card_queue", Integer.class, "DirtyCardQueue"),
        javaThreadSatbMarkQueueOffset  = c.getFieldOffset("JavaThread::_satb_mark_queue",  Integer.class);

    public static final int
        g1CardQueueIndexOffset   = javaThreadDirtyCardQueueOffset + c.getConstant("dirtyCardQueueIndexOffset",  Integer.class),
        g1CardQueueBufferOffset  = javaThreadDirtyCardQueueOffset + c.getConstant("dirtyCardQueueBufferOffset", Integer.class),
        g1SATBQueueMarkingOffset = javaThreadSatbMarkQueueOffset  + c.getConstant("satbMarkQueueActiveOffset",  Integer.class),
        g1SATBQueueIndexOffset   = javaThreadSatbMarkQueueOffset  + c.getConstant("satbMarkQueueIndexOffset",   Integer.class),
        g1SATBQueueBufferOffset  = javaThreadSatbMarkQueueOffset  + c.getConstant("satbMarkQueueBufferOffset",  Integer.class);

    public static final int
        klassOffset      = c.getFieldValue("java_lang_Class::_klass_offset",       Integer.class, "int"),
        arrayKlassOffset = c.getFieldValue("java_lang_Class::_array_klass_offset", Integer.class, "int");

    public static final int lockDisplacedMarkOffset = c.getFieldOffset("BasicLock::_displaced_header", Integer.class, "markOop");

    public static final int
        threadPollingPageOffset    = c.getFieldOffset("Thread::_polling_page",    Integer.class, "address"),
        threadAllocatedBytesOffset = c.getFieldOffset("Thread::_allocated_bytes", Integer.class, "jlong");

    public static final int tlabRefillWasteIncrement = c.getFlag("TLABWasteIncrement", Integer.class);

    public static final int
        tlabSlowAllocationsOffset  = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_slow_allocations",   Integer.class, "unsigned"),
        tlabFastRefillWasteOffset  = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_fast_refill_waste",  Integer.class, "unsigned"),
        tlabNumberOfRefillsOffset  = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_number_of_refills",  Integer.class, "unsigned"),
        tlabRefillWasteLimitOffset = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_refill_waste_limit", Integer.class, "size_t");

    public static final int
        threadTlabSizeOffset  = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_desired_size", Integer.class, "size_t"),
        threadTlabStartOffset = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_start",        Integer.class, "HeapWord*"),
        threadTlabEndOffset   = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_end",          Integer.class, "HeapWord*"),
        threadTlabTopOffset   = threadTlabOffset + c.getFieldOffset("ThreadLocalAllocBuffer::_top",          Integer.class, "HeapWord*");

    public static final int tlabAlignmentReserve = c.getFieldValue("CompilerToVM::Data::ThreadLocalAllocBuffer_alignment_reserve", Integer.class, "size_t");

    public static final boolean tlabStats = c.getFlag("TLABStats", Boolean.class);

    public static final boolean inlineContiguousAllocationSupported = c.getFieldValue("CompilerToVM::Data::_supports_inline_contig_alloc", Boolean.class);

    public static final long
        heapEndAddress      = c.getFieldValue("CompilerToVM::Data::_heap_end_addr",                         Long.class, "HeapWord**"),
        heapTopAddress      = c.getFieldValue("CompilerToVM::Data::_heap_top_addr",                         Long.class, "HeapWord* volatile*"),
        inlineCacheMissStub = c.getFieldValue("CompilerToVM::Data::SharedRuntime_ic_miss_stub",             Long.class, "address"),
        handleDeoptStub     = c.getFieldValue("CompilerToVM::Data::SharedRuntime_deopt_blob_unpack",        Long.class, "address"),
        uncommonTrapStub    = c.getFieldValue("CompilerToVM::Data::SharedRuntime_deopt_blob_uncommon_trap", Long.class, "address");

    public static final long
        codeCacheLowBound  = c.getFieldValue("CodeCache::_low_bound",  Long.class, "address"),
        codeCacheHighBound = c.getFieldValue("CodeCache::_high_bound", Long.class, "address");

    public static final long
        aescryptEncryptBlockStub               = c.getFieldValue("StubRoutines::_aescrypt_encryptBlock",               Long.class, "address"),
        aescryptDecryptBlockStub               = c.getFieldValue("StubRoutines::_aescrypt_decryptBlock",               Long.class, "address"),
        cipherBlockChainingEncryptAESCryptStub = c.getFieldValue("StubRoutines::_cipherBlockChaining_encryptAESCrypt", Long.class, "address"),
        cipherBlockChainingDecryptAESCryptStub = c.getFieldValue("StubRoutines::_cipherBlockChaining_decryptAESCrypt", Long.class, "address"),
        crcTableAddress                        = c.getFieldValue("StubRoutines::_crc_table_adr",                       Long.class, "address"),
        updateBytesCRC32Stub                   = c.getFieldValue("StubRoutines::_updateBytesCRC32",                    Long.class, "address"),
        updateBytesCRC32C                      = c.getFieldValue("StubRoutines::_updateBytesCRC32C",                   Long.class, "address"),
        sha256ImplCompress                     = c.getFieldValue("StubRoutines::_sha256_implCompress",                 Long.class, "address"),
        sha512ImplCompress                     = c.getFieldValue("StubRoutines::_sha512_implCompress",                 Long.class, "address"),
        multiplyToLen                          = c.getFieldValue("StubRoutines::_multiplyToLen",                       Long.class, "address"),
        squareToLen                            = c.getFieldValue("StubRoutines::_squareToLen",                         Long.class, "address"),
        mulAdd                                 = c.getFieldValue("StubRoutines::_mulAdd",                              Long.class, "address"),
        montgomeryMultiply                     = c.getFieldValue("StubRoutines::_montgomeryMultiply",                  Long.class, "address"),
        montgomerySquare                       = c.getFieldValue("StubRoutines::_montgomerySquare",                    Long.class, "address");

    public static final long
        jbyteArraycopy     = c.getFieldValue("StubRoutines::_jbyte_arraycopy",      Long.class, "address"),
        jshortArraycopy    = c.getFieldValue("StubRoutines::_jshort_arraycopy",     Long.class, "address"),
        jintArraycopy      = c.getFieldValue("StubRoutines::_jint_arraycopy",       Long.class, "address"),
        jlongArraycopy     = c.getFieldValue("StubRoutines::_jlong_arraycopy",      Long.class, "address"),
        oopArraycopy       = c.getFieldValue("StubRoutines::_oop_arraycopy",        Long.class, "address"),
        oopArraycopyUninit = c.getFieldValue("StubRoutines::_oop_arraycopy_uninit", Long.class, "address");

    public static final long
        jbyteDisjointArraycopy     = c.getFieldValue("StubRoutines::_jbyte_disjoint_arraycopy",      Long.class, "address"),
        jshortDisjointArraycopy    = c.getFieldValue("StubRoutines::_jshort_disjoint_arraycopy",     Long.class, "address"),
        jintDisjointArraycopy      = c.getFieldValue("StubRoutines::_jint_disjoint_arraycopy",       Long.class, "address"),
        jlongDisjointArraycopy     = c.getFieldValue("StubRoutines::_jlong_disjoint_arraycopy",      Long.class, "address"),
        oopDisjointArraycopy       = c.getFieldValue("StubRoutines::_oop_disjoint_arraycopy",        Long.class, "address"),
        oopDisjointArraycopyUninit = c.getFieldValue("StubRoutines::_oop_disjoint_arraycopy_uninit", Long.class, "address");

    public static final long
        jbyteAlignedArraycopy     = c.getFieldValue("StubRoutines::_arrayof_jbyte_arraycopy",      Long.class, "address"),
        jshortAlignedArraycopy    = c.getFieldValue("StubRoutines::_arrayof_jshort_arraycopy",     Long.class, "address"),
        jintAlignedArraycopy      = c.getFieldValue("StubRoutines::_arrayof_jint_arraycopy",       Long.class, "address"),
        jlongAlignedArraycopy     = c.getFieldValue("StubRoutines::_arrayof_jlong_arraycopy",      Long.class, "address"),
        oopAlignedArraycopy       = c.getFieldValue("StubRoutines::_arrayof_oop_arraycopy",        Long.class, "address"),
        oopAlignedArraycopyUninit = c.getFieldValue("StubRoutines::_arrayof_oop_arraycopy_uninit", Long.class, "address");

    public static final long
        jbyteAlignedDisjointArraycopy     = c.getFieldValue("StubRoutines::_arrayof_jbyte_disjoint_arraycopy",      Long.class, "address"),
        jshortAlignedDisjointArraycopy    = c.getFieldValue("StubRoutines::_arrayof_jshort_disjoint_arraycopy",     Long.class, "address"),
        jintAlignedDisjointArraycopy      = c.getFieldValue("StubRoutines::_arrayof_jint_disjoint_arraycopy",       Long.class, "address"),
        jlongAlignedDisjointArraycopy     = c.getFieldValue("StubRoutines::_arrayof_jlong_disjoint_arraycopy",      Long.class, "address"),
        oopAlignedDisjointArraycopy       = c.getFieldValue("StubRoutines::_arrayof_oop_disjoint_arraycopy",        Long.class, "address"),
        oopAlignedDisjointArraycopyUninit = c.getFieldValue("StubRoutines::_arrayof_oop_disjoint_arraycopy_uninit", Long.class, "address");

    public static final long
        checkcastArraycopy       = c.getFieldValue("StubRoutines::_checkcast_arraycopy",        Long.class, "address"),
        checkcastArraycopyUninit = c.getFieldValue("StubRoutines::_checkcast_arraycopy_uninit", Long.class, "address"),
        unsafeArraycopy          = c.getFieldValue("StubRoutines::_unsafe_arraycopy",           Long.class, "address"),
        genericArraycopy         = c.getFieldValue("StubRoutines::_generic_arraycopy",          Long.class, "address");

    public static final long
        newInstanceAddress                     = c.getAddress("JVMCIRuntime::new_instance"),
        newArrayAddress                        = c.getAddress("JVMCIRuntime::new_array"),
        newMultiArrayAddress                   = c.getAddress("JVMCIRuntime::new_multi_array"),
        dynamicNewArrayAddress                 = c.getAddress("JVMCIRuntime::dynamic_new_array"),
        dynamicNewInstanceAddress              = c.getAddress("JVMCIRuntime::dynamic_new_instance"),
        threadIsInterruptedAddress             = c.getAddress("JVMCIRuntime::thread_is_interrupted"),
        identityHashCodeAddress                = c.getAddress("JVMCIRuntime::identity_hash_code"),
        exceptionHandlerForPcAddress           = c.getAddress("JVMCIRuntime::exception_handler_for_pc"),
        monitorenterAddress                    = c.getAddress("JVMCIRuntime::monitorenter"),
        monitorexitAddress                     = c.getAddress("JVMCIRuntime::monitorexit"),
        throwAndPostJvmtiExceptionAddress      = c.getAddress("JVMCIRuntime::throw_and_post_jvmti_exception"),
        throwKlassExternalNameExceptionAddress = c.getAddress("JVMCIRuntime::throw_klass_external_name_exception"),
        throwClassCastExceptionAddress         = c.getAddress("JVMCIRuntime::throw_class_cast_exception"),
        loadAndClearExceptionAddress           = c.getAddress("JVMCIRuntime::load_and_clear_exception"),
        writeBarrierPreAddress                 = c.getAddress("JVMCIRuntime::write_barrier_pre"),
        writeBarrierPostAddress                = c.getAddress("JVMCIRuntime::write_barrier_post");

    public static final long
        registerFinalizerAddress                = c.getAddress("SharedRuntime::register_finalizer"),
        exceptionHandlerForReturnAddressAddress = c.getAddress("SharedRuntime::exception_handler_for_return_address"),
        osrMigrationEndAddress                  = c.getAddress("SharedRuntime::OSR_migration_end");

    public static final long
        javaTimeMillisAddress = c.getAddress("os::javaTimeMillis"),
        javaTimeNanosAddress  = c.getAddress("os::javaTimeNanos");

    public static final long
        fremAddress = c.getAddress("SharedRuntime::frem"),
        dremAddress = c.getAddress("SharedRuntime::drem");

    public static final int
        verifiedEntryMark                       = c.getConstant("CodeInstaller::VERIFIED_ENTRY",                         Integer.class),
        unverifiedEntryMark                     = c.getConstant("CodeInstaller::UNVERIFIED_ENTRY",                       Integer.class),
        osrEntryMark                            = c.getConstant("CodeInstaller::OSR_ENTRY",                              Integer.class),
        exceptionHandlerEntryMark               = c.getConstant("CodeInstaller::EXCEPTION_HANDLER_ENTRY",                Integer.class),
        deoptHandlerEntryMark                   = c.getConstant("CodeInstaller::DEOPT_HANDLER_ENTRY",                    Integer.class),
        invokeinterfaceMark                     = c.getConstant("CodeInstaller::INVOKEINTERFACE",                        Integer.class),
        invokevirtualMark                       = c.getConstant("CodeInstaller::INVOKEVIRTUAL",                          Integer.class),
        invokestaticMark                        = c.getConstant("CodeInstaller::INVOKESTATIC",                           Integer.class),
        invokespecialMark                       = c.getConstant("CodeInstaller::INVOKESPECIAL",                          Integer.class),
        inlineInvokeMark                        = c.getConstant("CodeInstaller::INLINE_INVOKE",                          Integer.class),
        pollNearMark                            = c.getConstant("CodeInstaller::POLL_NEAR",                              Integer.class),
        pollReturnNearMark                      = c.getConstant("CodeInstaller::POLL_RETURN_NEAR",                       Integer.class),
        pollFarMark                             = c.getConstant("CodeInstaller::POLL_FAR",                               Integer.class),
        pollReturnFarMark                       = c.getConstant("CodeInstaller::POLL_RETURN_FAR",                        Integer.class),
        cardTableAddressMark                    = c.getConstant("CodeInstaller::CARD_TABLE_ADDRESS",                     Integer.class),
        heapTopAddressMark                      = c.getConstant("CodeInstaller::HEAP_TOP_ADDRESS",                       Integer.class),
        heapEndAddressMark                      = c.getConstant("CodeInstaller::HEAP_END_ADDRESS",                       Integer.class),
        narrowKlassBaseAddressMark              = c.getConstant("CodeInstaller::NARROW_KLASS_BASE_ADDRESS",              Integer.class),
        crcTableAddressMark                     = c.getConstant("CodeInstaller::CRC_TABLE_ADDRESS",                      Integer.class),
        logOfHeapRegionGrainBytesMark           = c.getConstant("CodeInstaller::LOG_OF_HEAP_REGION_GRAIN_BYTES",         Integer.class),
        inlineContiguousAllocationSupportedMark = c.getConstant("CodeInstaller::INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED", Integer.class);

    public static final int getArrayBaseOffset(JavaKind kind)
    {
        return HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(kind);
    }

    public static final int getArrayIndexScale(JavaKind kind)
    {
        return HotSpotJVMCIRuntimeProvider.getArrayIndexScale(kind);
    }
}
