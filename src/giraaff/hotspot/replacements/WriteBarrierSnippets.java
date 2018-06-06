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
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node;
import giraaff.hotspot.HotSpotRuntime;
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
import giraaff.nodes.memory.HeapAccess;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.type.NarrowOopStamp;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.DirectStoreNode;
import giraaff.word.Word;

// @class WriteBarrierSnippets
public final class WriteBarrierSnippets implements Snippets
{
    // @def
    public static final LocationIdentity GC_CARD_LOCATION = NamedLocationIdentity.mutable("GC-Card");
    // @def
    public static final LocationIdentity GC_LOG_LOCATION = NamedLocationIdentity.mutable("GC-Log");
    // @def
    public static final LocationIdentity GC_INDEX_LOCATION = NamedLocationIdentity.mutable("GC-Index");

    private static void serialWriteBarrier(Pointer __ptr)
    {
        final long __startAddress = GraalHotSpotVMConfigNode.cardTableAddress();
        Word __base = (Word) __ptr.unsignedShiftRight(HotSpotRuntime.cardTableShift);
        if (((int) __startAddress) == __startAddress && GraalHotSpotVMConfigNode.isCardTableAddressConstant())
        {
            __base.writeByte((int) __startAddress, (byte) 0, GC_CARD_LOCATION);
        }
        else
        {
            __base.writeByte(WordFactory.unsigned(__startAddress), (byte) 0, GC_CARD_LOCATION);
        }
    }

    @Snippet
    public static void serialImpreciseWriteBarrier(Object __object)
    {
        serialWriteBarrier(Word.objectToTrackedPointer(__object));
    }

    @Snippet
    public static void serialPreciseWriteBarrier(AddressNode.Address __address)
    {
        serialWriteBarrier(Word.fromAddress(__address));
    }

    @Snippet
    public static void serialArrayRangeWriteBarrier(AddressNode.Address __address, int __length, @Snippet.ConstantParameter int __elementStride)
    {
        if (__length == 0)
        {
            return;
        }
        int __cardShift = HotSpotRuntime.cardTableShift;
        final long __cardStart = GraalHotSpotVMConfigNode.cardTableAddress();
        long __start = getPointerToFirstArrayElement(__address, __length, __elementStride) >>> __cardShift;
        long __end = getPointerToLastArrayElement(__address, __length, __elementStride) >>> __cardShift;
        long __count = __end - __start + 1;
        while (__count-- > 0)
        {
            DirectStoreNode.storeBoolean((__start + __cardStart) + __count, false, JavaKind.Boolean);
        }
    }

    @Snippet
    public static void g1PreWriteBarrier(AddressNode.Address __address, Object __object, Object __expectedObject, @Snippet.ConstantParameter boolean __doLoad, @Snippet.ConstantParameter boolean __nullCheck, @Snippet.ConstantParameter Register __threadRegister)
    {
        if (__nullCheck)
        {
            NullCheckNode.nullCheck(__address);
        }
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        Object __fixedExpectedObject = FixedValueAnchorNode.getObject(__expectedObject);
        Word __field = Word.fromAddress(__address);
        Pointer __previousOop = Word.objectToTrackedPointer(__fixedExpectedObject);
        byte __markingValue = __thread.readByte(HotSpotRuntime.g1SATBQueueMarkingOffset);
        int __gcCycle = 0;
        // If the concurrent marker is enabled, the barrier is issued.
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __markingValue != (byte) 0))
        {
            // If the previous value has to be loaded (before the write), the load is issued.
            // The load is always issued except the cases of CAS and referent field.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.LIKELY_PROBABILITY, __doLoad))
            {
                __previousOop = Word.objectToTrackedPointer(__field.readObject(0, HeapAccess.BarrierType.NONE));
            }
            // If the previous value is null the barrier should not be issued.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __previousOop.notEqual(0)))
            {
                // If the thread-local SATB buffer is full, issue a native call, which will initialize a new one and add the entry.
                Word __indexAddress = __thread.add(HotSpotRuntime.g1SATBQueueIndexOffset);
                Word __indexValue = __indexAddress.readWord(0);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __indexValue.notEqual(0)))
                {
                    Word __bufferAddress = __thread.readWord(HotSpotRuntime.g1SATBQueueBufferOffset);
                    Word __nextIndex = __indexValue.subtract(HotSpotReplacementsUtil.wordSize());
                    Word __logAddress = __bufferAddress.add(__nextIndex);
                    // Log the object to be marked as well as update the SATB's buffer next index.
                    __logAddress.writeWord(0, __previousOop, GC_LOG_LOCATION);
                    __indexAddress.writeWord(0, __nextIndex, GC_INDEX_LOCATION);
                }
                else
                {
                    g1PreBarrierStub(G1WBPRECALL, __previousOop.toObject());
                }
            }
        }
    }

    @Snippet
    public static void g1PostWriteBarrier(AddressNode.Address __address, Object __object, Object __value, @Snippet.ConstantParameter boolean __usePrecise, @Snippet.ConstantParameter Register __threadRegister)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        Object __fixedValue = FixedValueAnchorNode.getObject(__value);
        Pointer __oop;
        if (__usePrecise)
        {
            __oop = Word.fromAddress(__address);
        }
        else
        {
            __oop = Word.objectToTrackedPointer(__object);
        }
        int __gcCycle = 0;
        Pointer __writtenValue = Word.objectToTrackedPointer(__fixedValue);
        // The result of the xor reveals whether the installed pointer crosses heap regions.
        // In case it does the write barrier has to be issued.
        UnsignedWord __xorResult = (__oop.xor(__writtenValue)).unsignedShiftRight(GraalHotSpotVMConfigNode.logOfHeapRegionGrainBytes());

        // Calculate the address of the card to be enqueued to the thread local card queue.
        UnsignedWord __cardBase = __oop.unsignedShiftRight(HotSpotRuntime.cardTableShift);
        final long __startAddress = GraalHotSpotVMConfigNode.cardTableAddress();
        int __displacement = 0;
        if (((int) __startAddress) == __startAddress && GraalHotSpotVMConfigNode.isCardTableAddressConstant())
        {
            __displacement = (int) __startAddress;
        }
        else
        {
            __cardBase = __cardBase.add(WordFactory.unsigned(__startAddress));
        }
        Word __cardAddress = (Word) __cardBase.add(__displacement);

        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __xorResult.notEqual(0)))
        {
            // If the written value is not null continue with the barrier addition.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __writtenValue.notEqual(0)))
            {
                byte __cardByte = __cardAddress.readByte(0, GC_CARD_LOCATION);

                // If the card is already dirty, (hence already enqueued) skip the insertion.
                if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __cardByte != HotSpotRuntime.g1YoungCardValue))
                {
                    MembarNode.memoryBarrier(MemoryBarriers.STORE_LOAD, GC_CARD_LOCATION);
                    byte __cardByteReload = __cardAddress.readByte(0, GC_CARD_LOCATION);
                    if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __cardByteReload != HotSpotRuntime.dirtyCardValue))
                    {
                        __cardAddress.writeByte(0, (byte) 0, GC_CARD_LOCATION);

                        // If the thread-local card queue is full, issue a native call, which will initialize a new one and add the card entry.
                        Word __indexAddress = __thread.add(HotSpotRuntime.g1CardQueueIndexOffset);
                        Word __indexValue = __thread.readWord(HotSpotRuntime.g1CardQueueIndexOffset);
                        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __indexValue.notEqual(0)))
                        {
                            Word __bufferAddress = __thread.readWord(HotSpotRuntime.g1CardQueueBufferOffset);
                            Word __nextIndex = __indexValue.subtract(HotSpotReplacementsUtil.wordSize());
                            Word __logAddress = __bufferAddress.add(__nextIndex);
                            // Log the object to be scanned as well as update the card queue's next index.
                            __logAddress.writeWord(0, __cardAddress, GC_LOG_LOCATION);
                            __indexAddress.writeWord(0, __nextIndex, GC_INDEX_LOCATION);
                        }
                        else
                        {
                            g1PostBarrierStub(G1WBPOSTCALL, __cardAddress);
                        }
                    }
                }
            }
        }
    }

    @Snippet
    public static void g1ArrayRangePreWriteBarrier(AddressNode.Address __address, int __length, @Snippet.ConstantParameter int __elementStride, @Snippet.ConstantParameter Register __threadRegister)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        byte __markingValue = __thread.readByte(HotSpotRuntime.g1SATBQueueMarkingOffset);
        // If the concurrent marker is not enabled or the vector length is zero, return.
        if (__markingValue == (byte) 0 || __length == 0)
        {
            return;
        }
        Word __bufferAddress = __thread.readWord(HotSpotRuntime.g1SATBQueueBufferOffset);
        Word __indexAddress = __thread.add(HotSpotRuntime.g1SATBQueueIndexOffset);
        long __indexValue = __indexAddress.readWord(0).rawValue();
        final int __scale = HotSpotReplacementsUtil.arrayIndexScale(JavaKind.Object);
        long __start = getPointerToFirstArrayElement(__address, __length, __elementStride);

        for (int __i = 0; __i < __length; __i++)
        {
            Word __arrElemPtr = WordFactory.pointer(__start + __i * __scale);
            Pointer __oop = Word.objectToTrackedPointer(__arrElemPtr.readObject(0, HeapAccess.BarrierType.NONE));
            if (__oop.notEqual(0))
            {
                if (__indexValue != 0)
                {
                    __indexValue = __indexValue - HotSpotReplacementsUtil.wordSize();
                    Word __logAddress = __bufferAddress.add(WordFactory.unsigned(__indexValue));
                    // Log the object to be marked as well as update the SATB's buffer next index.
                    __logAddress.writeWord(0, __oop, GC_LOG_LOCATION);
                    __indexAddress.writeWord(0, WordFactory.unsigned(__indexValue), GC_INDEX_LOCATION);
                }
                else
                {
                    g1PreBarrierStub(G1WBPRECALL, __oop.toObject());
                }
            }
        }
    }

    @Snippet
    public static void g1ArrayRangePostWriteBarrier(AddressNode.Address __address, int __length, @Snippet.ConstantParameter int __elementStride, @Snippet.ConstantParameter Register __threadRegister)
    {
        if (__length == 0)
        {
            return;
        }
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        Word __bufferAddress = __thread.readWord(HotSpotRuntime.g1CardQueueBufferOffset);
        Word __indexAddress = __thread.add(HotSpotRuntime.g1CardQueueIndexOffset);
        long __indexValue = __thread.readWord(HotSpotRuntime.g1CardQueueIndexOffset).rawValue();

        int __cardShift = HotSpotRuntime.cardTableShift;
        final long __cardStart = GraalHotSpotVMConfigNode.cardTableAddress();
        long __start = getPointerToFirstArrayElement(__address, __length, __elementStride) >>> __cardShift;
        long __end = getPointerToLastArrayElement(__address, __length, __elementStride) >>> __cardShift;
        long __count = __end - __start + 1;

        while (__count-- > 0)
        {
            Word __cardAddress = WordFactory.unsigned((__start + __cardStart) + __count);
            byte __cardByte = __cardAddress.readByte(0, GC_CARD_LOCATION);
            // If the card is already dirty, (hence already enqueued) skip the insertion.
            if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __cardByte != HotSpotRuntime.g1YoungCardValue))
            {
                MembarNode.memoryBarrier(MemoryBarriers.STORE_LOAD, GC_CARD_LOCATION);
                byte __cardByteReload = __cardAddress.readByte(0, GC_CARD_LOCATION);
                if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __cardByteReload != HotSpotRuntime.dirtyCardValue))
                {
                    __cardAddress.writeByte(0, (byte) 0, GC_CARD_LOCATION);
                    // If the thread local card queue is full, issue a native call which will initialize
                    // a new one and add the card entry.
                    if (__indexValue != 0)
                    {
                        __indexValue = __indexValue - HotSpotReplacementsUtil.wordSize();
                        Word __logAddress = __bufferAddress.add(WordFactory.unsigned(__indexValue));
                        // Log the object to be scanned as well as update the card queue's next index.
                        __logAddress.writeWord(0, __cardAddress, GC_LOG_LOCATION);
                        __indexAddress.writeWord(0, WordFactory.unsigned(__indexValue), GC_INDEX_LOCATION);
                    }
                    else
                    {
                        g1PostBarrierStub(G1WBPOSTCALL, __cardAddress);
                    }
                }
            }
        }
    }

    private static long getPointerToFirstArrayElement(AddressNode.Address __address, int __length, int __elementStride)
    {
        long __result = Word.fromAddress(__address).rawValue();
        if (__elementStride < 0)
        {
            // the address points to the place after the last array element
            __result = __result + __elementStride * __length;
        }
        return __result;
    }

    private static long getPointerToLastArrayElement(AddressNode.Address __address, int __length, int __elementStride)
    {
        long __result = Word.fromAddress(__address).rawValue();
        if (__elementStride < 0)
        {
            // the address points to the place after the last array element
            __result = __result + __elementStride;
        }
        else
        {
            __result = __result + (__length - 1) * __elementStride;
        }
        return __result;
    }

    // @def
    public static final ForeignCallDescriptor G1WBPRECALL = new ForeignCallDescriptor("write_barrier_pre", void.class, Object.class);

    @Node.NodeIntrinsic(ForeignCallNode.class)
    private static native void g1PreBarrierStub(@Node.ConstantNodeParameter ForeignCallDescriptor __descriptor, Object __object);

    // @def
    public static final ForeignCallDescriptor G1WBPOSTCALL = new ForeignCallDescriptor("write_barrier_post", void.class, Word.class);

    @Node.NodeIntrinsic(ForeignCallNode.class)
    public static native void g1PostBarrierStub(@Node.ConstantNodeParameter ForeignCallDescriptor __descriptor, Word __card);

    // @class WriteBarrierSnippets.WriteBarrierTemplates
    public static final class WriteBarrierTemplates extends SnippetTemplate.AbstractTemplates
    {
        // @field
        private final SnippetTemplate.SnippetInfo ___serialImpreciseWriteBarrier = snippet(WriteBarrierSnippets.class, "serialImpreciseWriteBarrier", GC_CARD_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___serialPreciseWriteBarrier = snippet(WriteBarrierSnippets.class, "serialPreciseWriteBarrier", GC_CARD_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___serialArrayRangeWriteBarrier = snippet(WriteBarrierSnippets.class, "serialArrayRangeWriteBarrier");
        // @field
        private final SnippetTemplate.SnippetInfo ___g1PreWriteBarrier = snippet(WriteBarrierSnippets.class, "g1PreWriteBarrier", GC_INDEX_LOCATION, GC_LOG_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___g1ReferentReadBarrier = snippet(WriteBarrierSnippets.class, "g1PreWriteBarrier", GC_INDEX_LOCATION, GC_LOG_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___g1PostWriteBarrier = snippet(WriteBarrierSnippets.class, "g1PostWriteBarrier", GC_CARD_LOCATION, GC_INDEX_LOCATION, GC_LOG_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___g1ArrayRangePreWriteBarrier = snippet(WriteBarrierSnippets.class, "g1ArrayRangePreWriteBarrier", GC_INDEX_LOCATION, GC_LOG_LOCATION);
        // @field
        private final SnippetTemplate.SnippetInfo ___g1ArrayRangePostWriteBarrier = snippet(WriteBarrierSnippets.class, "g1ArrayRangePostWriteBarrier", GC_CARD_LOCATION, GC_INDEX_LOCATION, GC_LOG_LOCATION);

        // @field
        private final CompressEncoding ___oopEncoding;

        // @cons WriteBarrierSnippets.WriteBarrierTemplates
        public WriteBarrierTemplates(HotSpotProviders __providers, TargetDescription __target, CompressEncoding __oopEncoding)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
            this.___oopEncoding = __oopEncoding;
        }

        public void lower(SerialWriteBarrier __writeBarrier, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args;
            if (__writeBarrier.usePrecise())
            {
                __args = new SnippetTemplate.Arguments(this.___serialPreciseWriteBarrier, __writeBarrier.graph().getGuardsStage(), __tool.getLoweringStage());
                __args.add("address", __writeBarrier.getAddress());
            }
            else
            {
                __args = new SnippetTemplate.Arguments(this.___serialImpreciseWriteBarrier, __writeBarrier.graph().getGuardsStage(), __tool.getLoweringStage());
                OffsetAddressNode __address = (OffsetAddressNode) __writeBarrier.getAddress();
                __args.add("object", __address.getBase());
            }
            template(__writeBarrier, __args).instantiate(this.___providers.getMetaAccess(), __writeBarrier, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(SerialArrayRangeWriteBarrier __arrayRangeWriteBarrier, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___serialArrayRangeWriteBarrier, __arrayRangeWriteBarrier.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("address", __arrayRangeWriteBarrier.getAddress());
            __args.add("length", __arrayRangeWriteBarrier.getLength());
            __args.addConst("elementStride", __arrayRangeWriteBarrier.getElementStride());
            template(__arrayRangeWriteBarrier, __args).instantiate(this.___providers.getMetaAccess(), __arrayRangeWriteBarrier, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(G1PreWriteBarrier __writeBarrierPre, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___g1PreWriteBarrier, __writeBarrierPre.graph().getGuardsStage(), __tool.getLoweringStage());
            AddressNode __address = __writeBarrierPre.getAddress();
            __args.add("address", __address);
            if (__address instanceof OffsetAddressNode)
            {
                __args.add("object", ((OffsetAddressNode) __address).getBase());
            }
            else
            {
                __args.add("object", null);
            }

            ValueNode __expected = __writeBarrierPre.getExpectedObject();
            if (__expected != null && __expected.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp)
            {
                __expected = HotSpotCompressionNode.uncompress(__expected, this.___oopEncoding);
            }
            __args.add("expectedObject", __expected);

            __args.addConst("doLoad", __writeBarrierPre.doLoad());
            __args.addConst("nullCheck", __writeBarrierPre.getNullCheck());
            __args.addConst("threadRegister", __registers.getThreadRegister());
            template(__writeBarrierPre, __args).instantiate(this.___providers.getMetaAccess(), __writeBarrierPre, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(G1ReferentFieldReadBarrier __readBarrier, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___g1ReferentReadBarrier, __readBarrier.graph().getGuardsStage(), __tool.getLoweringStage());
            AddressNode __address = __readBarrier.getAddress();
            __args.add("address", __address);
            if (__address instanceof OffsetAddressNode)
            {
                __args.add("object", ((OffsetAddressNode) __address).getBase());
            }
            else
            {
                __args.add("object", null);
            }

            ValueNode __expected = __readBarrier.getExpectedObject();
            if (__expected != null && __expected.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp)
            {
                __expected = HotSpotCompressionNode.uncompress(__expected, this.___oopEncoding);
            }

            __args.add("expectedObject", __expected);
            __args.addConst("doLoad", __readBarrier.doLoad());
            __args.addConst("nullCheck", false);
            __args.addConst("threadRegister", __registers.getThreadRegister());
            template(__readBarrier, __args).instantiate(this.___providers.getMetaAccess(), __readBarrier, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(G1PostWriteBarrier __writeBarrierPost, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            StructuredGraph __graph = __writeBarrierPost.graph();
            if (__writeBarrierPost.alwaysNull())
            {
                __graph.removeFixed(__writeBarrierPost);
                return;
            }
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___g1PostWriteBarrier, __graph.getGuardsStage(), __tool.getLoweringStage());
            AddressNode __address = __writeBarrierPost.getAddress();
            __args.add("address", __address);
            if (__address instanceof OffsetAddressNode)
            {
                __args.add("object", ((OffsetAddressNode) __address).getBase());
            }
            else
            {
                __args.add("object", null);
            }

            ValueNode __value = __writeBarrierPost.getValue();
            if (__value.stamp(NodeView.DEFAULT) instanceof NarrowOopStamp)
            {
                __value = HotSpotCompressionNode.uncompress(__value, this.___oopEncoding);
            }
            __args.add("value", __value);

            __args.addConst("usePrecise", __writeBarrierPost.usePrecise());
            __args.addConst("threadRegister", __registers.getThreadRegister());
            template(__writeBarrierPost, __args).instantiate(this.___providers.getMetaAccess(), __writeBarrierPost, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(G1ArrayRangePreWriteBarrier __arrayRangeWriteBarrier, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___g1ArrayRangePreWriteBarrier, __arrayRangeWriteBarrier.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("address", __arrayRangeWriteBarrier.getAddress());
            __args.add("length", __arrayRangeWriteBarrier.getLength());
            __args.addConst("elementStride", __arrayRangeWriteBarrier.getElementStride());
            __args.addConst("threadRegister", __registers.getThreadRegister());
            template(__arrayRangeWriteBarrier, __args).instantiate(this.___providers.getMetaAccess(), __arrayRangeWriteBarrier, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(G1ArrayRangePostWriteBarrier __arrayRangeWriteBarrier, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___g1ArrayRangePostWriteBarrier, __arrayRangeWriteBarrier.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("address", __arrayRangeWriteBarrier.getAddress());
            __args.add("length", __arrayRangeWriteBarrier.getLength());
            __args.addConst("elementStride", __arrayRangeWriteBarrier.getElementStride());
            __args.addConst("threadRegister", __registers.getThreadRegister());
            template(__arrayRangeWriteBarrier, __args).instantiate(this.___providers.getMetaAccess(), __arrayRangeWriteBarrier, SnippetTemplate.DEFAULT_REPLACER, __args);
        }
    }
}
