package giraaff.hotspot.replacements;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.MemoryBarriers;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.TargetDescription;
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
import giraaff.core.common.calc.UnsignedMath;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotRuntime;
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
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.ExplodeLoopNode;
import giraaff.word.Word;

///
// Snippets used for implementing NEW, ANEWARRAY and NEWARRAY.
///
// @class NewObjectSnippets
public final class NewObjectSnippets implements Snippets
{
    // @cons
    private NewObjectSnippets()
    {
        super();
    }

    // @enum NewObjectSnippets.ProfileContext
    enum ProfileContext
    {
        AllocatingMethod,
        InstanceOrArray,
        AllocatedType,
        AllocatedTypesInMethod,
        Total
    }

    public static void emitPrefetchAllocate(Word __address, boolean __isArray)
    {
        if (HotSpotRuntime.allocatePrefetchStyle > 0)
        {
            // Insert a prefetch for each allocation only on the fast-path.
            // Generate several prefetch instructions.
            int __lines = __isArray ? HotSpotRuntime.allocatePrefetchLines : HotSpotRuntime.allocateInstancePrefetchLines;
            int __stepSize = HotSpotRuntime.allocatePrefetchStepSize;
            int __distance = HotSpotRuntime.allocatePrefetchDistance;
            ExplodeLoopNode.explodeLoop();
            for (int __i = 0; __i < __lines; __i++)
            {
                PrefetchAllocateNode.prefetch(OffsetAddressNode.address(__address, __distance));
                __distance += __stepSize;
            }
        }
    }

    @Snippet
    public static Object allocateInstance(@ConstantParameter int __size, KlassPointer __hub, Word __prototypeMarkWord, @ConstantParameter boolean __fillContents, @ConstantParameter Register __threadRegister, @ConstantParameter boolean __constantSize, @ConstantParameter String __typeContext)
    {
        return PiNode.piCastToSnippetReplaceeStamp(allocateInstanceHelper(__size, __hub, __prototypeMarkWord, __fillContents, __threadRegister, __constantSize, __typeContext));
    }

    public static Object allocateInstanceHelper(int __size, KlassPointer __hub, Word __prototypeMarkWord, boolean __fillContents, Register __threadRegister, boolean __constantSize, String __typeContext)
    {
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        Word __top = HotSpotReplacementsUtil.readTlabTop(__thread);
        Word __end = HotSpotReplacementsUtil.readTlabEnd(__thread);
        Word __newTop = __top.add(__size);
        if (HotSpotRuntime.useTLAB && BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __newTop.belowOrEqual(__end)))
        {
            HotSpotReplacementsUtil.writeTlabTop(__thread, __newTop);
            emitPrefetchAllocate(__newTop, false);
            return formatObject(__hub, __size, __top, __prototypeMarkWord, __fillContents, __constantSize);
        }
        else
        {
            return newInstance(HotSpotBackend.NEW_INSTANCE, __hub);
        }
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newInstance(@ConstantNodeParameter ForeignCallDescriptor __descriptor, KlassPointer __hub);

    @Snippet
    public static Object allocateInstanceDynamic(Class<?> __type, Class<?> __classClass, @ConstantParameter boolean __fillContents, @ConstantParameter Register __threadRegister)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __type == null))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        Class<?> __nonNullType = PiNode.piCastNonNullClass(__type, SnippetAnchorNode.anchor());

        if (BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, DynamicNewInstanceNode.throwsInstantiationException(__type, __classClass)))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }

        return PiNode.piCastToSnippetReplaceeStamp(allocateInstanceDynamicHelper(__type, __fillContents, __threadRegister, __nonNullType));
    }

    private static Object allocateInstanceDynamicHelper(Class<?> __type, boolean __fillContents, Register __threadRegister, Class<?> __nonNullType)
    {
        KlassPointer __hub = ClassGetHubNode.readClass(__nonNullType);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, !__hub.isNull()))
        {
            KlassPointer __nonNullHub = ClassGetHubNode.piCastNonNull(__hub, SnippetAnchorNode.anchor());

            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, HotSpotReplacementsUtil.isInstanceKlassFullyInitialized(__nonNullHub)))
            {
                int __layoutHelper = HotSpotReplacementsUtil.readLayoutHelper(__nonNullHub);
                // src/share/vm/oops/klass.hpp: For instances, layout helper is a positive number,
                // the instance size. This size is already passed through align_object_size and
                // scaled to bytes. The low order bit is set if instances of this class cannot be
                // allocated using the fastpath.
                if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, (__layoutHelper & 1) == 0))
                {
                    Word __prototypeMarkWord = __nonNullHub.readWord(HotSpotRuntime.prototypeMarkWordOffset, HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION);
                    // FIXME(je,ds): we should actually pass typeContext instead of "" but late
                    // binding of parameters is not yet supported by the GraphBuilderPlugin system.
                    return allocateInstanceHelper(__layoutHelper, __nonNullHub, __prototypeMarkWord, __fillContents, __threadRegister, false, "");
                }
            }
        }
        return dynamicNewInstanceStub(__type);
    }

    ///
    // Maximum array length for which fast path allocation is used.
    ///
    // @def
    public static final int MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH = 0x00FFFFFF;

    @Snippet
    public static Object allocateArray(KlassPointer __hub, int __length, Word __prototypeMarkWord, @ConstantParameter int __headerSize, @ConstantParameter int __log2ElementSize, @ConstantParameter boolean __fillContents, @ConstantParameter Register __threadRegister, @ConstantParameter boolean __maybeUnroll, @ConstantParameter String __typeContext)
    {
        Object __result = allocateArrayImpl(__hub, __length, __prototypeMarkWord, __headerSize, __log2ElementSize, __fillContents, __threadRegister, __maybeUnroll, __typeContext, false);
        return PiArrayNode.piArrayCastToSnippetReplaceeStamp(__result, __length);
    }

    private static Object allocateArrayImpl(KlassPointer __hub, int __length, Word __prototypeMarkWord, int __headerSize, int __log2ElementSize, boolean __fillContents, Register __threadRegister, boolean __maybeUnroll, String __typeContext, boolean __skipNegativeCheck)
    {
        int __allocationSize = HotSpotReplacementsUtil.arrayAllocationSize(__length, __headerSize, __log2ElementSize);
        Word __thread = HotSpotReplacementsUtil.registerAsWord(__threadRegister);
        Word __top = HotSpotReplacementsUtil.readTlabTop(__thread);
        Word __end = HotSpotReplacementsUtil.readTlabEnd(__thread);
        Word __newTop = __top.add(__allocationSize);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FREQUENT_PROBABILITY, __skipNegativeCheck || UnsignedMath.belowThan(__length, MAX_ARRAY_FAST_PATH_ALLOCATION_LENGTH)) && HotSpotRuntime.useTLAB && BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __newTop.belowOrEqual(__end)))
        {
            HotSpotReplacementsUtil.writeTlabTop(__thread, __newTop);
            emitPrefetchAllocate(__newTop, true);
            return formatArray(__hub, __allocationSize, __length, __headerSize, __top, __prototypeMarkWord, __fillContents, __maybeUnroll);
        }
        else
        {
            return newArray(HotSpotBackend.NEW_ARRAY, __hub, __length, __fillContents);
        }
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newArray(@ConstantNodeParameter ForeignCallDescriptor __descriptor, KlassPointer __hub, int __length, boolean __fillContents);

    // @def
    public static final ForeignCallDescriptor DYNAMIC_NEW_ARRAY = new ForeignCallDescriptor("dynamic_new_array", Object.class, Class.class, int.class);
    // @def
    public static final ForeignCallDescriptor DYNAMIC_NEW_INSTANCE = new ForeignCallDescriptor("dynamic_new_instance", Object.class, Class.class);

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object dynamicNewArrayStub(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Class<?> __elementType, int __length);

    public static Object dynamicNewInstanceStub(Class<?> __elementType)
    {
        return dynamicNewInstanceStubCall(DYNAMIC_NEW_INSTANCE, __elementType);
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object dynamicNewInstanceStubCall(@ConstantNodeParameter ForeignCallDescriptor __descriptor, Class<?> __elementType);

    @Snippet
    public static Object allocateArrayDynamic(Class<?> __elementType, Class<?> __voidClass, int __length, @ConstantParameter boolean __fillContents, @ConstantParameter Register __threadRegister, @ConstantParameter JavaKind __knownElementKind, @ConstantParameter int __knownLayoutHelper, Word __prototypeMarkWord)
    {
        return allocateArrayDynamicImpl(__elementType, __voidClass, __length, __fillContents, __threadRegister, __knownElementKind, __knownLayoutHelper, __prototypeMarkWord);
    }

    private static Object allocateArrayDynamicImpl(Class<?> __elementType, Class<?> __voidClass, int __length, boolean __fillContents, Register __threadRegister, JavaKind __knownElementKind, int __knownLayoutHelper, Word __prototypeMarkWord)
    {
        // We only need the dynamic check for void when we have no static information from knownElementKind.
        if (__knownElementKind == JavaKind.Illegal && BranchProbabilityNode.probability(BranchProbabilityNode.SLOW_PATH_PROBABILITY, __elementType == null || DynamicNewArrayNode.throwsIllegalArgumentException(__elementType, __voidClass)))
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }

        KlassPointer __klass = HotSpotReplacementsUtil.loadKlassFromObject(__elementType, HotSpotRuntime.arrayKlassOffset, HotSpotReplacementsUtil.CLASS_ARRAY_KLASS_LOCATION);
        if (__klass.isNull())
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        KlassPointer __nonNullKlass = ClassGetHubNode.piCastNonNull(__klass, SnippetAnchorNode.anchor());

        if (__length < 0)
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        int __layoutHelper;
        if (__knownElementKind == JavaKind.Illegal)
        {
            __layoutHelper = HotSpotReplacementsUtil.readLayoutHelper(__nonNullKlass);
        }
        else
        {
            __layoutHelper = __knownLayoutHelper;
        }

        // For arrays, layout helper is a negative number, containing four distinct bytes,
        // as follows:
        //    MSB:[tag, hsz, ebt, log2(esz)]:LSB
        // where:
        //    tag is 0x80 if the elements are oops, 0xC0 if non-oops
        //    hsz is array header size in bytes (i.e., offset of first element)
        //    ebt is the BasicType of the elements
        //    esz is the element size in bytes

        int __headerSize = (__layoutHelper >> HotSpotRuntime.layoutHelperHeaderSizeShift) & HotSpotRuntime.layoutHelperHeaderSizeMask;
        int __log2ElementSize = (__layoutHelper >> HotSpotRuntime.layoutHelperLog2ElementSizeShift) & HotSpotRuntime.layoutHelperLog2ElementSizeMask;

        Object __result = allocateArrayImpl(__nonNullKlass, __length, __prototypeMarkWord, __headerSize, __log2ElementSize, __fillContents, __threadRegister, false, "dynamic type", true);
        return PiArrayNode.piArrayCastToSnippetReplaceeStamp(__result, __length);
    }

    ///
    // Calls the runtime stub for implementing MULTIANEWARRAY.
    ///
    @Snippet
    public static Object newmultiarray(KlassPointer __hub, @ConstantParameter int __rank, @VarargsParameter int[] __dimensions)
    {
        Word __dims = DimensionsNode.allocaDimsArray(__rank);
        ExplodeLoopNode.explodeLoop();
        for (int __i = 0; __i < __rank; __i++)
        {
            __dims.writeInt(__i * 4, __dimensions[__i], LocationIdentity.init());
        }
        return newArrayCall(HotSpotBackend.NEW_MULTI_ARRAY, __hub, __rank, __dims);
    }

    @NodeIntrinsic(value = ForeignCallNode.class, injectedStampIsNonNull = true)
    public static native Object newArrayCall(@ConstantNodeParameter ForeignCallDescriptor __descriptor, KlassPointer __hub, int __rank, Word __dims);

    ///
    // Maximum number of long stores to emit when zeroing an object with a constant size.
    // Larger objects have their bodies initialized in a loop.
    ///
    // @def
    private static final int MAX_UNROLLED_OBJECT_ZEROING_STORES = 8;

    ///
    // Zero uninitialized memory in a newly allocated object, unrolling as necessary and
    // ensuring that stores are aligned.
    //
    // @param size number of bytes to zero
    // @param memory beginning of object which is being zeroed
    // @param constantSize is {@code size} known to be constant in the snippet
    // @param startOffset offset to begin zeroing. May not be word aligned.
    // @param manualUnroll maximally unroll zeroing
    ///
    private static void zeroMemory(int __size, Word __memory, boolean __constantSize, int __startOffset, boolean __manualUnroll)
    {
        fillMemory(0, __size, __memory, __constantSize, __startOffset, __manualUnroll);
    }

    private static void fillMemory(long __value, int __size, Word __memory, boolean __constantSize, int __startOffset, boolean __manualUnroll)
    {
        int __offset = __startOffset;
        if ((__offset & 0x7) != 0)
        {
            __memory.writeInt(__offset, (int) __value, LocationIdentity.init());
            __offset += 4;
        }
        if (__manualUnroll && ((__size - __offset) / 8) <= MAX_UNROLLED_OBJECT_ZEROING_STORES)
        {
            // This case handles arrays of constant length. Instead of having a snippet variant
            // for each length, generate a chain of stores of maximum length. Once it's inlined
            // the break statement will trim excess stores.
            ExplodeLoopNode.explodeLoop();
            for (int __i = 0; __i < MAX_UNROLLED_OBJECT_ZEROING_STORES; __i++, __offset += 8)
            {
                if (__offset == __size)
                {
                    break;
                }
                __memory.initializeLong(__offset, __value, LocationIdentity.init());
            }
        }
        else
        {
            // Use Word instead of int to avoid extension to long in generated code.
            Word __off = WordFactory.signed(__offset);
            if (__constantSize && ((__size - __offset) / 8) <= MAX_UNROLLED_OBJECT_ZEROING_STORES)
            {
                ExplodeLoopNode.explodeLoop();
            }
            for ( ; __off.rawValue() < __size; __off = __off.add(8))
            {
                __memory.initializeLong(__off, __value, LocationIdentity.init());
            }
        }
    }

    ///
    // Formats some allocated memory with an object header and zeroes out the rest.
    // Disables asserts since they can't be compiled in stubs.
    ///
    public static Object formatObjectForStub(KlassPointer __hub, int __size, Word __memory, Word __compileTimePrototypeMarkWord)
    {
        return formatObject(__hub, __size, __memory, __compileTimePrototypeMarkWord, true, false);
    }

    ///
    // Formats some allocated memory with an object header and zeroes out the rest.
    ///
    protected static Object formatObject(KlassPointer __hub, int __size, Word __memory, Word __compileTimePrototypeMarkWord, boolean __fillContents, boolean __constantSize)
    {
        Word __prototypeMarkWord = HotSpotRuntime.useBiasedLocking ? __hub.readWord(HotSpotRuntime.prototypeMarkWordOffset, HotSpotReplacementsUtil.PROTOTYPE_MARK_WORD_LOCATION) : __compileTimePrototypeMarkWord;
        HotSpotReplacementsUtil.initializeObjectHeader(__memory, __prototypeMarkWord, __hub);
        if (__fillContents)
        {
            zeroMemory(__size, __memory, __constantSize, HotSpotReplacementsUtil.instanceHeaderSize(), false);
        }
        MembarNode.memoryBarrier(MemoryBarriers.STORE_STORE, LocationIdentity.init());
        return __memory.toObjectNonNull();
    }

    ///
    // Formats some allocated memory with an object header and zeroes out the rest.
    ///
    public static Object formatArray(KlassPointer __hub, int __allocationSize, int __length, int __headerSize, Word __memory, Word __prototypeMarkWord, boolean __fillContents, boolean __maybeUnroll)
    {
        __memory.writeInt(HotSpotRuntime.arrayLengthOffset, __length, LocationIdentity.init());
        // store hub last as the concurrent garbage collectors assume length is valid if hub field is not null
        HotSpotReplacementsUtil.initializeObjectHeader(__memory, __prototypeMarkWord, __hub);
        if (__fillContents)
        {
            zeroMemory(__allocationSize, __memory, false, __headerSize, __maybeUnroll);
        }
        MembarNode.memoryBarrier(MemoryBarriers.STORE_STORE, LocationIdentity.init());
        return __memory.toObjectNonNull();
    }

    // @class NewObjectSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        // @field
        private final SnippetInfo ___allocateInstance = snippet(NewObjectSnippets.class, "allocateInstance", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        // @field
        private final SnippetInfo ___allocateArray = snippet(NewObjectSnippets.class, "allocateArray", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        // @field
        private final SnippetInfo ___allocateArrayDynamic = snippet(NewObjectSnippets.class, "allocateArrayDynamic", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        // @field
        private final SnippetInfo ___allocateInstanceDynamic = snippet(NewObjectSnippets.class, "allocateInstanceDynamic", HotSpotReplacementsUtil.MARK_WORD_LOCATION, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);
        // @field
        private final SnippetInfo ___newmultiarray = snippet(NewObjectSnippets.class, "newmultiarray", HotSpotReplacementsUtil.TLAB_TOP_LOCATION, HotSpotReplacementsUtil.TLAB_END_LOCATION);

        // @cons
        public Templates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
        }

        ///
        // Lowers a {@link NewInstanceNode}.
        ///
        public void lower(NewInstanceNode __newInstanceNode, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            StructuredGraph __graph = __newInstanceNode.graph();
            HotSpotResolvedObjectType __type = (HotSpotResolvedObjectType) __newInstanceNode.instanceClass();
            ConstantNode __hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), __type.klass(), this.___providers.getMetaAccess(), __graph);
            int __size = instanceSize(__type);

            SnippetInfo __snippet = this.___allocateInstance;
            Arguments __args = new Arguments(__snippet, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.addConst("size", __size);
            __args.add("hub", __hub);
            __args.add("prototypeMarkWord", __type.prototypeMarkWord());
            __args.addConst("fillContents", __newInstanceNode.fillContents());
            __args.addConst("threadRegister", __registers.getThreadRegister());
            __args.addConst("constantSize", true);
            __args.addConst("typeContext", "");

            SnippetTemplate __template = template(__newInstanceNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __newInstanceNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        ///
        // Lowers a {@link NewArrayNode}.
        ///
        public void lower(NewArrayNode __newArrayNode, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            StructuredGraph __graph = __newArrayNode.graph();
            ResolvedJavaType __elementType = __newArrayNode.elementType();
            HotSpotResolvedObjectType __arrayType = (HotSpotResolvedObjectType) __elementType.getArrayClass();
            JavaKind __elementKind = __elementType.getJavaKind();
            ConstantNode __hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), __arrayType.klass(), this.___providers.getMetaAccess(), __graph);
            final int __headerSize = HotSpotRuntime.getArrayBaseOffset(__elementKind);
            int __log2ElementSize = CodeUtil.log2(HotSpotRuntime.getArrayIndexScale(__elementKind));

            SnippetInfo __snippet = this.___allocateArray;

            Arguments __args = new Arguments(__snippet, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("hub", __hub);
            ValueNode __length = __newArrayNode.length();
            __args.add("length", __length.isAlive() ? __length : __graph.addOrUniqueWithInputs(__length));
            __args.add("prototypeMarkWord", __arrayType.prototypeMarkWord());
            __args.addConst("headerSize", __headerSize);
            __args.addConst("log2ElementSize", __log2ElementSize);
            __args.addConst("fillContents", __newArrayNode.fillContents());
            __args.addConst("threadRegister", __registers.getThreadRegister());
            __args.addConst("maybeUnroll", __length.isConstant());
            __args.addConst("typeContext", "");

            SnippetTemplate __template = template(__newArrayNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __newArrayNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(DynamicNewInstanceNode __newInstanceNode, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            Arguments __args = new Arguments(this.___allocateInstanceDynamic, __newInstanceNode.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("type", __newInstanceNode.getInstanceType());
            ValueNode __classClass = __newInstanceNode.getClassClass();
            __args.add("classClass", __classClass);
            __args.addConst("fillContents", __newInstanceNode.fillContents());
            __args.addConst("threadRegister", __registers.getThreadRegister());

            SnippetTemplate __template = template(__newInstanceNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __newInstanceNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        public void lower(DynamicNewArrayNode __newArrayNode, HotSpotRegistersProvider __registers, LoweringTool __tool)
        {
            StructuredGraph __graph = __newArrayNode.graph();
            Arguments __args = new Arguments(this.___allocateArrayDynamic, __newArrayNode.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("elementType", __newArrayNode.getElementType());
            ValueNode __voidClass = __newArrayNode.getVoidClass();
            __args.add("voidClass", __voidClass);
            ValueNode __length = __newArrayNode.length();
            __args.add("length", __length.isAlive() ? __length : __graph.addOrUniqueWithInputs(__length));
            __args.addConst("fillContents", __newArrayNode.fillContents());
            __args.addConst("threadRegister", __registers.getThreadRegister());
            // We use Kind.Illegal as a marker value instead of null because constant snippet parameters cannot be null.
            __args.addConst("knownElementKind", __newArrayNode.getKnownElementKind() == null ? JavaKind.Illegal : __newArrayNode.getKnownElementKind());
            if (__newArrayNode.getKnownElementKind() != null)
            {
                __args.addConst("knownLayoutHelper", lookupArrayClass(__tool, __newArrayNode.getKnownElementKind()).layoutHelper());
            }
            else
            {
                __args.addConst("knownLayoutHelper", 0);
            }
            __args.add("prototypeMarkWord", lookupArrayClass(__tool, JavaKind.Object).prototypeMarkWord());

            SnippetTemplate __template = template(__newArrayNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __newArrayNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        private static HotSpotResolvedObjectType lookupArrayClass(LoweringTool __tool, JavaKind __kind)
        {
            return (HotSpotResolvedObjectType) __tool.getMetaAccess().lookupJavaType(__kind == JavaKind.Object ? Object.class : __kind.toJavaClass()).getArrayClass();
        }

        public void lower(NewMultiArrayNode __newmultiarrayNode, LoweringTool __tool)
        {
            StructuredGraph __graph = __newmultiarrayNode.graph();
            int __rank = __newmultiarrayNode.dimensionCount();
            ValueNode[] __dims = new ValueNode[__rank];
            for (int __i = 0; __i < __newmultiarrayNode.dimensionCount(); __i++)
            {
                __dims[__i] = __newmultiarrayNode.dimension(__i);
            }
            HotSpotResolvedObjectType __type = (HotSpotResolvedObjectType) __newmultiarrayNode.type();
            ConstantNode __hub = ConstantNode.forConstant(KlassPointerStamp.klassNonNull(), __type.klass(), this.___providers.getMetaAccess(), __graph);

            SnippetInfo __snippet = this.___newmultiarray;
            Arguments __args = new Arguments(__snippet, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("hub", __hub);
            __args.addConst("rank", __rank);
            __args.addVarargs("dimensions", int.class, StampFactory.forKind(JavaKind.Int), __dims);
            template(__newmultiarrayNode, __args).instantiate(this.___providers.getMetaAccess(), __newmultiarrayNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }

        private static int instanceSize(HotSpotResolvedObjectType __type)
        {
            return __type.instanceSize();
        }
    }
}
