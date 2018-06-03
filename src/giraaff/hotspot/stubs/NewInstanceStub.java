package giraaff.hotspot.stubs;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotForeignCallLinkage;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.GraalHotSpotVMConfigNode;
import giraaff.hotspot.nodes.StubForeignCallNode;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.NewObjectSnippets;
import giraaff.hotspot.stubs.StubUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.word.Word;

/**
 * Stub implementing the fast path for TLAB refill during instance class allocation. This stub is
 * called from the {@linkplain NewObjectSnippets inline} allocation code when TLAB allocation fails.
 * If this stub fails to refill the TLAB or allocate the object, it calls out to the HotSpot C++
 * runtime for to complete the allocation.
 */
// @class NewInstanceStub
public final class NewInstanceStub extends SnippetStub
{
    // @cons
    public NewInstanceStub(HotSpotProviders __providers, HotSpotForeignCallLinkage __linkage)
    {
        super("newInstance", __providers, __linkage);
    }

    @Override
    protected Object[] makeConstArgs()
    {
        HotSpotResolvedObjectType __intArrayType = (HotSpotResolvedObjectType) providers.getMetaAccess().lookupJavaType(int[].class);
        int __count = method.getSignature().getParameterCount(false);
        Object[] __args = new Object[__count];
        __args[1] = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), __intArrayType.klass(), null);
        __args[2] = providers.getRegisters().getThreadRegister();
        return __args;
    }

    private static Word allocate(Word __thread, int __size)
    {
        Word __top = HotSpotReplacementsUtil.readTlabTop(__thread);
        Word __end = HotSpotReplacementsUtil.readTlabEnd(__thread);
        Word __newTop = __top.add(__size);
        // this check might lead to problems if the TLAB is within 16GB of the address space end (checked in c++ code)
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __newTop.belowOrEqual(__end)))
        {
            HotSpotReplacementsUtil.writeTlabTop(__thread, __newTop);
            return __top;
        }
        return WordFactory.zero();
    }

    /**
     * Re-attempts allocation after an initial TLAB allocation failed or was skipped (e.g. due to -XX:-UseTLAB).
     *
     * @param hub the hub of the object to be allocated
     * @param intArrayHub the hub for {@code int[].class}
     */
    @Snippet
    private static Object newInstance(KlassPointer __hub, @ConstantParameter KlassPointer __intArrayHub, @ConstantParameter Register __threadRegister)
    {
        // The type is known to be an instance so Klass::_layout_helper is the instance size as a raw number.
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        if (GraalHotSpotVMConfigNode.inlineContiguousAllocationSupported())
        {
            if (HotSpotReplacementsUtil.isInstanceKlassFullyInitialized(__hub))
            {
                int __sizeInBytes = HotSpotReplacementsUtil.readLayoutHelper(__hub);
                Word __memory = refillAllocate(__thread, __intArrayHub, __sizeInBytes);
                if (__memory.notEqual(0))
                {
                    Word __prototypeMarkWord = __hub.readWord(HotSpotRuntime.prototypeMarkWordOffset, HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION);
                    NewObjectSnippets.formatObjectForStub(__hub, __sizeInBytes, __memory, __prototypeMarkWord);
                    return __memory.toObject();
                }
            }
        }

        newInstanceC(NEW_INSTANCE_C, __thread, __hub);
        StubUtil.handlePendingException(__thread, true);
        return HotSpotReplacementsUtil.getAndClearObjectResult(__thread);
    }

    /**
     * Attempts to refill the current thread's TLAB and retries the allocation.
     *
     * @param intArrayHub the hub for {@code int[].class}
     * @param sizeInBytes the size of the allocation
     *
     * @return the newly allocated, uninitialized chunk of memory,
     *         or {@link WordFactory#zero()} if the operation was unsuccessful
     */
    static Word refillAllocate(Word __thread, KlassPointer __intArrayHub, int __sizeInBytes)
    {
        // If G1 is enabled, the "eden" allocation space is not the same always
        // and therefore we have to go to slowpath to allocate a new TLAB.
        if (HotSpotRuntime.useG1GC)
        {
            return WordFactory.zero();
        }
        if (!HotSpotRuntime.useTLAB)
        {
            return edenAllocate(WordFactory.unsigned(__sizeInBytes));
        }
        Word __intArrayMarkWord = WordFactory.unsigned(HotSpotRuntime.tlabIntArrayMarkWord);
        int __alignmentReserveInBytes = HotSpotRuntime.tlabAlignmentReserve * HotSpotReplacementsUtil.wordSize();

        Word __top = HotSpotReplacementsUtil.readTlabTop(__thread);
        Word __end = HotSpotReplacementsUtil.readTlabEnd(__thread);

        // calculate amount of free space
        long __tlabFreeSpaceInBytes = __end.subtract(__top).rawValue();

        long __tlabFreeSpaceInWords = __tlabFreeSpaceInBytes >>> HotSpotReplacementsUtil.log2WordSize();

        // retain TLAB and allocate object in shared space if the amount free in the TLAB is too large to discard
        Word __refillWasteLimit = __thread.readWord(HotSpotRuntime.tlabRefillWasteLimitOffset, HotSpotReplacementsUtil.TLAB_REFILL_WASTE_LIMIT_LOCATION);
        if (__tlabFreeSpaceInWords <= __refillWasteLimit.rawValue())
        {
            if (HotSpotRuntime.tlabStats)
            {
                // increment number of refills
                __thread.writeInt(HotSpotRuntime.tlabNumberOfRefillsOffset, __thread.readInt(HotSpotRuntime.tlabNumberOfRefillsOffset, HotSpotReplacementsUtil.TLAB_NOF_REFILLS_LOCATION) + 1, HotSpotReplacementsUtil.TLAB_NOF_REFILLS_LOCATION);
                // accumulate wastage
                int __wastage = __thread.readInt(HotSpotRuntime.tlabFastRefillWasteOffset, HotSpotReplacementsUtil.TLAB_FAST_REFILL_WASTE_LOCATION) + (int) __tlabFreeSpaceInWords;
                __thread.writeInt(HotSpotRuntime.tlabFastRefillWasteOffset, __wastage, HotSpotReplacementsUtil.TLAB_FAST_REFILL_WASTE_LOCATION);
            }

            // if TLAB is currently allocated (top or end != null), then fill [top, end + alignment_reserve) with array object
            if (__top.notEqual(0))
            {
                int __headerSize = HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Int);
                // just like the HotSpot assembler stubs, assumes that tlabFreeSpaceInInts fits in an int
                int __tlabFreeSpaceInInts = (int) __tlabFreeSpaceInBytes >>> 2;
                int __length = ((__alignmentReserveInBytes - __headerSize) >>> 2) + __tlabFreeSpaceInInts;
                NewObjectSnippets.formatArray(__intArrayHub, 0, __length, __headerSize, __top, __intArrayMarkWord, false, false);

                long __allocated = __thread.readLong(HotSpotRuntime.threadAllocatedBytesOffset, HotSpotReplacementsUtil.TLAB_THREAD_ALLOCATED_BYTES_LOCATION);
                __allocated = __allocated + __top.subtract(HotSpotReplacementsUtil.readTlabStart(__thread)).rawValue();
                __thread.writeLong(HotSpotRuntime.threadAllocatedBytesOffset, __allocated, HotSpotReplacementsUtil.TLAB_THREAD_ALLOCATED_BYTES_LOCATION);
            }

            // refill the TLAB with an eden allocation
            Word __tlabRefillSizeInWords = __thread.readWord(HotSpotRuntime.threadTlabSizeOffset, HotSpotReplacementsUtil.TLAB_SIZE_LOCATION);
            Word __tlabRefillSizeInBytes = __tlabRefillSizeInWords.multiply(HotSpotReplacementsUtil.wordSize());
            // allocate new TLAB, address returned in top
            __top = edenAllocate(__tlabRefillSizeInBytes);
            if (__top.notEqual(0))
            {
                __end = __top.add(__tlabRefillSizeInBytes.subtract(__alignmentReserveInBytes));
                HotSpotReplacementsUtil.initializeTlab(__thread, __top, __end);

                return NewInstanceStub.allocate(__thread, __sizeInBytes);
            }
            else
            {
                return WordFactory.zero();
            }
        }
        else
        {
            // retain TLAB
            Word __newRefillWasteLimit = __refillWasteLimit.add(HotSpotRuntime.tlabRefillWasteIncrement);
            __thread.writeWord(HotSpotRuntime.tlabRefillWasteLimitOffset, __newRefillWasteLimit, HotSpotReplacementsUtil.TLAB_REFILL_WASTE_LIMIT_LOCATION);

            if (HotSpotRuntime.tlabStats)
            {
                __thread.writeInt(HotSpotRuntime.tlabSlowAllocationsOffset, __thread.readInt(HotSpotRuntime.tlabSlowAllocationsOffset, HotSpotReplacementsUtil.TLAB_SLOW_ALLOCATIONS_LOCATION) + 1, HotSpotReplacementsUtil.TLAB_SLOW_ALLOCATIONS_LOCATION);
            }

            return edenAllocate(WordFactory.unsigned(__sizeInBytes));
        }
    }

    /**
     * Attempts to allocate a chunk of memory from Eden space.
     *
     * @param sizeInBytes the size of the chunk to allocate
     * @return the allocated chunk or {@link WordFactory#zero()} if allocation fails
     */
    public static Word edenAllocate(Word __sizeInBytes)
    {
        final long __heapTopRawAddress = GraalHotSpotVMConfigNode.heapTopAddress();
        final long __heapEndRawAddress = GraalHotSpotVMConfigNode.heapEndAddress();

        Word __heapTopAddress = WordFactory.unsigned(__heapTopRawAddress);
        Word __heapEndAddress = WordFactory.unsigned(__heapEndRawAddress);

        while (true)
        {
            Word __heapTop = __heapTopAddress.readWord(0, HotSpotReplacementsUtil.HEAP_TOP_LOCATION);
            Word __newHeapTop = __heapTop.add(__sizeInBytes);
            if (__newHeapTop.belowOrEqual(__heapTop))
            {
                return WordFactory.zero();
            }

            Word __heapEnd = __heapEndAddress.readWord(0, HotSpotReplacementsUtil.HEAP_END_LOCATION);
            if (__newHeapTop.aboveThan(__heapEnd))
            {
                return WordFactory.zero();
            }
            if (__heapTopAddress.logicCompareAndSwapWord(0, __heapTop, __newHeapTop, HotSpotReplacementsUtil.HEAP_TOP_LOCATION))
            {
                return __heapTop;
            }
        }
    }

    // @def
    public static final ForeignCallDescriptor NEW_INSTANCE_C = StubUtil.newDescriptor(NewInstanceStub.class, "newInstanceC", void.class, Word.class, KlassPointer.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    public static native void newInstanceC(@ConstantNodeParameter ForeignCallDescriptor newInstanceC, Word thread, KlassPointer hub);
}
