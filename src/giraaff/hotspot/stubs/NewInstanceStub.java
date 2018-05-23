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
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotForeignCallLinkage;
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
import giraaff.options.OptionValues;
import giraaff.word.Word;

/**
 * Stub implementing the fast path for TLAB refill during instance class allocation. This stub is
 * called from the {@linkplain NewObjectSnippets inline} allocation code when TLAB allocation fails.
 * If this stub fails to refill the TLAB or allocate the object, it calls out to the HotSpot C++
 * runtime for to complete the allocation.
 */
public class NewInstanceStub extends SnippetStub
{
    public NewInstanceStub(OptionValues options, HotSpotProviders providers, HotSpotForeignCallLinkage linkage)
    {
        super("newInstance", options, providers, linkage);
    }

    @Override
    protected Object[] makeConstArgs()
    {
        HotSpotResolvedObjectType intArrayType = (HotSpotResolvedObjectType) providers.getMetaAccess().lookupJavaType(int[].class);
        int count = method.getSignature().getParameterCount(false);
        Object[] args = new Object[count];
        args[1] = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), intArrayType.klass(), null);
        args[2] = providers.getRegisters().getThreadRegister();
        args[3] = options;
        return args;
    }

    private static Word allocate(Word thread, int size)
    {
        Word top = HotSpotReplacementsUtil.readTlabTop(thread);
        Word end = HotSpotReplacementsUtil.readTlabEnd(thread);
        Word newTop = top.add(size);
        /*
         * this check might lead to problems if the TLAB is within 16GB of the address space end
         * (checked in c++ code)
         */
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, newTop.belowOrEqual(end)))
        {
            HotSpotReplacementsUtil.writeTlabTop(thread, newTop);
            return top;
        }
        return WordFactory.zero();
    }

    /**
     * Re-attempts allocation after an initial TLAB allocation failed or was skipped (e.g., due to
     * -XX:-UseTLAB).
     *
     * @param hub the hub of the object to be allocated
     * @param intArrayHub the hub for {@code int[].class}
     */
    @Snippet
    private static Object newInstance(KlassPointer hub, @ConstantParameter KlassPointer intArrayHub, @ConstantParameter Register threadRegister, @ConstantParameter OptionValues options)
    {
        /*
         * The type is known to be an instance so Klass::_layout_helper is the instance size as a
         * raw number
         */
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        boolean inlineContiguousAllocationSupported = GraalHotSpotVMConfigNode.inlineContiguousAllocationSupported();
        if (inlineContiguousAllocationSupported && !HotSpotReplacementsUtil.useCMSIncrementalMode(GraalHotSpotVMConfig.INJECTED_VMCONFIG))
        {
            if (HotSpotReplacementsUtil.isInstanceKlassFullyInitialized(hub))
            {
                int sizeInBytes = HotSpotReplacementsUtil.readLayoutHelper(hub);
                Word memory = refillAllocate(thread, intArrayHub, sizeInBytes);
                if (memory.notEqual(0))
                {
                    Word prototypeMarkWord = hub.readWord(HotSpotReplacementsUtil.prototypeMarkWordOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION);
                    NewObjectSnippets.formatObjectForStub(hub, sizeInBytes, memory, prototypeMarkWord);
                    return memory.toObject();
                }
            }
        }

        newInstanceC(NEW_INSTANCE_C, thread, hub);
        StubUtil.handlePendingException(thread, true);
        return HotSpotReplacementsUtil.getAndClearObjectResult(thread);
    }

    /**
     * Attempts to refill the current thread's TLAB and retries the allocation.
     *
     * @param intArrayHub the hub for {@code int[].class}
     * @param sizeInBytes the size of the allocation
     *
     * @return the newly allocated, uninitialized chunk of memory, or {@link WordFactory#zero()} if
     *         the operation was unsuccessful
     */
    static Word refillAllocate(Word thread, KlassPointer intArrayHub, int sizeInBytes)
    {
        // If G1 is enabled, the "eden" allocation space is not the same always
        // and therefore we have to go to slowpath to allocate a new TLAB.
        if (HotSpotReplacementsUtil.useG1GC(GraalHotSpotVMConfig.INJECTED_VMCONFIG))
        {
            return WordFactory.zero();
        }
        if (!HotSpotReplacementsUtil.useTLAB(GraalHotSpotVMConfig.INJECTED_VMCONFIG))
        {
            return edenAllocate(WordFactory.unsigned(sizeInBytes));
        }
        Word intArrayMarkWord = WordFactory.unsigned(HotSpotReplacementsUtil.tlabIntArrayMarkWord(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        int alignmentReserveInBytes = HotSpotReplacementsUtil.tlabAlignmentReserveInHeapWords(GraalHotSpotVMConfig.INJECTED_VMCONFIG) * HotSpotReplacementsUtil.wordSize();

        Word top = HotSpotReplacementsUtil.readTlabTop(thread);
        Word end = HotSpotReplacementsUtil.readTlabEnd(thread);

        // calculate amount of free space
        long tlabFreeSpaceInBytes = end.subtract(top).rawValue();

        long tlabFreeSpaceInWords = tlabFreeSpaceInBytes >>> HotSpotReplacementsUtil.log2WordSize();

        // Retain TLAB and allocate object in shared space if
        // the amount free in the TLAB is too large to discard.
        Word refillWasteLimit = thread.readWord(HotSpotReplacementsUtil.tlabRefillWasteLimitOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.TLAB_REFILL_WASTE_LIMIT_LOCATION);
        if (tlabFreeSpaceInWords <= refillWasteLimit.rawValue())
        {
            if (HotSpotReplacementsUtil.tlabStats(GraalHotSpotVMConfig.INJECTED_VMCONFIG))
            {
                // increment number of refills
                thread.writeInt(HotSpotReplacementsUtil.tlabNumberOfRefillsOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), thread.readInt(HotSpotReplacementsUtil.tlabNumberOfRefillsOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.TLAB_NOF_REFILLS_LOCATION) + 1, HotSpotReplacementsUtil.TLAB_NOF_REFILLS_LOCATION);
                // accumulate wastage
                int wastage = thread.readInt(HotSpotReplacementsUtil.tlabFastRefillWasteOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.TLAB_FAST_REFILL_WASTE_LOCATION) + (int) tlabFreeSpaceInWords;
                thread.writeInt(HotSpotReplacementsUtil.tlabFastRefillWasteOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), wastage, HotSpotReplacementsUtil.TLAB_FAST_REFILL_WASTE_LOCATION);
            }

            // if TLAB is currently allocated (top or end != null) then
            // fill [top, end + alignment_reserve) with array object
            if (top.notEqual(0))
            {
                int headerSize = HotSpotReplacementsUtil.arrayBaseOffset(JavaKind.Int);
                // just like the HotSpot assembler stubs, assumes that tlabFreeSpaceInInts fits in
                // an int
                int tlabFreeSpaceInInts = (int) tlabFreeSpaceInBytes >>> 2;
                int length = ((alignmentReserveInBytes - headerSize) >>> 2) + tlabFreeSpaceInInts;
                NewObjectSnippets.formatArray(intArrayHub, 0, length, headerSize, top, intArrayMarkWord, false, false, null);

                long allocated = thread.readLong(HotSpotReplacementsUtil.threadAllocatedBytesOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.TLAB_THREAD_ALLOCATED_BYTES_LOCATION);
                allocated = allocated + top.subtract(HotSpotReplacementsUtil.readTlabStart(thread)).rawValue();
                thread.writeLong(HotSpotReplacementsUtil.threadAllocatedBytesOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), allocated, HotSpotReplacementsUtil.TLAB_THREAD_ALLOCATED_BYTES_LOCATION);
            }

            // refill the TLAB with an eden allocation
            Word tlabRefillSizeInWords = thread.readWord(HotSpotReplacementsUtil.threadTlabSizeOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.TLAB_SIZE_LOCATION);
            Word tlabRefillSizeInBytes = tlabRefillSizeInWords.multiply(HotSpotReplacementsUtil.wordSize());
            // allocate new TLAB, address returned in top
            top = edenAllocate(tlabRefillSizeInBytes);
            if (top.notEqual(0))
            {
                end = top.add(tlabRefillSizeInBytes.subtract(alignmentReserveInBytes));
                HotSpotReplacementsUtil.initializeTlab(thread, top, end);

                return NewInstanceStub.allocate(thread, sizeInBytes);
            }
            else
            {
                return WordFactory.zero();
            }
        }
        else
        {
            // Retain TLAB
            Word newRefillWasteLimit = refillWasteLimit.add(HotSpotReplacementsUtil.tlabRefillWasteIncrement(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
            thread.writeWord(HotSpotReplacementsUtil.tlabRefillWasteLimitOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), newRefillWasteLimit, HotSpotReplacementsUtil.TLAB_REFILL_WASTE_LIMIT_LOCATION);

            if (HotSpotReplacementsUtil.tlabStats(GraalHotSpotVMConfig.INJECTED_VMCONFIG))
            {
                thread.writeInt(HotSpotReplacementsUtil.tlabSlowAllocationsOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), thread.readInt(HotSpotReplacementsUtil.tlabSlowAllocationsOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.TLAB_SLOW_ALLOCATIONS_LOCATION) + 1, HotSpotReplacementsUtil.TLAB_SLOW_ALLOCATIONS_LOCATION);
            }

            return edenAllocate(WordFactory.unsigned(sizeInBytes));
        }
    }

    /**
     * Attempts to allocate a chunk of memory from Eden space.
     *
     * @param sizeInBytes the size of the chunk to allocate
     * @return the allocated chunk or {@link WordFactory#zero()} if allocation fails
     */
    public static Word edenAllocate(Word sizeInBytes)
    {
        final long heapTopRawAddress = GraalHotSpotVMConfigNode.heapTopAddress();
        final long heapEndRawAddress = GraalHotSpotVMConfigNode.heapEndAddress();

        Word heapTopAddress = WordFactory.unsigned(heapTopRawAddress);
        Word heapEndAddress = WordFactory.unsigned(heapEndRawAddress);

        while (true)
        {
            Word heapTop = heapTopAddress.readWord(0, HotSpotReplacementsUtil.HEAP_TOP_LOCATION);
            Word newHeapTop = heapTop.add(sizeInBytes);
            if (newHeapTop.belowOrEqual(heapTop))
            {
                return WordFactory.zero();
            }

            Word heapEnd = heapEndAddress.readWord(0, HotSpotReplacementsUtil.HEAP_END_LOCATION);
            if (newHeapTop.aboveThan(heapEnd))
            {
                return WordFactory.zero();
            }
            if (heapTopAddress.logicCompareAndSwapWord(0, heapTop, newHeapTop, HotSpotReplacementsUtil.HEAP_TOP_LOCATION))
            {
                return heapTop;
            }
        }
    }

    public static final ForeignCallDescriptor NEW_INSTANCE_C = StubUtil.newDescriptor(NewInstanceStub.class, "newInstanceC", void.class, Word.class, KlassPointer.class);

    @NodeIntrinsic(StubForeignCallNode.class)
    public static native void newInstanceC(@ConstantNodeParameter ForeignCallDescriptor newInstanceC, Word thread, KlassPointer hub);
}
