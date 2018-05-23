package giraaff.hotspot.replacements;

import jdk.vm.ci.code.MemoryBarriers;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.Pointer;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.CompressEncoding;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.hotspot.nodes.G1ArrayRangePostWriteBarrier;
import giraaff.hotspot.nodes.G1ArrayRangePreWriteBarrier;
import giraaff.hotspot.nodes.G1PostWriteBarrier;
import giraaff.hotspot.nodes.G1PreWriteBarrier;
import giraaff.hotspot.nodes.G1ReferentFieldReadBarrier;
import giraaff.hotspot.nodes.GraalHotSpotVMConfigNode;
import giraaff.hotspot.nodes.HotSpotCompressionNode;
import giraaff.hotspot.nodes.SerialArrayRangeWriteBarrier;
import giraaff.hotspot.nodes.SerialWriteBarrier;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.FixedValueAnchorNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.MembarNode;
import giraaff.nodes.extended.NullCheckNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.AddressNode.Address;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.type.NarrowOopStamp;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetCounter;
import giraaff.replacements.SnippetCounter.Group;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.DirectStoreNode;
import giraaff.word.Word;

public class WriteBarrierSnippets implements Snippets
{
    static class Counters
    {
        Counters(SnippetCounter.Group.Factory factory)
        {
            Group countersWriteBarriers = factory.createSnippetCounterGroup("WriteBarriers");
            serialWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "serialWriteBarrier", "Number of Serial Write Barriers");
            g1AttemptedPreWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1AttemptedPreWriteBarrier", "Number of attempted G1 Pre Write Barriers");
            g1EffectivePreWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1EffectivePreWriteBarrier", "Number of effective G1 Pre Write Barriers");
            g1ExecutedPreWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1ExecutedPreWriteBarrier", "Number of executed G1 Pre Write Barriers");
            g1AttemptedPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1AttemptedPostWriteBarrier", "Number of attempted G1 Post Write Barriers");
            g1EffectiveAfterXORPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1EffectiveAfterXORPostWriteBarrier", "Number of effective G1 Post Write Barriers (after passing the XOR test)");
            g1EffectiveAfterNullPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1EffectiveAfterNullPostWriteBarrier", "Number of effective G1 Post Write Barriers (after passing the NULL test)");
            g1ExecutedPostWriteBarrierCounter = new SnippetCounter(countersWriteBarriers, "g1ExecutedPostWriteBarrier", "Number of executed G1 Post Write Barriers");
        }

        final SnippetCounter serialWriteBarrierCounter;
        final SnippetCounter g1AttemptedPreWriteBarrierCounter;
        final SnippetCounter g1EffectivePreWriteBarrierCounter;
        final SnippetCounter g1ExecutedPreWriteBarrierCounter;
        final SnippetCounter g1AttemptedPostWriteBarrierCounter;
        final SnippetCounter g1EffectiveAfterXORPostWriteBarrierCounter;
        final SnippetCounter g1EffectiveAfterNullPostWriteBarrierCounter;
        final SnippetCounter g1ExecutedPostWriteBarrierCounter;
    }

    public static final LocationIdentity GC_CARD_LOCATION = NamedLocationIdentity.mutable("GC-Card");
    public static final LocationIdentity GC_LOG_LOCATION = NamedLocationIdentity.mutable("GC-Log");
    public static final LocationIdentity GC_INDEX_LOCATION = NamedLocationIdentity.mutable("GC-Index");

    private static void serialWriteBarrier(Pointer ptr, Counters counters)
    {
        counters.serialWriteBarrierCounter.inc();
        final long startAddress = GraalHotSpotVMConfigNode.cardTableAddress();
        Word base = (Word) ptr.unsignedShiftRight(HotSpotReplacementsUtil.cardTableShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        if (((int) startAddress) == startAddress && GraalHotSpotVMConfigNode.isCardTableAddressConstant())
        {
            base.writeByte((int) startAddress, (byte) 0, GC_CARD_LOCATION);
        }
        else
        {
            base.writeByte(WordFactory.unsigned(startAddress), (byte) 0, GC_CARD_LOCATION);
        }
    }

    @Snippet
    public static void serialImpreciseWriteBarrier(Object object, @ConstantParameter Counters counters)
    {
        serialWriteBarrier(Word.objectToTrackedPointer(object), counters);
    }

    @Snippet
    public static void serialPreciseWriteBarrier(Address address, @ConstantParameter Counters counters)
    {
        serialWriteBarrier(Word.fromAddress(address), counters);
    }

    @Snippet
    public static void serialArrayRangeWriteBarrier(Address address, int length, @ConstantParameter int elementStride)
    {
        if (length == 0)
        {
            return;
        }
        int cardShift = HotSpotReplacementsUtil.cardTableShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        final long cardStart = GraalHotSpotVMConfigNode.cardTableAddress();
        long start = getPointerToFirstArrayElement(address, length, elementStride) >>> cardShift;
        long end = getPointerToLastArrayElement(address, length, elementStride) >>> cardShift;
        long count = end - start + 1;
        while (count-- > 0)
        {
            DirectStoreNode.storeBoolean((start + cardStart) + count, false, JavaKind.Boolean);
        }
    }

    @Snippet
    public static void g1PreWriteBarrier(Address address, Object object, Object expectedObject, @ConstantParameter boolean doLoad, @ConstantParameter boolean nullCheck, @ConstantParameter Register threadRegister, @ConstantParameter Counters counters)
    {
        if (nullCheck)
        {
            NullCheckNode.nullCheck(address);
        }
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Object fixedExpectedObject = FixedValueAnchorNode.getObject(expectedObject);
        Word field = Word.fromAddress(address);
        Pointer previousOop = Word.objectToTrackedPointer(fixedExpectedObject);
        byte markingValue = thread.readByte(HotSpotReplacementsUtil.g1SATBQueueMarkingOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        int gcCycle = 0;
        counters.g1AttemptedPreWriteBarrierCounter.inc();
        // If the concurrent marker is enabled, the barrier is issued.
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, markingValue != (byte) 0))
        {
            // If the previous value has to be loaded (before the write), the load is issued.
            // The load is always issued except the cases of CAS and referent field.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, doLoad))
            {
                previousOop = Word.objectToTrackedPointer(field.readObject(0, BarrierType.NONE));
            }
            counters.g1EffectivePreWriteBarrierCounter.inc();
            // If the previous value is null the barrier should not be issued.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, previousOop.notEqual(0)))
            {
                counters.g1ExecutedPreWriteBarrierCounter.inc();
                // If the thread-local SATB buffer is full issue a native call which will
                // initialize a new one and add the entry.
                Word indexAddress = thread.add(HotSpotReplacementsUtil.g1SATBQueueIndexOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
                Word indexValue = indexAddress.readWord(0);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, indexValue.notEqual(0)))
                {
                    Word bufferAddress = thread.readWord(HotSpotReplacementsUtil.g1SATBQueueBufferOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
                    Word nextIndex = indexValue.subtract(HotSpotReplacementsUtil.wordSize());
                    Word logAddress = bufferAddress.add(nextIndex);
                    // Log the object to be marked as well as update the SATB's buffer next index.
                    logAddress.writeWord(0, previousOop, GC_LOG_LOCATION);
                    indexAddress.writeWord(0, nextIndex, GC_INDEX_LOCATION);
                }
                else
                {
                    g1PreBarrierStub(G1WBPRECALL, previousOop.toObject());
                }
            }
        }
    }

    @Snippet
    public static void g1PostWriteBarrier(Address address, Object object, Object value, @ConstantParameter boolean usePrecise, @ConstantParameter Register threadRegister, @ConstantParameter Counters counters)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Object fixedValue = FixedValueAnchorNode.getObject(value);
        Pointer oop;
        if (usePrecise)
        {
            oop = Word.fromAddress(address);
        }
        else
        {
            oop = Word.objectToTrackedPointer(object);
        }
        int gcCycle = 0;
        Pointer writtenValue = Word.objectToTrackedPointer(fixedValue);
        // The result of the xor reveals whether the installed pointer crosses heap regions.
        // In case it does the write barrier has to be issued.
        final int logOfHeapRegionGrainBytes = GraalHotSpotVMConfigNode.logOfHeapRegionGrainBytes();
        UnsignedWord xorResult = (oop.xor(writtenValue)).unsignedShiftRight(logOfHeapRegionGrainBytes);

        // Calculate the address of the card to be enqueued to the
        // thread local card queue.
        UnsignedWord cardBase = oop.unsignedShiftRight(HotSpotReplacementsUtil.cardTableShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        final long startAddress = GraalHotSpotVMConfigNode.cardTableAddress();
        int displacement = 0;
        if (((int) startAddress) == startAddress && GraalHotSpotVMConfigNode.isCardTableAddressConstant())
        {
            displacement = (int) startAddress;
        }
        else
        {
            cardBase = cardBase.add(WordFactory.unsigned(startAddress));
        }
        Word cardAddress = (Word) cardBase.add(displacement);

        counters.g1AttemptedPostWriteBarrierCounter.inc();
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, xorResult.notEqual(0)))
        {
            counters.g1EffectiveAfterXORPostWriteBarrierCounter.inc();

            // If the written value is not null continue with the barrier addition.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, writtenValue.notEqual(0)))
            {
                byte cardByte = cardAddress.readByte(0, GC_CARD_LOCATION);
                counters.g1EffectiveAfterNullPostWriteBarrierCounter.inc();

                // If the card is already dirty, (hence already enqueued) skip the insertion.
                if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, cardByte != HotSpotReplacementsUtil.g1YoungCardValue(GraalHotSpotVMConfig.INJECTED_VMCONFIG)))
                {
                    MembarNode.memoryBarrier(MemoryBarriers.STORE_LOAD, GC_CARD_LOCATION);
                    byte cardByteReload = cardAddress.readByte(0, GC_CARD_LOCATION);
                    if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, cardByteReload != HotSpotReplacementsUtil.dirtyCardValue(GraalHotSpotVMConfig.INJECTED_VMCONFIG)))
                    {
                        cardAddress.writeByte(0, (byte) 0, GC_CARD_LOCATION);
                        counters.g1ExecutedPostWriteBarrierCounter.inc();

                        // If the thread local card queue is full, issue a native call which will
                        // initialize a new one and add the card entry.
                        Word indexAddress = thread.add(HotSpotReplacementsUtil.g1CardQueueIndexOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
                        Word indexValue = thread.readWord(HotSpotReplacementsUtil.g1CardQueueIndexOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
                        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, indexValue.notEqual(0)))
                        {
                            Word bufferAddress = thread.readWord(HotSpotReplacementsUtil.g1CardQueueBufferOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
                            Word nextIndex = indexValue.subtract(HotSpotReplacementsUtil.wordSize());
                            Word logAddress = bufferAddress.add(nextIndex);
                            // Log the object to be scanned as well as update
                            // the card queue's next index.
                            logAddress.writeWord(0, cardAddress, GC_LOG_LOCATION);
                            indexAddress.writeWord(0, nextIndex, GC_INDEX_LOCATION);
                        }
                        else
                        {
                            g1PostBarrierStub(G1WBPOSTCALL, cardAddress);
                        }
                    }
                }
            }
        }
    }

    @Snippet
    public static void g1ArrayRangePreWriteBarrier(Address address, int length, @ConstantParameter int elementStride, @ConstantParameter Register threadRegister)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        byte markingValue = thread.readByte(HotSpotReplacementsUtil.g1SATBQueueMarkingOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        // If the concurrent marker is not enabled or the vector length is zero, return.
        if (markingValue == (byte) 0 || length == 0)
        {
            return;
        }
        Word bufferAddress = thread.readWord(HotSpotReplacementsUtil.g1SATBQueueBufferOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        Word indexAddress = thread.add(HotSpotReplacementsUtil.g1SATBQueueIndexOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        long indexValue = indexAddress.readWord(0).rawValue();
        final int scale = HotSpotReplacementsUtil.arrayIndexScale(JavaKind.Object);
        long start = getPointerToFirstArrayElement(address, length, elementStride);

        for (int i = 0; i < length; i++)
        {
            Word arrElemPtr = WordFactory.pointer(start + i * scale);
            Pointer oop = Word.objectToTrackedPointer(arrElemPtr.readObject(0, BarrierType.NONE));
            if (oop.notEqual(0))
            {
                if (indexValue != 0)
                {
                    indexValue = indexValue - HotSpotReplacementsUtil.wordSize();
                    Word logAddress = bufferAddress.add(WordFactory.unsigned(indexValue));
                    // Log the object to be marked as well as update the SATB's buffer next index.
                    logAddress.writeWord(0, oop, GC_LOG_LOCATION);
                    indexAddress.writeWord(0, WordFactory.unsigned(indexValue), GC_INDEX_LOCATION);
                }
                else
                {
                    g1PreBarrierStub(G1WBPRECALL, oop.toObject());
                }
            }
        }
    }

    @Snippet
    public static void g1ArrayRangePostWriteBarrier(Address address, int length, @ConstantParameter int elementStride, @ConstantParameter Register threadRegister)
    {
        if (length == 0)
        {
            return;
        }
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Word bufferAddress = thread.readWord(HotSpotReplacementsUtil.g1CardQueueBufferOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        Word indexAddress = thread.add(HotSpotReplacementsUtil.g1CardQueueIndexOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        long indexValue = thread.readWord(HotSpotReplacementsUtil.g1CardQueueIndexOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG)).rawValue();

        int cardShift = HotSpotReplacementsUtil.cardTableShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        final long cardStart = GraalHotSpotVMConfigNode.cardTableAddress();
        long start = getPointerToFirstArrayElement(address, length, elementStride) >>> cardShift;
        long end = getPointerToLastArrayElement(address, length, elementStride) >>> cardShift;
        long count = end - start + 1;

        while (count-- > 0)
        {
            Word cardAddress = WordFactory.unsigned((start + cardStart) + count);
            byte cardByte = cardAddress.readByte(0, GC_CARD_LOCATION);
            // If the card is already dirty, (hence already enqueued) skip the insertion.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, cardByte != HotSpotReplacementsUtil.g1YoungCardValue(GraalHotSpotVMConfig.INJECTED_VMCONFIG)))
            {
                MembarNode.memoryBarrier(MemoryBarriers.STORE_LOAD, GC_CARD_LOCATION);
                byte cardByteReload = cardAddress.readByte(0, GC_CARD_LOCATION);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, cardByteReload != HotSpotReplacementsUtil.dirtyCardValue(GraalHotSpotVMConfig.INJECTED_VMCONFIG)))
                {
                    cardAddress.writeByte(0, (byte) 0, GC_CARD_LOCATION);
                    // If the thread local card queue is full, issue a native call which will
                    // initialize a new one and add the card entry.
                    if (indexValue != 0)
                    {
                        indexValue = indexValue - HotSpotReplacementsUtil.wordSize();
                        Word logAddress = bufferAddress.add(WordFactory.unsigned(indexValue));
                        // Log the object to be scanned as well as update
                        // the card queue's next index.
                        logAddress.writeWord(0, cardAddress, GC_LOG_LOCATION);
                        indexAddress.writeWord(0, WordFactory.unsigned(indexValue), GC_INDEX_LOCATION);
                    }
                    else
                    {
                        g1PostBarrierStub(G1WBPOSTCALL, cardAddress);
                    }
                }
            }
        }
    }

    private static long getPointerToFirstArrayElement(Address address, int length, int elementStride)
    {
        long result = Word.fromAddress(address).rawValue();
        if (elementStride < 0)
        {
            // the address points to the place after the last array element
            result = result + elementStride * length;
        }
        return result;
    }

    private static long getPointerToLastArrayElement(Address address, int length, int elementStride)
    {
        long result = Word.fromAddress(address).rawValue();
        if (elementStride < 0)
        {
            // the address points to the place after the last array element
            result = result + elementStride;
        }
        else
        {
            result = result + (length - 1) * elementStride;
        }
        return result;
    }

    public static final ForeignCallDescriptor G1WBPRECALL = new ForeignCallDescriptor("write_barrier_pre", void.class, Object.class);

    @NodeIntrinsic(ForeignCallNode.class)
    private static native void g1PreBarrierStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object object);

    public static final ForeignCallDescriptor G1WBPOSTCALL = new ForeignCallDescriptor("write_barrier_post", void.class, Word.class);

    @NodeIntrinsic(ForeignCallNode.class)
    public static native void g1PostBarrierStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Word card);

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo serialImpreciseWriteBarrier = snippet(WriteBarrierSnippets.class, "serialImpreciseWriteBarrier", GC_CARD_LOCATION);
        private final SnippetInfo serialPreciseWriteBarrier = snippet(WriteBarrierSnippets.class, "serialPreciseWriteBarrier", GC_CARD_LOCATION);
        private final SnippetInfo serialArrayRangeWriteBarrier = snippet(WriteBarrierSnippets.class, "serialArrayRangeWriteBarrier");
        private final SnippetInfo g1PreWriteBarrier = snippet(WriteBarrierSnippets.class, "g1PreWriteBarrier", GC_INDEX_LOCATION, GC_LOG_LOCATION);
        private final SnippetInfo g1ReferentReadBarrier = snippet(WriteBarrierSnippets.class, "g1PreWriteBarrier", GC_INDEX_LOCATION, GC_LOG_LOCATION);
        private final SnippetInfo g1PostWriteBarrier = snippet(WriteBarrierSnippets.class, "g1PostWriteBarrier", GC_CARD_LOCATION, GC_INDEX_LOCATION, GC_LOG_LOCATION);
        private final SnippetInfo g1ArrayRangePreWriteBarrier = snippet(WriteBarrierSnippets.class, "g1ArrayRangePreWriteBarrier", GC_INDEX_LOCATION, GC_LOG_LOCATION);
        private final SnippetInfo g1ArrayRangePostWriteBarrier = snippet(WriteBarrierSnippets.class, "g1ArrayRangePostWriteBarrier", GC_CARD_LOCATION, GC_INDEX_LOCATION, GC_LOG_LOCATION);

        private final CompressEncoding oopEncoding;
        private final Counters counters;

        public Templates(OptionValues options, SnippetCounter.Group.Factory factory, HotSpotProviders providers, TargetDescription target, CompressEncoding oopEncoding)
        {
            super(options, providers, providers.getSnippetReflection(), target);
            this.oopEncoding = oopEncoding;
            this.counters = new Counters(factory);
        }

        public void lower(SerialWriteBarrier writeBarrier, LoweringTool tool)
        {
            Arguments args;
            if (writeBarrier.usePrecise())
            {
                args = new Arguments(serialPreciseWriteBarrier, writeBarrier.graph().getGuardsStage(), tool.getLoweringStage());
                args.add("address", writeBarrier.getAddress());
            }
            else
            {
                args = new Arguments(serialImpreciseWriteBarrier, writeBarrier.graph().getGuardsStage(), tool.getLoweringStage());
                OffsetAddressNode address = (OffsetAddressNode) writeBarrier.getAddress();
                args.add("object", address.getBase());
            }
            args.addConst("counters", counters);
            template(writeBarrier, args).instantiate(providers.getMetaAccess(), writeBarrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(SerialArrayRangeWriteBarrier arrayRangeWriteBarrier, LoweringTool tool)
        {
            Arguments args = new Arguments(serialArrayRangeWriteBarrier, arrayRangeWriteBarrier.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("address", arrayRangeWriteBarrier.getAddress());
            args.add("length", arrayRangeWriteBarrier.getLength());
            args.addConst("elementStride", arrayRangeWriteBarrier.getElementStride());
            template(arrayRangeWriteBarrier, args).instantiate(providers.getMetaAccess(), arrayRangeWriteBarrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(G1PreWriteBarrier writeBarrierPre, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            Arguments args = new Arguments(g1PreWriteBarrier, writeBarrierPre.graph().getGuardsStage(), tool.getLoweringStage());
            AddressNode address = writeBarrierPre.getAddress();
            args.add("address", address);
            if (address instanceof OffsetAddressNode)
            {
                args.add("object", ((OffsetAddressNode) address).getBase());
            }
            else
            {
                args.add("object", null);
            }

            ValueNode expected = writeBarrierPre.getExpectedObject();
            if (expected != null && expected.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp)
            {
                expected = HotSpotCompressionNode.uncompress(expected, oopEncoding);
            }
            args.add("expectedObject", expected);

            args.addConst("doLoad", writeBarrierPre.doLoad());
            args.addConst("nullCheck", writeBarrierPre.getNullCheck());
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("counters", counters);
            template(writeBarrierPre, args).instantiate(providers.getMetaAccess(), writeBarrierPre, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(G1ReferentFieldReadBarrier readBarrier, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            Arguments args = new Arguments(g1ReferentReadBarrier, readBarrier.graph().getGuardsStage(), tool.getLoweringStage());
            AddressNode address = readBarrier.getAddress();
            args.add("address", address);
            if (address instanceof OffsetAddressNode)
            {
                args.add("object", ((OffsetAddressNode) address).getBase());
            }
            else
            {
                args.add("object", null);
            }

            ValueNode expected = readBarrier.getExpectedObject();
            if (expected != null && expected.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp)
            {
                expected = HotSpotCompressionNode.uncompress(expected, oopEncoding);
            }

            args.add("expectedObject", expected);
            args.addConst("doLoad", readBarrier.doLoad());
            args.addConst("nullCheck", false);
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("counters", counters);
            template(readBarrier, args).instantiate(providers.getMetaAccess(), readBarrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(G1PostWriteBarrier writeBarrierPost, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            StructuredGraph graph = writeBarrierPost.graph();
            if (writeBarrierPost.alwaysNull())
            {
                graph.removeFixed(writeBarrierPost);
                return;
            }
            Arguments args = new Arguments(g1PostWriteBarrier, graph.getGuardsStage(), tool.getLoweringStage());
            AddressNode address = writeBarrierPost.getAddress();
            args.add("address", address);
            if (address instanceof OffsetAddressNode)
            {
                args.add("object", ((OffsetAddressNode) address).getBase());
            }
            else
            {
                args.add("object", null);
            }

            ValueNode value = writeBarrierPost.getValue();
            if (value.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp)
            {
                value = HotSpotCompressionNode.uncompress(value, oopEncoding);
            }
            args.add("value", value);

            args.addConst("usePrecise", writeBarrierPost.usePrecise());
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("counters", counters);
            template(writeBarrierPost, args).instantiate(providers.getMetaAccess(), writeBarrierPost, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(G1ArrayRangePreWriteBarrier arrayRangeWriteBarrier, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            Arguments args = new Arguments(g1ArrayRangePreWriteBarrier, arrayRangeWriteBarrier.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("address", arrayRangeWriteBarrier.getAddress());
            args.add("length", arrayRangeWriteBarrier.getLength());
            args.addConst("elementStride", arrayRangeWriteBarrier.getElementStride());
            args.addConst("threadRegister", registers.getThreadRegister());
            template(arrayRangeWriteBarrier, args).instantiate(providers.getMetaAccess(), arrayRangeWriteBarrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(G1ArrayRangePostWriteBarrier arrayRangeWriteBarrier, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            Arguments args = new Arguments(g1ArrayRangePostWriteBarrier, arrayRangeWriteBarrier.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("address", arrayRangeWriteBarrier.getAddress());
            args.add("length", arrayRangeWriteBarrier.getLength());
            args.addConst("elementStride", arrayRangeWriteBarrier.getElementStride());
            args.addConst("threadRegister", registers.getThreadRegister());
            template(arrayRangeWriteBarrier, args).instantiate(providers.getMetaAccess(), arrayRangeWriteBarrier, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
}
