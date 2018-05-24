package giraaff.hotspot.replacements;

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

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.api.replacements.Snippet.VarargsParameter;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.calc.UnsignedMath;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.meta.HotSpotRegistersProvider;
import giraaff.hotspot.nodes.DimensionsNode;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.PiArrayNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.PrefetchAllocateNode;
import giraaff.nodes.SnippetAnchorNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.MembarNode;
import giraaff.nodes.java.DynamicNewArrayNode;
import giraaff.nodes.java.DynamicNewInstanceNode;
import giraaff.nodes.java.NewArrayNode;
import giraaff.nodes.java.NewInstanceNode;
import giraaff.nodes.java.NewMultiArrayNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.CStringConstant;
import giraaff.replacements.nodes.ExplodeLoopNode;
import giraaff.word.Word;

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
    public static Object allocateInstance(@ConstantParameter int size, KlassPointer hub, Word prototypeMarkWord, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter boolean constantSize, @ConstantParameter String typeContext, @ConstantParameter OptionValues options)
    {
        return PiNode.piCastToSnippetReplaceeStamp(allocateInstanceHelper(size, hub, prototypeMarkWord, fillContents, threadRegister, constantSize, typeContext, options));
    }

    public static Object allocateInstanceHelper(int size, KlassPointer hub, Word prototypeMarkWord, boolean fillContents, Register threadRegister, boolean constantSize, String typeContext, OptionValues options)
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
            result = formatObject(hub, size, top, prototypeMarkWord, fillContents, constantSize);
        }
        else
        {
            result = newInstance(HotSpotBackend.NEW_INSTANCE, hub);
        }
        return result;
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newInstance(@ConstantNodeParameter ForeignCallDescriptor descriptor, KlassPointer hub);

    @Snippet
    public static Object allocateInstanceDynamic(Class<?> type, Class<?> classClass, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter OptionValues options)
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

        return PiNode.piCastToSnippetReplaceeStamp(allocateInstanceDynamicHelper(type, fillContents, threadRegister, options, nonNullType));
    }

    private static Object allocateInstanceDynamicHelper(Class<?> type, boolean fillContents, Register threadRegister, OptionValues options, Class<?> nonNullType)
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
                    return allocateInstanceHelper(layoutHelper, nonNullHub, prototypeMarkWord, fillContents, threadRegister, false, "", options);
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
    public static Object allocateArray(KlassPointer hub, int length, Word prototypeMarkWord, @ConstantParameter int headerSize, @ConstantParameter int log2ElementSize, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter boolean maybeUnroll, @ConstantParameter String typeContext, @ConstantParameter OptionValues options)
    {
        Object result = allocateArrayImpl(hub, length, prototypeMarkWord, headerSize, log2ElementSize, fillContents, threadRegister, maybeUnroll, typeContext, false, options);
        return PiArrayNode.piArrayCastToSnippetReplaceeStamp(result, length);
    }

    private static Object allocateArrayImpl(KlassPointer hub, int length, Word prototypeMarkWord, int headerSize, int log2ElementSize, boolean fillContents, Register threadRegister, boolean maybeUnroll, String typeContext, boolean skipNegativeCheck, OptionValues options)
    {
        int allocationSize = HotSpotReplacementsUtil.arrayAllocationSize(length, headerSize, log2ElementSize);
        Word thread = HotSpotReplacementsUtil.registerAsWord(threadRegister);
        Word top = HotSpotReplacementsUtil.readTlabTop(thread);
        Word end = HotSpotReplacementsUtil.readTlabEnd(thread);
        Word newTop = top.add(allocationSize);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, skipNegativeCheck || UnsignedMath.belowThan(length, MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH)) && HotSpotReplacementsUtil.useTLAB(GraalHotSpotVMConfig.INJECTED_VMCONFIG) && BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, newTop.belowOrEqual(end)))
        {
            HotSpotReplacementsUtil.writeTlabTop(thread, newTop);
            emitPrefetchAllocate(newTop, true);
            return formatArray(hub, allocationSize, length, headerSize, top, prototypeMarkWord, fillContents, maybeUnroll);
        }
        else
        {
            return newArray(HotSpotBackend.NEW_ARRAY, hub, length, fillContents);
        }
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
    public static Object allocateArrayDynamic(Class<?> elementType, Class<?> voidClass, int length, @ConstantParameter boolean fillContents, @ConstantParameter Register threadRegister, @ConstantParameter JavaKind knownElementKind, @ConstantParameter int knownLayoutHelper, Word prototypeMarkWord, @ConstantParameter OptionValues options)
    {
        Object result = allocateArrayDynamicImpl(elementType, voidClass, length, fillContents, threadRegister, knownElementKind, knownLayoutHelper, prototypeMarkWord, options);
        return result;
    }

    private static Object allocateArrayDynamicImpl(Class<?> elementType, Class<?> voidClass, int length, boolean fillContents, Register threadRegister, JavaKind knownElementKind, int knownLayoutHelper, Word prototypeMarkWord, OptionValues options)
    {
        // We only need the dynamic check for void when we have no static information from knownElementKind.
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

        // For arrays, layout helper is a negative number, containing four distinct bytes,
        // as follows:
        //    MSB:[tag, hsz, ebt, log2(esz)]:LSB
        // where:
        //    tag is 0x80 if the elements are oops, 0xC0 if non-oops
        //    hsz is array header size in bytes (i.e., offset of first element)
        //    ebt is the BasicType of the elements
        //    esz is the element size in bytes

        int headerSize = (layoutHelper >> HotSpotReplacementsUtil.layoutHelperHeaderSizeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) & HotSpotReplacementsUtil.layoutHelperHeaderSizeMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        int log2ElementSize = (layoutHelper >> HotSpotReplacementsUtil.layoutHelperLog2ElementSizeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)) & HotSpotReplacementsUtil.layoutHelperLog2ElementSizeMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG);

        Object result = allocateArrayImpl(nonNullKlass, length, prototypeMarkWord, headerSize, log2ElementSize, fillContents, threadRegister, false, "dynamic type", true, options);
        return PiArrayNode.piArrayCastToSnippetReplaceeStamp(result, length);
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

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newArrayCall(@ConstantNodeParameter ForeignCallDescriptor descriptor, KlassPointer hub, int rank, Word dims);

    /**
     * Maximum number of long stores to emit when zeroing an object with a constant size.
     * Larger objects have their bodies initialized in a loop.
     */
    private static final int MAX_UNROLLED_OBJECT_ZEROING_STORES = 8;

    /**
     * Zero uninitialized memory in a newly allocated object, unrolling as necessary and
     * ensuring that stores are aligned.
     *
     * @param size number of bytes to zero
     * @param memory beginning of object which is being zeroed
     * @param constantSize is {@code size} known to be constant in the snippet
     * @param startOffset offset to begin zeroing. May not be word aligned.
     * @param manualUnroll maximally unroll zeroing
     */
    private static void zeroMemory(int size, Word memory, boolean constantSize, int startOffset, boolean manualUnroll)
    {
        fillMemory(0, size, memory, constantSize, startOffset, manualUnroll);
    }

    private static void fillMemory(long value, int size, Word memory, boolean constantSize, int startOffset, boolean manualUnroll)
    {
        int offset = startOffset;
        if ((offset & 0x7) != 0)
        {
            memory.writeInt(offset, (int) value, LocationIdentity.init());
            offset += 4;
        }
        if (manualUnroll && ((size - offset) / 8) <= MAX_UNROLLED_OBJECT_ZEROING_STORES)
        {
            // This case handles arrays of constant length. Instead of having a snippet variant
            // for each length, generate a chain of stores of maximum length. Once it's inlined
            // the break statement will trim excess stores.
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
            // Use Word instead of int to avoid extension to long in generated code.
            Word off = WordFactory.signed(offset);
            if (constantSize && ((size - offset) / 8) <= MAX_UNROLLED_OBJECT_ZEROING_STORES)
            {
                ExplodeLoopNode.explodeLoop();
            }
            for ( ; off.rawValue() < size; off = off.add(8))
            {
                memory.initializeLong(off, value, LocationIdentity.init());
            }
        }
    }

    /**
     * Formats some allocated memory with an object header and zeroes out the rest.
     * Disables asserts since they can't be compiled in stubs.
     */
    public static Object formatObjectForStub(KlassPointer hub, int size, Word memory, Word compileTimePrototypeMarkWord)
    {
        return formatObject(hub, size, memory, compileTimePrototypeMarkWord, true, false);
    }

    /**
     * Formats some allocated memory with an object header and zeroes out the rest.
     */
    protected static Object formatObject(KlassPointer hub, int size, Word memory, Word compileTimePrototypeMarkWord, boolean fillContents, boolean constantSize)
    {
        Word prototypeMarkWord = HotSpotReplacementsUtil.useBiasedLocking(GraalHotSpotVMConfig.INJECTED_VMCONFIG) ? hub.readWord(HotSpotReplacementsUtil.prototypeMarkWordOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION) : compileTimePrototypeMarkWord;
        HotSpotReplacementsUtil.initializeObjectHeader(memory, prototypeMarkWord, hub);
        if (fillContents)
        {
            zeroMemory(size, memory, constantSize, HotSpotReplacementsUtil.instanceHeaderSize(GraalHotSpotVMConfig.INJECTED_VMCONFIG), false);
        }
        MembarNode.memoryBarrier(MemoryBarriers.STORE_STORE, LocationIdentity.init());
        return memory.toObjectNonNull();
    }

    /**
     * Formats some allocated memory with an object header and zeroes out the rest.
     */
    public static Object formatArray(KlassPointer hub, int allocationSize, int length, int headerSize, Word memory, Word prototypeMarkWord, boolean fillContents, boolean maybeUnroll)
    {
        memory.writeInt(HotSpotReplacementsUtil.arrayLengthOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), length, LocationIdentity.init());
        // store hub last as the concurrent garbage collectors assume length is valid if hub field is not null
        HotSpotReplacementsUtil.initializeObjectHeader(memory, prototypeMarkWord, hub);
        if (fillContents)
        {
            zeroMemory(allocationSize, memory, false, headerSize, maybeUnroll);
        }
        MembarNode.memoryBarrier(MemoryBarriers.STORE_STORE, LocationIdentity.init());
        return memory.toObjectNonNull();
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo allocateInstance = snippet(NewObjectSnippets.class, "allocateInstance", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateArray = snippet(NewObjectSnippets.class, "allocateArray", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateArrayDynamic = snippet(NewObjectSnippets.class, "allocateArrayDynamic", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo allocateInstanceDynamic = snippet(NewObjectSnippets.class, "allocateInstanceDynamic", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final SnippetInfo newmultiarray = snippet(NewObjectSnippets.class, "newmultiarray", HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        private final GraalHotSpotVMConfig config;

        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target, GraalHotSpotVMConfig config)
        {
            super(options, providers, providers.getSnippetReflection(), target);
            this.config = config;
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
            SnippetInfo snippet = allocateInstance;
            Arguments args = new Arguments(snippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.addConst("size", size);
            args.add("hub", hub);
            args.add("prototypeMarkWord", type.prototypeMarkWord());
            args.addConst("fillContents", newInstanceNode.fillContents());
            args.addConst("threadRegister", registers.getThreadRegister());
            args.addConst("constantSize", true);
            args.addConst("typeContext", "");
            args.addConst("options", localOptions);

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
            SnippetInfo snippet = allocateArray;

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
            // We use Kind.Illegal as a marker value instead of null because constant snippet parameters cannot be null.
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

            SnippetInfo snippet = newmultiarray;
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
    }
}
