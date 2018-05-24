package giraaff.hotspot;

import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotVMConfigStore;

import giraaff.core.common.CompressEncoding;
import giraaff.hotspot.nodes.GraalHotSpotVMConfigNode;

/**
 * Used to access native configuration details.
 */
public class GraalHotSpotVMConfig extends GraalHotSpotVMConfigBase
{
    GraalHotSpotVMConfig(HotSpotVMConfigStore store)
    {
        super(store);

        oopEncoding = new CompressEncoding(narrowOopBase, narrowOopShift);
        klassEncoding = new CompressEncoding(narrowKlassBase, narrowKlassShift);
    }

    private final CompressEncoding oopEncoding;
    private final CompressEncoding klassEncoding;

    public CompressEncoding getOopEncoding()
    {
        return oopEncoding;
    }

    public CompressEncoding getKlassEncoding()
    {
        return klassEncoding;
    }

    public final int codeEntryAlignment = getFlag("CodeEntryAlignment", Integer.class);
    public final boolean inline = getFlag("Inline", Boolean.class);
    public final boolean inlineNotify = getFlag("InlineNotify", Boolean.class);
    public final boolean useFastLocking = getFlag("JVMCIUseFastLocking", Boolean.class);
    public final boolean forceUnreachable = getFlag("ForceUnreachable", Boolean.class);
    public final boolean foldStableValues = getFlag("FoldStableValues", Boolean.class);

    public final boolean useTLAB = getFlag("UseTLAB", Boolean.class);
    public final boolean useBiasedLocking = getFlag("UseBiasedLocking", Boolean.class);
    public final boolean usePopCountInstruction = getFlag("UsePopCountInstruction", Boolean.class);
    public final boolean useAESIntrinsics = getFlag("UseAESIntrinsics", Boolean.class);
    public final boolean useCRC32Intrinsics = getFlag("UseCRC32Intrinsics", Boolean.class);
    public final boolean useCRC32CIntrinsics = getFlag("UseCRC32CIntrinsics", Boolean.class);
    public final boolean threadLocalHandshakes = getFlag("ThreadLocalHandshakes", Boolean.class, false);

    private final boolean useMultiplyToLenIntrinsic = getFlag("UseMultiplyToLenIntrinsic", Boolean.class);
    private final boolean useSHA1Intrinsics = getFlag("UseSHA1Intrinsics", Boolean.class);
    private final boolean useSHA256Intrinsics = getFlag("UseSHA256Intrinsics", Boolean.class);
    private final boolean useSHA512Intrinsics = getFlag("UseSHA512Intrinsics", Boolean.class);
    private final boolean useMontgomeryMultiplyIntrinsic = getFlag("UseMontgomeryMultiplyIntrinsic", Boolean.class, false);
    private final boolean useMontgomerySquareIntrinsic = getFlag("UseMontgomerySquareIntrinsic", Boolean.class, false);
    private final boolean useMulAddIntrinsic = getFlag("UseMulAddIntrinsic", Boolean.class, false);
    private final boolean useSquareToLenIntrinsic = getFlag("UseSquareToLenIntrinsic", Boolean.class, false);

    /*
     * These are methods because in some JDKs the flags are visible but the stubs themselves haven't
     * been exported so we have to check both if the flag is on and if we have the stub.
     */
    public boolean useMultiplyToLenIntrinsic()
    {
        return useMultiplyToLenIntrinsic && multiplyToLen != 0;
    }

    public boolean useSHA1Intrinsics()
    {
        return useSHA1Intrinsics && sha1ImplCompress != 0;
    }

    public boolean useSHA256Intrinsics()
    {
        return useSHA256Intrinsics && sha256ImplCompress != 0;
    }

    public boolean useSHA512Intrinsics()
    {
        return useSHA512Intrinsics && sha512ImplCompress != 0;
    }

    public boolean useMontgomeryMultiplyIntrinsic()
    {
        return useMontgomeryMultiplyIntrinsic && montgomeryMultiply != 0;
    }

    public boolean useMontgomerySquareIntrinsic()
    {
        return useMontgomerySquareIntrinsic && montgomerySquare != 0;
    }

    public boolean useMulAddIntrinsic()
    {
        return useMulAddIntrinsic && mulAdd != 0;
    }

    public boolean useSquareToLenIntrinsic()
    {
        return useSquareToLenIntrinsic && squareToLen != 0;
    }

    public boolean inlineNotify()
    {
        return inlineNotify && notifyAddress != 0;
    }

    public boolean inlineNotifyAll()
    {
        return inlineNotify && notifyAllAddress != 0;
    }

    public final boolean useG1GC = getFlag("UseG1GC", Boolean.class);
    public final boolean useCMSGC = getFlag("UseConcMarkSweepGC", Boolean.class);

    public final int allocatePrefetchStyle = getFlag("AllocatePrefetchStyle", Integer.class);
    public final int allocatePrefetchInstr = getFlag("AllocatePrefetchInstr", Integer.class);
    public final int allocatePrefetchLines = getFlag("AllocatePrefetchLines", Integer.class);
    public final int allocateInstancePrefetchLines = getFlag("AllocateInstancePrefetchLines", Integer.class);
    public final int allocatePrefetchStepSize = getFlag("AllocatePrefetchStepSize", Integer.class);
    public final int allocatePrefetchDistance = getFlag("AllocatePrefetchDistance", Integer.class);

    public final boolean useDeferredInitBarriers = getFlag("ReduceInitialCardMarks", Boolean.class);

    // Compressed Oops related values.
    public final boolean useCompressedOops = getFlag("UseCompressedOops", Boolean.class);
    public final boolean useCompressedClassPointers = getFlag("UseCompressedClassPointers", Boolean.class);

    public final long narrowOopBase = getFieldValue("CompilerToVM::Data::Universe_narrow_oop_base", Long.class, "address");
    public final int narrowOopShift = getFieldValue("CompilerToVM::Data::Universe_narrow_oop_shift", Integer.class, "int");
    public final int objectAlignment = getFlag("ObjectAlignmentInBytes", Integer.class);

    public final int minObjAlignment()
    {
        return objectAlignment / heapWordSize;
    }

    public final int logMinObjAlignment()
    {
        return (int) (Math.log(objectAlignment) / Math.log(2));
    }

    public final int narrowKlassSize = getFieldValue("CompilerToVM::Data::sizeof_narrowKlass", Integer.class, "int");
    public final long narrowKlassBase = getFieldValue("CompilerToVM::Data::Universe_narrow_klass_base", Long.class, "address");
    public final int narrowKlassShift = getFieldValue("CompilerToVM::Data::Universe_narrow_klass_shift", Integer.class, "int");

    public final int stackShadowPages = getFlag("StackShadowPages", Integer.class);
    public final boolean useStackBanging = getFlag("UseStackBanging", Boolean.class);
    public final int stackBias = getConstant("STACK_BIAS", Integer.class);
    public final int vmPageSize = getFieldValue("CompilerToVM::Data::vm_page_size", Integer.class, "int");

    public final int markOffset = getFieldOffset("oopDesc::_mark", Integer.class, "markOop");
    public final int hubOffset = getFieldOffset("oopDesc::_metadata._klass", Integer.class, "Klass*");

    public final int prototypeMarkWordOffset = getFieldOffset("Klass::_prototype_header", Integer.class, "markOop");
    public final int superCheckOffsetOffset = getFieldOffset("Klass::_super_check_offset", Integer.class, "juint");
    public final int secondarySuperCacheOffset = getFieldOffset("Klass::_secondary_super_cache", Integer.class, "Klass*");
    public final int secondarySupersOffset = getFieldOffset("Klass::_secondary_supers", Integer.class, "Array<Klass*>*");

    public final boolean classMirrorIsHandle;
    public final int classMirrorOffset;
    {
        String name = "Klass::_java_mirror";
        int offset = -1;
        boolean isHandle = false;
        try
        {
            offset = getFieldOffset(name, Integer.class, "oop");
        }
        catch (JVMCIError e)
        {
        }
        if (offset == -1)
        {
            try
            {
                offset = getFieldOffset(name, Integer.class, "jobject");
                isHandle = true;
            }
            catch (JVMCIError e)
            {
                try
                {
                    // JDK-8186777
                    offset = getFieldOffset(name, Integer.class, "OopHandle");
                    isHandle = true;
                }
                catch (JVMCIError e2)
                {
                }
            }
        }
        if (offset == -1)
        {
            throw new JVMCIError("cannot get offset of field " + name + " with type oop, jobject or OopHandle");
        }
        classMirrorOffset = offset;
        classMirrorIsHandle = isHandle;
    }

    public final int klassSuperKlassOffset = getFieldOffset("Klass::_super", Integer.class, "Klass*");
    public final int klassModifierFlagsOffset = getFieldOffset("Klass::_modifier_flags", Integer.class, "jint");
    public final int klassAccessFlagsOffset = getFieldOffset("Klass::_access_flags", Integer.class, "AccessFlags");
    public final int klassLayoutHelperOffset = getFieldOffset("Klass::_layout_helper", Integer.class, "jint");
    public final int klassLayoutHelperNeutralValue = getConstant("Klass::_lh_neutral_value", Integer.class);
    public final int layoutHelperLog2ElementSizeShift = getConstant("Klass::_lh_log2_element_size_shift", Integer.class);
    public final int layoutHelperLog2ElementSizeMask = getConstant("Klass::_lh_log2_element_size_mask", Integer.class);
    public final int layoutHelperElementTypeShift = getConstant("Klass::_lh_element_type_shift", Integer.class);
    public final int layoutHelperElementTypeMask = getConstant("Klass::_lh_element_type_mask", Integer.class);
    public final int layoutHelperHeaderSizeShift = getConstant("Klass::_lh_header_size_shift", Integer.class);
    public final int layoutHelperHeaderSizeMask = getConstant("Klass::_lh_header_size_mask", Integer.class);
    public final int layoutHelperArrayTagShift = getConstant("Klass::_lh_array_tag_shift", Integer.class);
    public final int layoutHelperArrayTagTypeValue = getConstant("Klass::_lh_array_tag_type_value", Integer.class);
    public final int layoutHelperArrayTagObjectValue = getConstant("Klass::_lh_array_tag_obj_value", Integer.class);

    /**
     * This filters out the bit that differentiates a type array from an object array.
     */
    public int layoutHelperElementTypePrimitiveInPlace()
    {
        return (layoutHelperArrayTagTypeValue & ~layoutHelperArrayTagObjectValue) << layoutHelperArrayTagShift;
    }

    public final int instanceKlassInitStateOffset = getFieldOffset("InstanceKlass::_init_state", Integer.class, "u1");
    public final int instanceKlassConstantsOffset = getFieldOffset("InstanceKlass::_constants", Integer.class, "ConstantPool*");
    public final int instanceKlassStateFullyInitialized = getConstant("InstanceKlass::fully_initialized", Integer.class);

    public final int arrayOopDescSize = getFieldValue("CompilerToVM::Data::sizeof_arrayOopDesc", Integer.class, "int");

    /**
     * The offset of the array length word in an array object's header.
     *
     * See {@code arrayOopDesc::length_offset_in_bytes()}.
     */
    public final int arrayOopDescLengthOffset()
    {
        return useCompressedClassPointers ? hubOffset + narrowKlassSize : arrayOopDescSize;
    }

    public final int metaspaceArrayLengthOffset = getFieldOffset("Array<Klass*>::_length", Integer.class, "int");
    public final int metaspaceArrayBaseOffset = getFieldOffset("Array<Klass*>::_data[0]", Integer.class, "Klass*");

    public final int arrayClassElementOffset = getFieldOffset("ObjArrayKlass::_element_klass", Integer.class, "Klass*");

    public final int jvmAccWrittenFlags = getConstant("JVM_ACC_WRITTEN_FLAGS", Integer.class);

    public final int threadTlabOffset = getFieldOffset("Thread::_tlab", Integer.class, "ThreadLocalAllocBuffer");
    public final int javaThreadAnchorOffset = getFieldOffset("JavaThread::_anchor", Integer.class, "JavaFrameAnchor");
    public final int threadObjectOffset = getFieldOffset("JavaThread::_threadObj", Integer.class, "oop");
    public final int osThreadOffset = getFieldOffset("JavaThread::_osthread", Integer.class, "OSThread*");
    public final int threadIsMethodHandleReturnOffset = getFieldOffset("JavaThread::_is_method_handle_return", Integer.class, "int");
    public final int threadObjectResultOffset = getFieldOffset("JavaThread::_vm_result", Integer.class, "oop");

    /**
     * This field is used to pass exception objects into and out of the runtime system during
     * exception handling for compiled code.
     */
    public final int threadExceptionOopOffset = getFieldOffset("JavaThread::_exception_oop", Integer.class, "oop");
    public final int threadExceptionPcOffset = getFieldOffset("JavaThread::_exception_pc", Integer.class, "address");
    public final int pendingExceptionOffset = getFieldOffset("ThreadShadow::_pending_exception", Integer.class, "oop");

    public final int pendingDeoptimizationOffset = getFieldOffset("JavaThread::_pending_deoptimization", Integer.class, "int");
    public final int pendingFailedSpeculationOffset = getFieldOffset("JavaThread::_pending_failed_speculation", Integer.class, "oop");

    private final int javaFrameAnchorLastJavaSpOffset = getFieldOffset("JavaFrameAnchor::_last_Java_sp", Integer.class, "intptr_t*");
    private final int javaFrameAnchorLastJavaPcOffset = getFieldOffset("JavaFrameAnchor::_last_Java_pc", Integer.class, "address");

    public int threadLastJavaSpOffset()
    {
        return javaThreadAnchorOffset + javaFrameAnchorLastJavaSpOffset;
    }

    public int threadLastJavaPcOffset()
    {
        return javaThreadAnchorOffset + javaFrameAnchorLastJavaPcOffset;
    }

    public int threadLastJavaFpOffset()
    {
        return javaThreadAnchorOffset + getFieldOffset("JavaFrameAnchor::_last_Java_fp", Integer.class, "intptr_t*");
    }

    public int threadJavaFrameAnchorFlagsOffset()
    {
        return javaThreadAnchorOffset + getFieldOffset("JavaFrameAnchor::_flags", Integer.class, "int");
    }

    public final int frameInterpreterFrameSenderSpOffset = getConstant("frame::interpreter_frame_sender_sp_offset", Integer.class, null);
    public final int frameInterpreterFrameLastSpOffset = getConstant("frame::interpreter_frame_last_sp_offset", Integer.class, null);

    public final int osThreadInterruptedOffset = getFieldOffset("OSThread::_interrupted", Integer.class, "jint");

    public final long markOopDescHashShift = getConstant("markOopDesc::hash_shift", Long.class);

    public final int biasedLockMaskInPlace = getConstant("markOopDesc::biased_lock_mask_in_place", Integer.class);
    public final int ageMaskInPlace = getConstant("markOopDesc::age_mask_in_place", Integer.class);
    public final int epochMaskInPlace = getConstant("markOopDesc::epoch_mask_in_place", Integer.class);
    public final long markOopDescHashMask = getConstant("markOopDesc::hash_mask", Long.class);
    public final long markOopDescHashMaskInPlace = getConstant("markOopDesc::hash_mask_in_place", Long.class);

    public final int unlockedMask = getConstant("markOopDesc::unlocked_value", Integer.class);
    public final int monitorMask = getConstant("markOopDesc::monitor_value", Integer.class, -1);
    public final int biasedLockPattern = getConstant("markOopDesc::biased_lock_pattern", Integer.class);

    // This field has no type in vmStructs.cpp
    public final int objectMonitorOwner = getFieldOffset("ObjectMonitor::_owner", Integer.class, null, -1);
    public final int objectMonitorRecursions = getFieldOffset("ObjectMonitor::_recursions", Integer.class, "intptr_t", -1);
    public final int objectMonitorCxq = getFieldOffset("ObjectMonitor::_cxq", Integer.class, "ObjectWaiter*", -1);
    public final int objectMonitorEntryList = getFieldOffset("ObjectMonitor::_EntryList", Integer.class, "ObjectWaiter*", -1);

    public final int markWordNoHashInPlace = getConstant("markOopDesc::no_hash_in_place", Integer.class);
    public final int markWordNoLockInPlace = getConstant("markOopDesc::no_lock_in_place", Integer.class);

    /**
     * See {@code markOopDesc::prototype()}.
     */
    public long arrayPrototypeMarkWord()
    {
        return markWordNoHashInPlace | markWordNoLockInPlace;
    }

    /**
     * See {@code markOopDesc::copy_set_hash()}.
     */
    public long tlabIntArrayMarkWord()
    {
        long tmp = arrayPrototypeMarkWord() & (~markOopDescHashMaskInPlace);
        tmp |= ((0x2 & markOopDescHashMask) << markOopDescHashShift);
        return tmp;
    }

    /**
     * Mark word right shift to get identity hash code.
     */
    public final int identityHashCodeShift = getConstant("markOopDesc::hash_shift", Integer.class);

    /**
     * Identity hash code value when uninitialized.
     */
    public final int uninitializedIdentityHashCodeValue = getConstant("markOopDesc::no_hash", Integer.class);

    public final int methodCompiledEntryOffset = getFieldOffset("Method::_from_compiled_entry", Integer.class, "address");

    public final int nmethodEntryOffset = getFieldOffset("nmethod::_verified_entry_point", Integer.class, "address");
    public final int compilationLevelFullOptimization = getConstant("CompLevel_full_optimization", Integer.class);

    public final int constantPoolSize = getFieldValue("CompilerToVM::Data::sizeof_ConstantPool", Integer.class, "int");
    public final int constantPoolLengthOffset = getFieldOffset("ConstantPool::_length", Integer.class, "int");

    public final int heapWordSize = getConstant("HeapWordSize", Integer.class);

    /**
     * Bit pattern that represents a non-oop. Neither the high bits nor the low bits of this value
     * are allowed to look like (respectively) the high or low bits of a real oop.
     */
    public final long nonOopBits = getFieldValue("CompilerToVM::Data::Universe_non_oop_bits", Long.class, "void*");

    public final int logOfHRGrainBytes = getFieldValue("HeapRegion::LogOfHRGrainBytes", Integer.class, "int");

    public final long cardtableStartAddress = getFieldValue("CompilerToVM::Data::cardtable_start_address", Long.class, "jbyte*");
    public final int cardtableShift = getFieldValue("CompilerToVM::Data::cardtable_shift", Integer.class, "int");

    /**
     * This is the largest stack offset encodeable in an OopMapValue. Offsets larger than this will
     * throw an exception during code installation.
     */
    public final int maxOopMapStackOffset = getFieldValue("CompilerToVM::Data::_max_oop_map_stack_offset", Integer.class, "int");

    public final long safepointPollingAddress = getFieldValue("os::_polling_page", Long.class, "address");

    // G1 Collector Related Values.

    public final byte dirtyCardValue = getConstant("CardTableModRefBS::dirty_card", Byte.class);
    public final byte g1YoungCardValue = getConstant("G1SATBCardTableModRefBS::g1_young_gen", Byte.class);

    final int dirtyCardQueueBufferOffset = getConstant("dirtyCardQueueBufferOffset", Integer.class);
    final int dirtyCardQueueIndexOffset = getConstant("dirtyCardQueueIndexOffset", Integer.class);
    final int satbMarkQueueBufferOffset = getConstant("satbMarkQueueBufferOffset", Integer.class);
    final int satbMarkQueueIndexOffset = getConstant("satbMarkQueueIndexOffset", Integer.class);
    final int satbMarkQueueActiveOffset = getConstant("satbMarkQueueActiveOffset", Integer.class);

    final int javaThreadDirtyCardQueueOffset = getFieldOffset("JavaThread::_dirty_card_queue", Integer.class, "DirtyCardQueue");
    final int javaThreadSatbMarkQueueOffset = getFieldOffset("JavaThread::_satb_mark_queue", Integer.class);

    public final int g1CardQueueIndexOffset = javaThreadDirtyCardQueueOffset + dirtyCardQueueIndexOffset;
    public final int g1CardQueueBufferOffset = javaThreadDirtyCardQueueOffset + dirtyCardQueueBufferOffset;
    public final int g1SATBQueueMarkingOffset = javaThreadSatbMarkQueueOffset + satbMarkQueueActiveOffset;
    public final int g1SATBQueueIndexOffset = javaThreadSatbMarkQueueOffset + satbMarkQueueIndexOffset;
    public final int g1SATBQueueBufferOffset = javaThreadSatbMarkQueueOffset + satbMarkQueueBufferOffset;

    public final int klassOffset = getFieldValue("java_lang_Class::_klass_offset", Integer.class, "int");
    public final int arrayKlassOffset = getFieldValue("java_lang_Class::_array_klass_offset", Integer.class, "int");

    public final int basicLockDisplacedHeaderOffset = getFieldOffset("BasicLock::_displaced_header", Integer.class, "markOop");

    public final int threadPollingPageOffset = getFieldOffset("Thread::_polling_page", Integer.class, "address", -1);
    public final int threadAllocatedBytesOffset = getFieldOffset("Thread::_allocated_bytes", Integer.class, "jlong");

    public final int tlabRefillWasteIncrement = getFlag("TLABWasteIncrement", Integer.class);

    private final int threadLocalAllocBufferStartOffset = getFieldOffset("ThreadLocalAllocBuffer::_start", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferEndOffset = getFieldOffset("ThreadLocalAllocBuffer::_end", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferTopOffset = getFieldOffset("ThreadLocalAllocBuffer::_top", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferPfTopOffset = getFieldOffset("ThreadLocalAllocBuffer::_pf_top", Integer.class, "HeapWord*");
    private final int threadLocalAllocBufferSlowAllocationsOffset = getFieldOffset("ThreadLocalAllocBuffer::_slow_allocations", Integer.class, "unsigned");
    private final int threadLocalAllocBufferFastRefillWasteOffset = getFieldOffset("ThreadLocalAllocBuffer::_fast_refill_waste", Integer.class, "unsigned");
    private final int threadLocalAllocBufferNumberOfRefillsOffset = getFieldOffset("ThreadLocalAllocBuffer::_number_of_refills", Integer.class, "unsigned");
    private final int threadLocalAllocBufferRefillWasteLimitOffset = getFieldOffset("ThreadLocalAllocBuffer::_refill_waste_limit", Integer.class, "size_t");
    private final int threadLocalAllocBufferDesiredSizeOffset = getFieldOffset("ThreadLocalAllocBuffer::_desired_size", Integer.class, "size_t");

    public int tlabSlowAllocationsOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferSlowAllocationsOffset;
    }

    public int tlabFastRefillWasteOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferFastRefillWasteOffset;
    }

    public int tlabNumberOfRefillsOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferNumberOfRefillsOffset;
    }

    public int tlabRefillWasteLimitOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferRefillWasteLimitOffset;
    }

    public int threadTlabSizeOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferDesiredSizeOffset;
    }

    public int threadTlabStartOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferStartOffset;
    }

    public int threadTlabEndOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferEndOffset;
    }

    public int threadTlabTopOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferTopOffset;
    }

    public int threadTlabPfTopOffset()
    {
        return threadTlabOffset + threadLocalAllocBufferPfTopOffset;
    }

    public final int tlabAlignmentReserve = getFieldValue("CompilerToVM::Data::ThreadLocalAllocBuffer_alignment_reserve", Integer.class, "size_t");

    public final boolean tlabStats = getFlag("TLABStats", Boolean.class);

    // FIXME This is only temporary until the GC code is changed.
    public final boolean inlineContiguousAllocationSupported = getFieldValue("CompilerToVM::Data::_supports_inline_contig_alloc", Boolean.class);
    public final long heapEndAddress = getFieldValue("CompilerToVM::Data::_heap_end_addr", Long.class, "HeapWord**");
    public final long heapTopAddress = getFieldValue("CompilerToVM::Data::_heap_top_addr", Long.class, "HeapWord* volatile*");

    public final boolean cmsIncrementalMode = getFlag("CMSIncrementalMode", Boolean.class, false);

    public final long inlineCacheMissStub = getFieldValue("CompilerToVM::Data::SharedRuntime_ic_miss_stub", Long.class, "address");

    public final long handleDeoptStub = getFieldValue("CompilerToVM::Data::SharedRuntime_deopt_blob_unpack", Long.class, "address");
    public final long uncommonTrapStub = getFieldValue("CompilerToVM::Data::SharedRuntime_deopt_blob_uncommon_trap", Long.class, "address");

    public final long codeCacheLowBound = getFieldValue("CodeCache::_low_bound", Long.class, "address");
    public final long codeCacheHighBound = getFieldValue("CodeCache::_high_bound", Long.class, "address");

    public final long aescryptEncryptBlockStub = getFieldValue("StubRoutines::_aescrypt_encryptBlock", Long.class, "address");
    public final long aescryptDecryptBlockStub = getFieldValue("StubRoutines::_aescrypt_decryptBlock", Long.class, "address");
    public final long cipherBlockChainingEncryptAESCryptStub = getFieldValue("StubRoutines::_cipherBlockChaining_encryptAESCrypt", Long.class, "address");
    public final long cipherBlockChainingDecryptAESCryptStub = getFieldValue("StubRoutines::_cipherBlockChaining_decryptAESCrypt", Long.class, "address");
    public final long updateBytesCRC32Stub = getFieldValue("StubRoutines::_updateBytesCRC32", Long.class, "address");
    public final long crcTableAddress = getFieldValue("StubRoutines::_crc_table_adr", Long.class, "address");

    public final long sha1ImplCompress = getFieldValue("StubRoutines::_sha1_implCompress", Long.class, "address", 0L);
    public final long sha256ImplCompress = getFieldValue("StubRoutines::_sha256_implCompress", Long.class, "address", 0L);
    public final long sha512ImplCompress = getFieldValue("StubRoutines::_sha512_implCompress", Long.class, "address", 0L);
    public final long multiplyToLen = getFieldValue("StubRoutines::_multiplyToLen", Long.class, "address", null);

    public final long updateBytesCRC32C = getFieldValue("StubRoutines::_updateBytesCRC32C", Long.class, "address", 0L);
    public final long squareToLen = getFieldValue("StubRoutines::_squareToLen", Long.class, "address", null);
    public final long mulAdd = getFieldValue("StubRoutines::_mulAdd", Long.class, "address", null);
    public final long montgomeryMultiply = getFieldValue("StubRoutines::_montgomeryMultiply", Long.class, "address", null);
    public final long montgomerySquare = getFieldValue("StubRoutines::_montgomerySquare", Long.class, "address", null);

    public final long jbyteArraycopy = getFieldValue("StubRoutines::_jbyte_arraycopy", Long.class, "address");
    public final long jshortArraycopy = getFieldValue("StubRoutines::_jshort_arraycopy", Long.class, "address");
    public final long jintArraycopy = getFieldValue("StubRoutines::_jint_arraycopy", Long.class, "address");
    public final long jlongArraycopy = getFieldValue("StubRoutines::_jlong_arraycopy", Long.class, "address");
    public final long oopArraycopy = getFieldValue("StubRoutines::_oop_arraycopy", Long.class, "address");
    public final long oopArraycopyUninit = getFieldValue("StubRoutines::_oop_arraycopy_uninit", Long.class, "address");
    public final long jbyteDisjointArraycopy = getFieldValue("StubRoutines::_jbyte_disjoint_arraycopy", Long.class, "address");
    public final long jshortDisjointArraycopy = getFieldValue("StubRoutines::_jshort_disjoint_arraycopy", Long.class, "address");
    public final long jintDisjointArraycopy = getFieldValue("StubRoutines::_jint_disjoint_arraycopy", Long.class, "address");
    public final long jlongDisjointArraycopy = getFieldValue("StubRoutines::_jlong_disjoint_arraycopy", Long.class, "address");
    public final long oopDisjointArraycopy = getFieldValue("StubRoutines::_oop_disjoint_arraycopy", Long.class, "address");
    public final long oopDisjointArraycopyUninit = getFieldValue("StubRoutines::_oop_disjoint_arraycopy_uninit", Long.class, "address");
    public final long jbyteAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jbyte_arraycopy", Long.class, "address");
    public final long jshortAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jshort_arraycopy", Long.class, "address");
    public final long jintAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jint_arraycopy", Long.class, "address");
    public final long jlongAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_jlong_arraycopy", Long.class, "address");
    public final long oopAlignedArraycopy = getFieldValue("StubRoutines::_arrayof_oop_arraycopy", Long.class, "address");
    public final long oopAlignedArraycopyUninit = getFieldValue("StubRoutines::_arrayof_oop_arraycopy_uninit", Long.class, "address");
    public final long jbyteAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jbyte_disjoint_arraycopy", Long.class, "address");
    public final long jshortAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jshort_disjoint_arraycopy", Long.class, "address");
    public final long jintAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jint_disjoint_arraycopy", Long.class, "address");
    public final long jlongAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_jlong_disjoint_arraycopy", Long.class, "address");
    public final long oopAlignedDisjointArraycopy = getFieldValue("StubRoutines::_arrayof_oop_disjoint_arraycopy", Long.class, "address");
    public final long oopAlignedDisjointArraycopyUninit = getFieldValue("StubRoutines::_arrayof_oop_disjoint_arraycopy_uninit", Long.class, "address");
    public final long checkcastArraycopy = getFieldValue("StubRoutines::_checkcast_arraycopy", Long.class, "address");
    public final long checkcastArraycopyUninit = getFieldValue("StubRoutines::_checkcast_arraycopy_uninit", Long.class, "address");
    public final long unsafeArraycopy = getFieldValue("StubRoutines::_unsafe_arraycopy", Long.class, "address");
    public final long genericArraycopy = getFieldValue("StubRoutines::_generic_arraycopy", Long.class, "address");

    public final long newInstanceAddress = getAddress("JVMCIRuntime::new_instance");
    public final long newArrayAddress = getAddress("JVMCIRuntime::new_array");
    public final long newMultiArrayAddress = getAddress("JVMCIRuntime::new_multi_array");
    public final long dynamicNewArrayAddress = getAddress("JVMCIRuntime::dynamic_new_array");
    public final long dynamicNewInstanceAddress = getAddress("JVMCIRuntime::dynamic_new_instance");

    public final long threadIsInterruptedAddress = getAddress("JVMCIRuntime::thread_is_interrupted");
    public final long identityHashCodeAddress = getAddress("JVMCIRuntime::identity_hash_code");
    public final long exceptionHandlerForPcAddress = getAddress("JVMCIRuntime::exception_handler_for_pc");
    public final long monitorenterAddress = getAddress("JVMCIRuntime::monitorenter");
    public final long monitorexitAddress = getAddress("JVMCIRuntime::monitorexit");
    public final long notifyAddress = getAddress("JVMCIRuntime::object_notify", 0L);
    public final long notifyAllAddress = getAddress("JVMCIRuntime::object_notifyAll", 0L);
    public final long throwAndPostJvmtiExceptionAddress = getAddress("JVMCIRuntime::throw_and_post_jvmti_exception");
    public final long throwKlassExternalNameExceptionAddress = getAddress("JVMCIRuntime::throw_klass_external_name_exception");
    public final long throwClassCastExceptionAddress = getAddress("JVMCIRuntime::throw_class_cast_exception");
    public final long loadAndClearExceptionAddress = getAddress("JVMCIRuntime::load_and_clear_exception");
    public final long writeBarrierPreAddress = getAddress("JVMCIRuntime::write_barrier_pre");
    public final long writeBarrierPostAddress = getAddress("JVMCIRuntime::write_barrier_post");

    public final long registerFinalizerAddress = getAddress("SharedRuntime::register_finalizer");
    public final long exceptionHandlerForReturnAddressAddress = getAddress("SharedRuntime::exception_handler_for_return_address");
    public final long osrMigrationEndAddress = getAddress("SharedRuntime::OSR_migration_end");

    public final long javaTimeMillisAddress = getAddress("os::javaTimeMillis");
    public final long javaTimeNanosAddress = getAddress("os::javaTimeNanos");
    public final long arithmeticSinAddress = getFieldValue("CompilerToVM::Data::dsin", Long.class, "address");
    public final long arithmeticCosAddress = getFieldValue("CompilerToVM::Data::dcos", Long.class, "address");
    public final long arithmeticTanAddress = getFieldValue("CompilerToVM::Data::dtan", Long.class, "address");
    public final long arithmeticExpAddress = getFieldValue("CompilerToVM::Data::dexp", Long.class, "address");
    public final long arithmeticLogAddress = getFieldValue("CompilerToVM::Data::dlog", Long.class, "address");
    public final long arithmeticLog10Address = getFieldValue("CompilerToVM::Data::dlog10", Long.class, "address");
    public final long arithmeticPowAddress = getFieldValue("CompilerToVM::Data::dpow", Long.class, "address");

    public final long fremAddress = getAddress("SharedRuntime::frem");
    public final long dremAddress = getAddress("SharedRuntime::drem");

    public final int MARKID_VERIFIED_ENTRY = getConstant("CodeInstaller::VERIFIED_ENTRY", Integer.class);
    public final int MARKID_UNVERIFIED_ENTRY = getConstant("CodeInstaller::UNVERIFIED_ENTRY", Integer.class);
    public final int MARKID_OSR_ENTRY = getConstant("CodeInstaller::OSR_ENTRY", Integer.class);
    public final int MARKID_EXCEPTION_HANDLER_ENTRY = getConstant("CodeInstaller::EXCEPTION_HANDLER_ENTRY", Integer.class);
    public final int MARKID_DEOPT_HANDLER_ENTRY = getConstant("CodeInstaller::DEOPT_HANDLER_ENTRY", Integer.class);
    public final int MARKID_INVOKEINTERFACE = getConstant("CodeInstaller::INVOKEINTERFACE", Integer.class);
    public final int MARKID_INVOKEVIRTUAL = getConstant("CodeInstaller::INVOKEVIRTUAL", Integer.class);
    public final int MARKID_INVOKESTATIC = getConstant("CodeInstaller::INVOKESTATIC", Integer.class);
    public final int MARKID_INVOKESPECIAL = getConstant("CodeInstaller::INVOKESPECIAL", Integer.class);
    public final int MARKID_INLINE_INVOKE = getConstant("CodeInstaller::INLINE_INVOKE", Integer.class);
    public final int MARKID_POLL_NEAR = getConstant("CodeInstaller::POLL_NEAR", Integer.class);
    public final int MARKID_POLL_RETURN_NEAR = getConstant("CodeInstaller::POLL_RETURN_NEAR", Integer.class);
    public final int MARKID_POLL_FAR = getConstant("CodeInstaller::POLL_FAR", Integer.class);
    public final int MARKID_POLL_RETURN_FAR = getConstant("CodeInstaller::POLL_RETURN_FAR", Integer.class);
    public final int MARKID_CARD_TABLE_ADDRESS = getConstant("CodeInstaller::CARD_TABLE_ADDRESS", Integer.class);

    /**
     * The following constants are given default values here since they are missing in the native
     * JVMCI-8 code but are still required for {@link GraalHotSpotVMConfigNode#canonical} to work in
     * a JDK8 environment.
     */
    public final int MARKID_HEAP_TOP_ADDRESS = getConstant("CodeInstaller::HEAP_TOP_ADDRESS", Integer.class, 17);
    public final int MARKID_HEAP_END_ADDRESS = getConstant("CodeInstaller::HEAP_END_ADDRESS", Integer.class, 18);
    public final int MARKID_NARROW_KLASS_BASE_ADDRESS = getConstant("CodeInstaller::NARROW_KLASS_BASE_ADDRESS", Integer.class, 19);
    public final int MARKID_CRC_TABLE_ADDRESS = getConstant("CodeInstaller::CRC_TABLE_ADDRESS", Integer.class, 21);
    public final int MARKID_LOG_OF_HEAP_REGION_GRAIN_BYTES = getConstant("CodeInstaller::LOG_OF_HEAP_REGION_GRAIN_BYTES", Integer.class, 22);
    public final int MARKID_INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED = getConstant("CodeInstaller::INLINE_CONTIGUOUS_ALLOCATION_SUPPORTED", Integer.class, 23);
}