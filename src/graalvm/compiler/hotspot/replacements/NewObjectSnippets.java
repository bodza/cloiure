package graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.MemoryBarriers;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.WordFactory;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.api.replacements.Snippet.VarargsParameter;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.calc.UnsignedMath;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.meta.HotSpotRegistersProvider;
import graalvm.compiler.hotspot.nodes.DimensionsNode;
import graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyFixedNode;
import graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode;
import graalvm.compiler.hotspot.nodes.type.KlassPointerStamp;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.PiArrayNode;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.PrefetchAllocateNode;
import graalvm.compiler.nodes.SnippetAnchorNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.debug.VerifyHeapNode;
import graalvm.compiler.nodes.extended.BranchProbabilityNode;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.nodes.extended.MembarNode;
import graalvm.compiler.nodes.java.DynamicNewArrayNode;
import graalvm.compiler.nodes.java.DynamicNewInstanceNode;
import graalvm.compiler.nodes.java.NewArrayNode;
import graalvm.compiler.nodes.java.NewInstanceNode;
import graalvm.compiler.nodes.java.NewMultiArrayNode;
import graalvm.compiler.nodes.memory.address.OffsetAddressNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetCounter;
import graalvm.compiler.replacements.SnippetCounter.Group;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.replacements.nodes.CStringConstant;
import graalvm.compiler.replacements.nodes.ExplodeLoopNode;
import graalvm.compiler.word.Word;

/**
 * Snippets used for implementing NEW, ANEWARRAY and NEWARRAY.
 */
public class NewObjectSnippets implements Snippets
{
    enum ProfileContext
    {
        AllocatingMethod,
        InstanceOrArray,
        AllocatedType,
        AllocatedTypesInMethod,
        Total
    }

    public static void emitPrefetchAllocate(Word address, boolean isArray)
    {
        GraalHotSpotVMConfig config = HotSpotReplacementsUtil.config(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        if (config.allocatePrefetchStyle > 0)
        {
            // Insert a prefetch for each allocation only on the fast-path
            // Generate several prefetch instructions.
            int lines = isArray ? config.allocatePrefetchLines : config.allocateInstancePrefetchLines;
            int stepSize = config.allocatePrefetchStepSize;
            int distance = config.allocatePrefetchDistance;
            ExplodeLoopNode.explodeLoop();
            for (int i = 0; i < lines; i++)
            {
                PrefetchAllocateNode.prefetch(OffsetAddressNode.address(address, distance));
                distance += stepSize;
            }
        }
    }

    @Snippet
    public static Object allocateInstance(@ConstantParameter int size, KlassPointer hub, Word prototypeMarkWord, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter boolean constantSize, @ConstantParameter String typeContext, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        return PiNode.piCastToSnippetReplaceeStamp(allocateInstanceHelper(size, hub, prototypeMarkWord, fillContents, threadRegister, constantSize, typeContext, options, counters));
    }

    public static Object allocateInstanceHelper(int size, KlassPointer hub, Word prototypeMarkWord, boolean fillContents, Register threadRegister, boolean constantSize, String typeContext, OptionValues options, Counters counters)
    {
        Object result;
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Word top = HotSpotReplacementsUtil.readTlabTop(thread);
        Word end = HotSpotReplacementsUtil.readTlabEnd(thread);
        Word newTop = top.add(size);
        if (HotSpotReplacementsUtil.useTLAB(GraalHotSpotVMConfig.INJECTED_VMCONFIG) && BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, newTop.belowOrEqual(end)))
        {
            HotSpotReplacementsUtil.writeTlabTop(thread, newTop);
            emitPrefetchAllocate(newTop, false);
            result = formatObject(hub, size, top, prototypeMarkWord, fillContents, constantSize, counters);
        }
        else
        {
            if (counters != null && counters.stub != null)
            {
                counters.stub.inc();
            }
            result = newInstance(HotSpotBackend.NEW_INSTANCE, hub);
        }
        return HotSpotReplacementsUtil.verifyOop(result);
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newInstance(@ConstantNodeParameter ForeignCallDescriptor descriptor, KlassPointer hub);

    @Snippet
    public static Object allocateInstancePIC(@ConstantParameter int size, KlassPointer hub, Word prototypeMarkWord, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter boolean constantSize, @ConstantParameter String typeContext, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        // Klass must be initialized by the time the first instance is allocated, therefore we can
        // just load it from the corresponding cell and avoid the resolution check. We have to use a
        // fixed load though, to prevent it from floating above the initialization.
        KlassPointer picHub = LoadConstantIndirectlyFixedNode.loadKlass(hub);
        return PiNode.piCastToSnippetReplaceeStamp(allocateInstanceHelper(size, picHub, prototypeMarkWord, fillContents, threadRegister, constantSize, typeContext, options, counters));
    }

    @Snippet
    public static Object allocateInstanceDynamic(Class<?> type, Class<?> classClass, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, type == null))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        Class<?> nonNullType = PiNode.piCastNonNullClass(type, SnippetAnchorNode.anchor());

        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, DynamicNewInstanceNode.throwsInstantiationException(type, classClass)))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }

        return PiNode.piCastToSnippetReplaceeStamp(allocateInstanceDynamicHelper(type, fillContents, threadRegister, options, counters, nonNullType));
    }

    private static Object allocateInstanceDynamicHelper(Class<?> type, boolean fillContents, Register threadRegister, OptionValues options, Counters counters, Class<?> nonNullType)
    {
        KlassPointer hub = ClassGetHubNode.readClass(nonNullType);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !hub.isNull()))
        {
            KlassPointer nonNullHub = ClassGetHubNode.piCastNonNull(hub, SnippetAnchorNode.anchor());

            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, HotSpotReplacementsUtil.isInstanceKlassFullyInitialized(nonNullHub)))
            {
                int layoutHelper = HotSpotReplacementsUtil.readLayoutHelper(nonNullHub);
                /*
                 * src/share/vm/oops/klass.hpp: For instances, layout helper is a positive number,
                 * the instance size. This size is already passed through align_object_size and
                 * scaled to bytes. The low order bit is set if instances of this class cannot be
                 * allocated using the fastpath.
                 */
                if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, (layoutHelper & 1) == 0))
                {
                    Word prototypeMarkWord = nonNullHub.readWord(HotSpotReplacementsUtil.prototypeMarkWordOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION);
                    /*
                     * FIXME(je,ds): we should actually pass typeContext instead of "" but late
                     * binding of parameters is not yet supported by the GraphBuilderPlugin system.
                     */
                    return allocateInstanceHelper(layoutHelper, nonNullHub, prototypeMarkWord, fillContents, threadRegister, false, "", options, counters);
                }
            }
        }
        return dynamicNewInstanceStub(type);
    }

    /**
     * Maximum array length for which fast path allocation is used.
     */
    public static final int MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH = 0x00FFFFFF;

    @Snippet
    public static Object allocatePrimitiveArrayPIC(KlassPointer hub, int length, Word prototypeMarkWord, @ConstantParameter int headerSize, @ConstantParameter int log2ElementSize, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter boolean maybeUnroll, @ConstantParameter String typeContext, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        // Primitive array types are eagerly pre-resolved. We can use a floating load.
        KlassPointer picHub = LoadConstantIndirectlyNode.loadKlass(hub);
        return allocateArrayImpl(picHub, length, prototypeMarkWord, headerSize, log2ElementSize, fillContents, threadRegister, maybeUnroll, typeContext, false, options, counters);
    }

    @Snippet
    public static Object allocateArrayPIC(KlassPointer hub, int length, Word prototypeMarkWord, @ConstantParameter int headerSize, @ConstantParameter int log2ElementSize, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter boolean maybeUnroll, @ConstantParameter String typeContext, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        // Array type would be resolved by dominating resolution.
        KlassPointer picHub = LoadConstantIndirectlyFixedNode.loadKlass(hub);
        return allocateArrayImpl(picHub, length, prototypeMarkWord, headerSize, log2ElementSize, fillContents, threadRegister, maybeUnroll, typeContext, false, options, counters);
    }

    @Snippet
    public static Object allocateArray(KlassPointer hub, int length, Word prototypeMarkWord, @ConstantParameter int headerSize, @ConstantParameter int log2ElementSize, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter boolean maybeUnroll, @ConstantParameter String typeContext, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        Object result = allocateArrayImpl(hub, length, prototypeMarkWord, headerSize, log2ElementSize, fillContents, threadRegister, maybeUnroll, typeContext, false, options, counters);
        return PiArrayNode.piArrayCastToSnippetReplaceeStamp(HotSpotReplacementsUtil.verifyOop(result), length);
    }

    private static Object allocateArrayImpl(KlassPointer hub, int length, Word prototypeMarkWord, int headerSize, int log2ElementSize, boolean fillContents, Register threadRegister, boolean maybeUnroll, String typeContext, boolean skipNegativeCheck, OptionValues options, Counters counters)
    {
        Object result;
        int allocationSize = HotSpotReplacementsUtil.arrayAllocationSize(length, headerSize, log2ElementSize);
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Word top = HotSpotReplacementsUtil.readTlabTop(thread);
        Word end = HotSpotReplacementsUtil.readTlabEnd(thread);
        Word newTop = top.add(allocationSize);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, skipNegativeCheck || UnsignedMath.belowThan(length, MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH)) && HotSpotReplacementsUtil.useTLAB(GraalHotSpotVMConfig.INJECTED_VMCONFIG) && BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, newTop.belowOrEqual(end)))
        {
            HotSpotReplacementsUtil.writeTlabTop(thread, newTop);
            emitPrefetchAllocate(newTop, true);
            if (counters != null && counters.arrayLoopInit != null)
            {
                counters.arrayLoopInit.inc();
            }
            result = formatArray(hub, allocationSize, length, headerSize, top, prototypeMarkWord, fillContents, maybeUnroll, counters);
        }
        else
        {
            result = newArray(HotSpotBackend.NEW_ARRAY, hub, length, fillContents);
        }
        return result;
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newArray(@ConstantNodeParameter ForeignCallDescriptor descriptor, KlassPointer hub, int length, boolean fillContents);

    public static final ForeignCallDescriptor DYNAMIC_NEW_ARRAY = new ForeignCallDescriptor("dynamic_new_array", Object.class, Class.class, int.class);
    public static final ForeignCallDescriptor DYNAMIC_NEW_INSTANCE = new ForeignCallDescriptor("dynamic_new_instance", Object.class, Class.class);

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object dynamicNewArrayStub(@ConstantNodeParameter ForeignCallDescriptor descriptor, Class<?> elementType, int length);

    public static Object dynamicNewInstanceStub(Class<?> elementType)
    {
        return dynamicNewInstanceStubCall(DYNAMIC_NEW_INSTANCE, elementType);
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object dynamicNewInstanceStubCall(@ConstantNodeParameter ForeignCallDescriptor descriptor, Class<?> elementType);

    @Snippet
    public static Object allocateArrayDynamic(Class<?> elementType, Class<?> voidClass, int length, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter JavaKind knownElementKind, @ConstantParameter int knownLayoutHelper, Word prototypeMarkWord, @ConstantParameter OptionValues options, @ConstantParameter Counters counters)
    {
        Object result = allocateArrayDynamicImpl(elementType, voidClass, length, fillContents, threadRegister, knownElementKind, knownLayoutHelper, prototypeMarkWord, options, counters);
        return result;
    }

    private static Object allocateArrayDynamicImpl(Class<?> elementType, Class<?> voidClass, int length, boolean fillContents, Register threadRegister, JavaKind knownElementKind, int knownLayoutHelper, Word prototypeMarkWord, OptionValues options, Counters counters)
    {
        /*
         * We only need the dynamic check for void when we have no static information from
         * knownElementKind.
         */
        if (knownElementKind == JavaKind.Illegal && BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, elementType == null || DynamicNewArrayNode.throwsIllegalArgumentException(elementType, voidClass)))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }

        KlassPointer klass = HotSpotReplacementsUtil.loadKlassFromObject(elementType, HotSpotReplacementsUtil.arrayKlassOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.CLASS_ARRAY_KLASS_LOCATION);
        if (klass.isNull())
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        KlassPointer nonNullKlass = ClassGetHubNode.piCastNonNull(klass, SnippetAnchorNode.anchor());

        if (length < 0)
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        int layoutHelper;
        if (knownElementKind == JavaKind.Illegal)
        {
            layoutHelper = HotSpotReplacementsUtil.readLayoutHelper(nonNullKlass);
        }
        else
        {
            layoutHelper = knownLayoutHelper;
        }
        // from src/share/vm/oops/klass.hpp:
        //
        // For arrays, layout helper is a negative number, containing four
        // distinct bytes, as follows:
        //    MSB:[tag, hsz, ebt, log2(esz)]:LSB
        // where:
        //    tag is 0x80 if the elements are oops, 0xC0 if non-oops
        //    hsz is array header size in bytes (i.e., offset of first element)
        //    ebt is the BasicType of the elements
        //    esz is the element size in bytes

        int headerSize = (layoutHelper >> HotSpotReplacementsUtil.layoutHelperHeaderSizeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) & HotSpotReplacementsUtil.layoutHelperHeaderSizeMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        int log2ElementSize = (layoutHelper >> HotSpotReplacementsUtil.layoutHelperLog2ElementSizeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) & HotSpotReplacementsUtil.layoutHelperLog2ElementSizeMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG);

        Object result = allocateArrayImpl(nonNullKlass, length, prototypeMarkWord, headerSize, log2ElementSize, fillContents, threadRegister, false, "dynamic type", true, options, counters);
        return PiArrayNode.piArrayCastToSnippetReplaceeStamp(HotSpotReplacementsUtil.verifyOop(result), length);
    }

    /**
     * Calls the runtime stub for implementing MULTIANEWARRAY.
     */
    @Snippet
    public static Object newmultiarray(KlassPointer hub, @ConstantParameter int rank, @VarargsParameter int[] dimensions)
    {
        Word dims = DimensionsNode.allocaDimsArray(rank);
        ExplodeLoopNode.explodeLoop();
        for (int i = 0; i < rank; i++)
        {
            dims.writeInt(i * 4, dimensions[i], LocationIdentity.init());
        }
        return newArrayCall(HotSpotBackend.NEW_MULTI_ARRAY, hub, rank, dims);
    }

    @Snippet
    public static Object newmultiarrayPIC(KlassPointer hub, @ConstantParameter int rank, @VarargsParameter int[] dimensions)
    {
        // Array type would be resolved by dominating resolution.
        KlassPointer picHub = LoadConstantIndirectlyFixedNode.loadKlass(hub);
        return newmultiarray(picHub, rank, dimensions);
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newArrayCall(@ConstantNodeParameter ForeignCallDescriptor descriptor, KlassPointer hub, int rank, Word dims);

    /**
     * Maximum number of long stores to emit when zeroing an object with a constant size. Larger
     * objects have their bodies initialized in a loop.
     */
    private static final int MAX_UNROLLED_OBJECT_ZEROING_STORES = 8;

    /**
     * Zero uninitialized memory in a newly allocated object, unrolling as necessary and ensuring
     * that stores are aligned.
     *
     * @param size number of bytes to zero
     * @param memory beginning of object which is being zeroed
     * @param constantSize is {@code size} known to be constant in the snippet
     * @param startOffset offset to begin zeroing. May not be word aligned.
     * @param manualUnroll maximally unroll zeroing
     */
    private static void zeroMemory(int size, Word memory, boolean constantSize, int startOffset, boolean manualUnroll, Counters counters)
    {
        fillMemory(0, size, memory, constantSize, startOffset, manualUnroll, counters);
    }

    private static void fillMemory(long value, int size, Word memory, boolean constantSize, int startOffset, boolean manualUnroll, Counters counters)
    {
        int offset = startOffset;
        if ((offset & 0x7) != 0)
        {
            memory.writeInt(offset, (int) value, LocationIdentity.init());
            offset += 4;
        }
        if (manualUnroll && ((size - offset) / 8) <= MAX_UNROLLED_OBJECT_ZEROING_STORES)
        {
            // This case handles arrays of constant length. Instead of having a snippet variant for
            // each length, generate a chain of stores of maximum length. Once it's inlined the
            // break statement will trim excess stores.
            if (counters != null && counters.instanceSeqInit != null)
            {
                counters.instanceSeqInit.inc();
            }

            ExplodeLoopNode.explodeLoop();
            for (int i = 0; i < MAX_UNROLLED_OBJECT_ZEROING_STORES; i++, offset += 8)
            {
                if (offset == size)
                {
                    break;
                }
                memory.initializeLong(offset, value, LocationIdentity.init());
            }
        }
        else
        {
            // Use Word instead of int to avoid extension to long in generated code
            Word off = WordFactory.signed(offset);
            if (constantSize && ((size - offset) / 8) <= MAX_UNROLLED_OBJECT_ZEROING_STORES)
            {
                if (counters != null && counters.instanceSeqInit != null)
                {
                    counters.instanceSeqInit.inc();
                }
                ExplodeLoopNode.explodeLoop();
            }
            else
            {
                if (counters != null && counters.instanceLoopInit != null)
                {
                    counters.instanceLoopInit.inc();
                }
            }
            for (; off.rawValue() < size; off = off.add(8))
            {
                memory.initializeLong(off, value, LocationIdentity.init());
            }
        }
    }

    /**
     * Formats some allocated memory with an object header and zeroes out the rest. Disables asserts
     * since they can't be compiled in stubs.
     */
    public static Object formatObjectForStub(KlassPointer hub, int size, Word memory, Word compileTimePrototypeMarkWord)
    {
        return formatObject(hub, size, memory, compileTimePrototypeMarkWord, true, false, null);
    }

    /**
     * Formats some allocated memory with an object header and zeroes out the rest.
     */
    protected static Object formatObject(KlassPointer hub, int size, Word memory, Word compileTimePrototypeMarkWord, boolean fillContents, boolean constantSize, Counters counters)
    {
        Word prototypeMarkWord = HotSpotReplacementsUtil.useBiasedLocking(GraalHotSpotVMConfig.INJECTED_VMCONFIG) ? hub.readWord(HotSpotReplacementsUtil.prototypeMarkWordOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION) : compileTimePrototypeMarkWord;
        HotSpotReplacementsUtil.initializeObjectHeader(memory, prototypeMarkWord, hub);
        if (fillContents)
        {
            zeroMemory(size, memory, constantSize, HotSpotReplacementsUtil.instanceHeaderSize(GraalHotSpotVMConfig.INJECTED_VMCONFIG), false, counters);
        }
        MembarNode.memoryBarrier(MemoryBarriers.STORE_STORE, LocationIdentity.init());
        return memory.toObjectNonNull();
    }

    @Snippet
    protected static void verifyHeap(@ConstantParameter Register threadRegister)
    {
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Word topValue = HotSpotReplacementsUtil.readTlabTop(thread);
        if (!topValue.equal(WordFactory.zero()))
        {
            Word topValueContents = topValue.readWord(0, HotSpotReplacementsUtil.MARK_WORD_LOCATION);
            if (topValueContents.equal(WordFactory.zero()))
            {
                AssertionSnippets.vmMessageC(AssertionSnippets.ASSERTION_VM_MESSAGE_C, true, CStringConstant.cstring("overzeroing of TLAB detected"), 0L, 0L, 0L);
            }
        }
    }

    /**
     * Formats some allocated memory with an object header and zeroes out the rest.
     */
    public static Object formatArray(KlassPointer hub, int allocationSize, int length, int headerSize, Word memory, Word prototypeMarkWord, boolean fillContents, boolean maybeUnroll, Counters counters)
    {
        memory.writeInt(HotSpotReplacementsUtil.arrayLengthOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), length, LocationIdentity.init());
        /*
         * store hub last as the concurrent garbage collectors assume length is valid if hub field
         * is not null
         */
        HotSpotReplacementsUtil.initializeObjectHeader(memory, prototypeMarkWord, hub);
        if (fillContents)
        {
            zeroMemory(allocationSize, memory, false, headerSize, maybeUnroll, counters);
        }
        MembarNode.memoryBarrier(MemoryBarriers.STORE_STORE, LocationIdentity.init());
        return memory.toObjectNonNull();
    }

    static class Counters
    {
        Counters(SnippetCounter.Group.Factory factory)
        {
            Group newInstance = factory.createSnippetCounterGroup("NewInstance");
            Group newArray = factory.createSnippetCounterGroup("NewArray");
            instanceSeqInit = new SnippetCounter(newInstance, "tlabSeqInit", "TLAB alloc with unrolled zeroing");
            instanceLoopInit = new SnippetCounter(newInstance, "tlabLoopInit", "TLAB alloc with zeroing in a loop");
            arrayLoopInit = new SnippetCounter(newArray, "tlabLoopInit", "TLAB alloc with zeroing in a loop");
            stub = new SnippetCounter(newInstance, "stub", "alloc and zeroing via stub");
        }

        final SnippetCounter instanceSeqInit;
        final SnippetCounter instanceLoopInit;
        final SnippetCounter arrayLoopInit;
        final SnippetCounter stub;
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo allocateInstance = snippet(NewObjectSnippets.class, "allocateInstance", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateInstancePIC = snippet(NewObjectSnippets.class, "allocateInstancePIC", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateArray = snippet(NewObjectSnippets.class, "allocateArray", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateArrayPIC = snippet(NewObjectSnippets.class, "allocateArrayPIC", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocatePrimitiveArrayPIC = snippet(NewObjectSnippets.class, "allocatePrimitiveArrayPIC", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateArrayDynamic = snippet(NewObjectSnippets.class, "allocateArrayDynamic", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateInstanceDynamic = snippet(NewObjectSnippets.class, "allocateInstanceDynamic", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo newmultiarray = snippet(NewObjectSnippets.class, "newmultiarray", HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo newmultiarrayPIC = snippet(NewObjectSnippets.class, "newmultiarrayPIC", HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo verifyHeap = snippet(NewObjectSnippets.class, "verifyHeap");
        private final GraalHotSpotVMConfig config;
        private final Counters counters;

        public Templates(OptionValues options, SnippetCounter.Group.Factory factory, HotSpotProviders providers, TargetDescription target, GraalHotSpotVMConfig config)
        {
            super(options, providers, providers.getSnippetReflection(), target);
            this.config = config;
            counters = new Counters(factory);
        }

        /**
         * Lowers a {@link NewInstanceNode}.
         */
        public void lower(NewInstanceNode newInstanceNode, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            StructuredGraph graph = newInstanceNode.graph();
            HotSpotResolvedObjectType type = (HotSpotResolvedObjectType) newInstanceNode.instanceClass();
            ConstantNode hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), type.klass(), providers.getMetaAccess(), graph);
            int size = instanceSize(type);

            OptionValues localOptions = graph.getOptions();
            SnippetInfo snippet = GraalOptions.GeneratePIC.getValue(localOptions) ? allocateInstancePIC : allocateInstance;
            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.addConst("size", size);
            args.add("hub", hub);
            args.add("prototypeMarkWord", type.prototypeMarkWord());
            args.addConst("fillContents", newInstanceNode.fillContents());
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("constantSize", true);
            args.addConst("typeContext", "");
            args.addConst("options", localOptions);
            args.addConst("counters", counters);

            SnippetTemplate template = template(newInstanceNode, args);
            template.instantiate(providers.getMetaAccess(), newInstanceNode, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        /**
         * Lowers a {@link NewArrayNode}.
         */
        public void lower(NewArrayNode newArrayNode, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            StructuredGraph graph = newArrayNode.graph();
            ResolvedJavaType elementType = newArrayNode.elementType();
            HotSpotResolvedObjectType arrayType = (HotSpotResolvedObjectType) elementType.getArrayClass();
            JavaKind elementKind = elementType.getJavaKind();
            ConstantNode hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), arrayType.klass(), providers.getMetaAccess(), graph);
            final int headerSize = HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(elementKind);
            int log2ElementSize = CodeUtil.log2(HotSpotJVMCIRuntimeProvider.getArrayIndexScale(elementKind));

            OptionValues localOptions = graph.getOptions();
            SnippetInfo snippet;
            if (GraalOptions.GeneratePIC.getValue(localOptions))
            {
                if (elementType.isPrimitive())
                {
                    snippet = allocatePrimitiveArrayPIC;
                }
                else
                {
                    snippet = allocateArrayPIC;
                }
            }
            else
            {
                snippet = allocateArray;
            }

            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("hub", hub);
            ValueNode length = newArrayNode.length();
            args.add("length", length.isAlive() ? length : graph.addOrUniqueWithInputs(length));
            args.add("prototypeMarkWord", arrayType.prototypeMarkWord());
            args.addConst("headerSize", headerSize);
            args.addConst("log2ElementSize", log2ElementSize);
            args.addConst("fillContents", newArrayNode.fillContents());
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("maybeUnroll", length.isConstant());
            args.addConst("typeContext", "");
            args.addConst("options", localOptions);
            args.addConst("counters", counters);
            SnippetTemplate template = template(newArrayNode, args);
            template.instantiate(providers.getMetaAccess(), newArrayNode, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(DynamicNewInstanceNode newInstanceNode, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            Arguments args = new Arguments(allocateInstanceDynamic, newInstanceNode.graph().getGuardsStage(), tool.getLoweringStage());
            OptionValues localOptions = newInstanceNode.getOptions();
            args.add("type", newInstanceNode.getInstanceType());
            ValueNode classClass = newInstanceNode.getClassClass();
            args.add("classClass", classClass);
            args.addConst("fillContents", newInstanceNode.fillContents());
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("options", localOptions);
            args.addConst("counters", counters);

            SnippetTemplate template = template(newInstanceNode, args);
            template.instantiate(providers.getMetaAccess(), newInstanceNode, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        public void lower(DynamicNewArrayNode newArrayNode, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            StructuredGraph graph = newArrayNode.graph();
            OptionValues localOptions = graph.getOptions();
            Arguments args = new Arguments(allocateArrayDynamic, newArrayNode.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("elementType", newArrayNode.getElementType());
            ValueNode voidClass = newArrayNode.getVoidClass();
            args.add("voidClass", voidClass);
            ValueNode length = newArrayNode.length();
            args.add("length", length.isAlive() ? length : graph.addOrUniqueWithInputs(length));
            args.addConst("fillContents", newArrayNode.fillContents());
            args.addConst("threadRegister", registers.getThreadRegister());
            /*
             * We use Kind.Illegal as a marker value instead of null because constant snippet
             * parameters cannot be null.
             */
            args.addConst("knownElementKind", newArrayNode.getKnownElementKind() == null ? JavaKind.Illegal : newArrayNode.getKnownElementKind());
            if (newArrayNode.getKnownElementKind() != null)
            {
                args.addConst("knownLayoutHelper", lookupArrayClass(tool, newArrayNode.getKnownElementKind()).layoutHelper());
            }
            else
            {
                args.addConst("knownLayoutHelper", 0);
            }
            args.add("prototypeMarkWord", lookupArrayClass(tool, JavaKind.Object).prototypeMarkWord());
            args.addConst("options", localOptions);
            args.addConst("counters", counters);
            SnippetTemplate template = template(newArrayNode, args);
            template.instantiate(providers.getMetaAccess(), newArrayNode, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        private static HotSpotResolvedObjectType lookupArrayClass(LoweringTool tool, JavaKind kind)
        {
            return (HotSpotResolvedObjectType) tool.getMetaAccess().lookupJavaType(kind == JavaKind.Object ? Object.class : kind.toJavaClass()).getArrayClass();
        }

        public void lower(NewMultiArrayNode newmultiarrayNode, LoweringTool tool)
        {
            StructuredGraph graph = newmultiarrayNode.graph();
            OptionValues localOptions = graph.getOptions();
            int rank = newmultiarrayNode.dimensionCount();
            ValueNode[] dims = new ValueNode[rank];
            for (int i = 0; i < newmultiarrayNode.dimensionCount(); i++)
            {
                dims[i] = newmultiarrayNode.dimension(i);
            }
            HotSpotResolvedObjectType type = (HotSpotResolvedObjectType) newmultiarrayNode.type();
            ConstantNode hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), type.klass(), providers.getMetaAccess(), graph);

            SnippetInfo snippet = GraalOptions.GeneratePIC.getValue(localOptions) ? newmultiarrayPIC : newmultiarray;
            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("hub", hub);
            args.addConst("rank", rank);
            args.addVarargs("dimensions", int.class, StampFactory.forKind(JavaKind.Int), dims);
            template(newmultiarrayNode, args).instantiate(providers.getMetaAccess(), newmultiarrayNode, SnippetTemplate.DEFAULT_REPLACER, args);
        }

        private static int instanceSize(HotSpotResolvedObjectType type)
        {
            int size = type.instanceSize();
            return size;
        }

        public void lower(VerifyHeapNode verifyHeapNode, HotSpotRegistersProvider registers, LoweringTool tool)
        {
            GraphUtil.removeFixedWithUnusedInputs(verifyHeapNode);
        }
    }
}
