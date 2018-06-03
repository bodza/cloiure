package giraaff.hotspot.replacements;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.WordFactory;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.CanonicalizableLocation;
import giraaff.nodes.CompressionNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.extended.StoreHubNode;
import giraaff.nodes.memory.Access;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.type.StampTool;
import giraaff.replacements.nodes.ReadRegisterNode;
import giraaff.replacements.nodes.WriteRegisterNode;
import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;
import giraaff.word.Word;

/**
 * A collection of methods used in HotSpot snippets, substitutions and stubs.
 */
// @class HotSpotReplacementsUtil
public final class HotSpotReplacementsUtil
{
    // @cons
    private HotSpotReplacementsUtil()
    {
        super();
    }

    // @class HotSpotReplacementsUtil.HotSpotOptimizingLocationIdentity
    abstract static class HotSpotOptimizingLocationIdentity extends NamedLocationIdentity implements CanonicalizableLocation
    {
        // @cons
        HotSpotOptimizingLocationIdentity(String __name)
        {
            super(__name, true);
        }

        @Override
        public abstract ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool);

        protected ValueNode findReadHub(ValueNode __object)
        {
            ValueNode __base = __object;
            if (__base instanceof CompressionNode)
            {
                __base = ((CompressionNode) __base).getValue();
            }
            if (__base instanceof Access)
            {
                Access __access = (Access) __base;
                if (__access.getLocationIdentity().equals(HUB_LOCATION) || __access.getLocationIdentity().equals(COMPRESSED_HUB_LOCATION))
                {
                    AddressNode __address = __access.getAddress();
                    if (__address instanceof OffsetAddressNode)
                    {
                        OffsetAddressNode __offset = (OffsetAddressNode) __address;
                        return __offset.getBase();
                    }
                }
            }
            else if (__base instanceof LoadHubNode)
            {
                LoadHubNode __loadhub = (LoadHubNode) __base;
                return __loadhub.getValue();
            }
            return null;
        }

        /**
         * Fold reads that convert from Class -> Hub -> Class or vice versa.
         *
         * @return an earlier read or the original {@code read}
         */
        protected static ValueNode foldIndirection(ValueNode __read, ValueNode __object, LocationIdentity __otherLocation)
        {
            if (__object instanceof Access)
            {
                Access __access = (Access) __object;
                if (__access.getLocationIdentity().equals(__otherLocation))
                {
                    AddressNode __address = __access.getAddress();
                    if (__address instanceof OffsetAddressNode)
                    {
                        OffsetAddressNode __offset = (OffsetAddressNode) __address;
                        return __offset.getBase();
                    }
                }
            }
            return __read;
        }
    }

    // @def
    public static final LocationIdentity EXCEPTION_OOP_LOCATION = NamedLocationIdentity.mutable("ExceptionOop");
    // @def
    public static final LocationIdentity EXCEPTION_PC_LOCATION = NamedLocationIdentity.mutable("ExceptionPc");
    // @def
    public static final LocationIdentity TLAB_TOP_LOCATION = NamedLocationIdentity.mutable("TlabTop");
    // @def
    public static final LocationIdentity TLAB_END_LOCATION = NamedLocationIdentity.mutable("TlabEnd");
    // @def
    public static final LocationIdentity TLAB_START_LOCATION = NamedLocationIdentity.mutable("TlabStart");
    // @def
    public static final LocationIdentity PENDING_EXCEPTION_LOCATION = NamedLocationIdentity.mutable("PendingException");
    // @def
    public static final LocationIdentity PENDING_DEOPTIMIZATION_LOCATION = NamedLocationIdentity.mutable("PendingDeoptimization");
    // @def
    public static final LocationIdentity OBJECT_RESULT_LOCATION = NamedLocationIdentity.mutable("ObjectResult");

    public static final Object readExceptionOop(Word __thread)
    {
        return __thread.readObject(HotSpotRuntime.threadExceptionOopOffset, EXCEPTION_OOP_LOCATION);
    }

    public static final Word readExceptionPc(Word __thread)
    {
        return __thread.readWord(HotSpotRuntime.threadExceptionPcOffset, EXCEPTION_PC_LOCATION);
    }

    public static final void writeExceptionOop(Word __thread, Object __value)
    {
        __thread.writeObject(HotSpotRuntime.threadExceptionOopOffset, __value, EXCEPTION_OOP_LOCATION);
    }

    public static final void writeExceptionPc(Word __thread, Word __value)
    {
        __thread.writeWord(HotSpotRuntime.threadExceptionPcOffset, __value, EXCEPTION_PC_LOCATION);
    }

    public static final Word readTlabTop(Word __thread)
    {
        return __thread.readWord(HotSpotRuntime.threadTlabTopOffset, TLAB_TOP_LOCATION);
    }

    public static final Word readTlabEnd(Word __thread)
    {
        return __thread.readWord(HotSpotRuntime.threadTlabEndOffset, TLAB_END_LOCATION);
    }

    public static final Word readTlabStart(Word __thread)
    {
        return __thread.readWord(HotSpotRuntime.threadTlabStartOffset, TLAB_START_LOCATION);
    }

    public static final void writeTlabTop(Word __thread, Word __top)
    {
        __thread.writeWord(HotSpotRuntime.threadTlabTopOffset, __top, TLAB_TOP_LOCATION);
    }

    public static final void initializeTlab(Word __thread, Word __start, Word __end)
    {
        __thread.writeWord(HotSpotRuntime.threadTlabStartOffset, __start, TLAB_START_LOCATION);
        __thread.writeWord(HotSpotRuntime.threadTlabTopOffset, __start, TLAB_TOP_LOCATION);
        __thread.writeWord(HotSpotRuntime.threadTlabEndOffset, __end, TLAB_END_LOCATION);
    }

    /**
     * Clears the pending exception for the given thread.
     *
     * @return the pending exception, or null if there was none
     */
    public static final Object clearPendingException(Word __thread)
    {
        Object __result = __thread.readObject(HotSpotRuntime.pendingExceptionOffset, PENDING_EXCEPTION_LOCATION);
        __thread.writeObject(HotSpotRuntime.pendingExceptionOffset, null, PENDING_EXCEPTION_LOCATION);
        return __result;
    }

    /**
     * Reads the pending deoptimization value for the given thread.
     *
     * @return {@code true} if there was a pending deoptimization
     */
    public static final int readPendingDeoptimization(Word __thread)
    {
        return __thread.readInt(HotSpotRuntime.pendingDeoptimizationOffset, PENDING_DEOPTIMIZATION_LOCATION);
    }

    /**
     * Writes the pending deoptimization value for the given thread.
     */
    public static final void writePendingDeoptimization(Word __thread, int __value)
    {
        __thread.writeInt(HotSpotRuntime.pendingDeoptimizationOffset, __value, PENDING_DEOPTIMIZATION_LOCATION);
    }

    /**
     * Gets and clears the object result from a runtime call stored in a thread local.
     *
     * @return the object that was in the thread local
     */
    public static final Object getAndClearObjectResult(Word __thread)
    {
        Object __result = __thread.readObject(HotSpotRuntime.objectResultOffset, OBJECT_RESULT_LOCATION);
        __thread.writeObject(HotSpotRuntime.objectResultOffset, null, OBJECT_RESULT_LOCATION);
        return __result;
    }

    /*
     * As far as Java code is concerned this can be considered immutable: it is set just after the
     * JavaThread is created, before it is published. After that, it is never changed.
     */
    // @def
    public static final LocationIdentity JAVA_THREAD_THREAD_OBJECT_LOCATION = NamedLocationIdentity.immutable("JavaThread::_threadObj");

    // @def
    public static final LocationIdentity JAVA_THREAD_OSTHREAD_LOCATION = NamedLocationIdentity.mutable("JavaThread::_osthread");

    // @Fold
    public static final JavaKind getWordKind()
    {
        return HotSpotRuntime.JVMCI.getHostJVMCIBackend().getCodeCache().getTarget().wordJavaKind;
    }

    // @Fold
    public static final int wordSize()
    {
        return HotSpotRuntime.JVMCI.getHostJVMCIBackend().getCodeCache().getTarget().wordSize;
    }

    // @Fold
    public static final int pageSize()
    {
        return UnsafeAccess.UNSAFE.pageSize();
    }

    // @def
    public static final LocationIdentity PROTOTYPE_MARK_WORD_LOCATION = NamedLocationIdentity.mutable("PrototypeMarkWord");

    // @def
    public static final LocationIdentity KLASS_ACCESS_FLAGS_LOCATION = NamedLocationIdentity.immutable("Klass::_access_flags");

    // @closure
    public static final LocationIdentity KLASS_LAYOUT_HELPER_LOCATION = new HotSpotOptimizingLocationIdentity("Klass::_layout_helper")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode __read, AddressNode __location, ValueNode __object, CanonicalizerTool __tool)
        {
            ValueNode __javaObject = findReadHub(__object);
            if (__javaObject != null)
            {
                if (__javaObject.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
                {
                    ObjectStamp __stamp = (ObjectStamp) __javaObject.stamp(NodeView.DEFAULT);
                    HotSpotResolvedObjectType __type = (HotSpotResolvedObjectType) __stamp.javaType(__tool.getMetaAccess());
                    if (__type.isArray() && !__type.getComponentType().isPrimitive())
                    {
                        int __layout = __type.layoutHelper();
                        return ConstantNode.forInt(__layout);
                    }
                }
            }
            return __read;
        }
    };

    @NodeIntrinsic(value = KlassLayoutHelperNode.class)
    public static native int readLayoutHelper(KlassPointer object);

    /**
     * Checks if class {@code klass} is an array.
     *
     * See: Klass::layout_helper_is_array
     *
     * @param klassNonNull the class to be checked
     * @return true if klassNonNull is an array, false otherwise
     */
    public static final boolean klassIsArray(KlassPointer __klassNonNull)
    {
        /*
         * The less-than check only works if both values are ints. We use local variables to make
         * sure these are still ints and haven't changed.
         */
        final int __layoutHelper = readLayoutHelper(__klassNonNull);
        final int __layoutHelperNeutralValue = HotSpotRuntime.klassLayoutHelperNeutralValue;
        return (__layoutHelper < __layoutHelperNeutralValue);
    }

    // @def
    public static final LocationIdentity ARRAY_KLASS_COMPONENT_MIRROR = NamedLocationIdentity.immutable("ArrayKlass::_component_mirror");

    // @def
    public static final LocationIdentity KLASS_SUPER_KLASS_LOCATION = NamedLocationIdentity.immutable("Klass::_super");

    // @def
    public static final LocationIdentity MARK_WORD_LOCATION = NamedLocationIdentity.mutable("MarkWord");

    // @def
    public static final LocationIdentity HUB_WRITE_LOCATION = NamedLocationIdentity.mutable("Hub:write");

    // @closure
    public static final LocationIdentity HUB_LOCATION = new HotSpotOptimizingLocationIdentity("Hub")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode __read, AddressNode __location, ValueNode __object, CanonicalizerTool __tool)
        {
            TypeReference __constantType = StampTool.typeReferenceOrNull(__object);
            if (__constantType != null && __constantType.isExact())
            {
                return ConstantNode.forConstant(__read.stamp(NodeView.DEFAULT), __tool.getConstantReflection().asObjectHub(__constantType.getType()), __tool.getMetaAccess());
            }
            return __read;
        }
    };

    // @closure
    public static final LocationIdentity COMPRESSED_HUB_LOCATION = new HotSpotOptimizingLocationIdentity("CompressedHub")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode __read, AddressNode __location, ValueNode __object, CanonicalizerTool __tool)
        {
            TypeReference __constantType = StampTool.typeReferenceOrNull(__object);
            if (__constantType != null && __constantType.isExact())
            {
                return ConstantNode.forConstant(__read.stamp(NodeView.DEFAULT), ((HotSpotMetaspaceConstant) __tool.getConstantReflection().asObjectHub(__constantType.getType())).compress(), __tool.getMetaAccess());
            }
            return __read;
        }
    };

    public static final void initializeObjectHeader(Word __memory, Word __markWord, KlassPointer __hub)
    {
        __memory.writeWord(HotSpotRuntime.markOffset, __markWord, MARK_WORD_LOCATION);
        StoreHubNode.write(__memory, __hub);
    }

    // @Fold
    public static final int arrayBaseOffset(JavaKind __elementKind)
    {
        return HotSpotRuntime.getArrayBaseOffset(__elementKind);
    }

    // @Fold
    public static final int arrayIndexScale(JavaKind __elementKind)
    {
        return HotSpotRuntime.getArrayIndexScale(__elementKind);
    }

    public static final Word arrayStart(int[] __a)
    {
        return WordFactory.unsigned(ComputeObjectAddressNode.get(__a, HotSpotRuntime.getArrayBaseOffset(JavaKind.Int)));
    }

    /**
     * Computes the size of the memory chunk allocated for an array. This size accounts for the
     * array header size, body size and any padding after the last element to satisfy object
     * alignment requirements.
     *
     * @param length the number of elements in the array
     * @param headerSize the size of the array header
     * @param log2ElementSize log2 of the size of an element in the array
     *
     * @return the size of the memory chunk
     */
    public static final int arrayAllocationSize(int __length, int __headerSize, int __log2ElementSize)
    {
        int __alignment = HotSpotRuntime.objectAlignment;
        int __size = (__length << __log2ElementSize) + __headerSize + (__alignment - 1);
        int __mask = ~(__alignment - 1);
        return __size & __mask;
    }

    public static final int instanceHeaderSize()
    {
        return HotSpotRuntime.useCompressedClassPointers ? (2 * wordSize()) - 4 : 2 * wordSize();
    }

    // @def
    public static final LocationIdentity KLASS_SUPER_CHECK_OFFSET_LOCATION = NamedLocationIdentity.immutable("Klass::_super_check_offset");
    // @def
    public static final LocationIdentity SECONDARY_SUPER_CACHE_LOCATION = NamedLocationIdentity.mutable("SecondarySuperCache");
    // @def
    public static final LocationIdentity SECONDARY_SUPERS_LOCATION = NamedLocationIdentity.immutable("SecondarySupers");
    // @def
    public static final LocationIdentity DISPLACED_MARK_WORD_LOCATION = NamedLocationIdentity.mutable("DisplacedMarkWord");
    // @def
    public static final LocationIdentity OBJECT_MONITOR_OWNER_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_owner");
    // @def
    public static final LocationIdentity OBJECT_MONITOR_RECURSION_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_recursions");
    // @def
    public static final LocationIdentity OBJECT_MONITOR_CXQ_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_cxq");
    // @def
    public static final LocationIdentity OBJECT_MONITOR_ENTRY_LIST_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_EntryList");

    /**
     * Loads the hub of an object (without null checking it first).
     */
    public static final KlassPointer loadHub(Object __object)
    {
        return loadHubIntrinsic(__object);
    }

    public static final Word loadWordFromObject(Object __object, int __offset)
    {
        return loadWordFromObjectIntrinsic(__object, __offset, LocationIdentity.any(), getWordKind());
    }

    public static final Word loadWordFromObject(Object __object, int __offset, LocationIdentity __identity)
    {
        return loadWordFromObjectIntrinsic(__object, __offset, __identity, getWordKind());
    }

    public static final KlassPointer loadKlassFromObject(Object __object, int __offset, LocationIdentity __identity)
    {
        return loadKlassFromObjectIntrinsic(__object, __offset, __identity, getWordKind());
    }

    /**
     * Reads the value of a given register.
     *
     * @param register a register which must not be available to the register allocator
     * @return the value of {@code register} as a word
     */
    public static final Word registerAsWord(@ConstantNodeParameter Register __register)
    {
        return registerAsWord(__register, true, false);
    }

    @NodeIntrinsic(value = ReadRegisterNode.class)
    public static native Word registerAsWord(@ConstantNodeParameter Register register, @ConstantNodeParameter boolean directUse, @ConstantNodeParameter boolean incoming);

    @NodeIntrinsic(value = WriteRegisterNode.class)
    public static native void writeRegisterAsWord(@ConstantNodeParameter Register register, Word value);

    @NodeIntrinsic(value = RawLoadNode.class)
    private static native Word loadWordFromObjectIntrinsic(Object object, long offset, @ConstantNodeParameter LocationIdentity locationIdentity, @ConstantNodeParameter JavaKind wordKind);

    @NodeIntrinsic(value = RawLoadNode.class)
    private static native KlassPointer loadKlassFromObjectIntrinsic(Object object, long offset, @ConstantNodeParameter LocationIdentity locationIdentity, @ConstantNodeParameter JavaKind wordKind);

    @NodeIntrinsic(value = LoadHubNode.class)
    public static native KlassPointer loadHubIntrinsic(Object object);

    // @Fold
    public static final int log2WordSize()
    {
        return CodeUtil.log2(wordSize());
    }

    // @def
    public static final LocationIdentity CLASS_STATE_LOCATION = NamedLocationIdentity.mutable("ClassState");

    /**
     * @param hub the hub of an InstanceKlass
     * @return true is the InstanceKlass represented by hub is fully initialized
     */
    public static final boolean isInstanceKlassFullyInitialized(KlassPointer __hub)
    {
        return readInstanceKlassState(__hub) == HotSpotRuntime.instanceKlassStateFullyInitialized;
    }

    private static final byte readInstanceKlassState(KlassPointer __hub)
    {
        return __hub.readByte(HotSpotRuntime.instanceKlassInitStateOffset, CLASS_STATE_LOCATION);
    }

    // @def
    public static final LocationIdentity KLASS_MODIFIER_FLAGS_LOCATION = NamedLocationIdentity.immutable("Klass::_modifier_flags");

    // @closure
    public static final LocationIdentity CLASS_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("Class._klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode __read, AddressNode __location, ValueNode __object, CanonicalizerTool __tool)
        {
            return foldIndirection(__read, __object, CLASS_MIRROR_LOCATION);
        }
    };

    // @closure
    public static final LocationIdentity CLASS_ARRAY_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("Class._array_klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode __read, AddressNode __location, ValueNode __object, CanonicalizerTool __tool)
        {
            return foldIndirection(__read, __object, ARRAY_KLASS_COMPONENT_MIRROR);
        }
    };

    // @def
    public static final LocationIdentity CLASS_MIRROR_LOCATION = NamedLocationIdentity.immutable("Klass::_java_mirror");
    // @def
    public static final LocationIdentity CLASS_MIRROR_HANDLE_LOCATION = NamedLocationIdentity.immutable("Klass::_java_mirror handle");
    // @def
    public static final LocationIdentity HEAP_TOP_LOCATION = NamedLocationIdentity.mutable("HeapTop");
    // @def
    public static final LocationIdentity HEAP_END_LOCATION = NamedLocationIdentity.mutable("HeapEnd");
    // @def
    public static final LocationIdentity TLAB_SIZE_LOCATION = NamedLocationIdentity.mutable("TlabSize");
    // @def
    public static final LocationIdentity TLAB_THREAD_ALLOCATED_BYTES_LOCATION = NamedLocationIdentity.mutable("TlabThreadAllocatedBytes");
    // @def
    public static final LocationIdentity TLAB_REFILL_WASTE_LIMIT_LOCATION = NamedLocationIdentity.mutable("RefillWasteLimit");
    // @def
    public static final LocationIdentity TLAB_NOF_REFILLS_LOCATION = NamedLocationIdentity.mutable("TlabNOfRefills");
    // @def
    public static final LocationIdentity TLAB_FAST_REFILL_WASTE_LOCATION = NamedLocationIdentity.mutable("TlabFastRefillWaste");
    // @def
    public static final LocationIdentity TLAB_SLOW_ALLOCATIONS_LOCATION = NamedLocationIdentity.mutable("TlabSlowAllocations");

    @NodeIntrinsic(ForeignCallNode.class)
    public static native int identityHashCode(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object object);

    // @Fold
    public static final long referentOffset()
    {
        try
        {
            return UnsafeAccess.UNSAFE.objectFieldOffset(java.lang.ref.Reference.class.getDeclaredField("referent"));
        }
        catch (Exception __e)
        {
            throw new GraalError(__e);
        }
    }

    // @closure
    public static final LocationIdentity OBJ_ARRAY_KLASS_ELEMENT_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("ObjArrayKlass::_element_klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode __read, AddressNode __location, ValueNode __object, CanonicalizerTool __tool)
        {
            ValueNode __javaObject = findReadHub(__object);
            if (__javaObject != null)
            {
                ResolvedJavaType __type = StampTool.typeOrNull(__javaObject);
                if (__type != null && __type.isArray())
                {
                    ResolvedJavaType __element = __type.getComponentType();
                    if (__element != null && !__element.isPrimitive() && !__element.getElementalType().isInterface())
                    {
                        Assumptions __assumptions = __object.graph().getAssumptions();
                        AssumptionResult<ResolvedJavaType> __leafType = __element.findLeafConcreteSubtype();
                        if (__leafType != null && __leafType.canRecordTo(__assumptions))
                        {
                            __leafType.recordTo(__assumptions);
                            return ConstantNode.forConstant(__read.stamp(NodeView.DEFAULT), __tool.getConstantReflection().asObjectHub(__leafType.getResult()), __tool.getMetaAccess());
                        }
                    }
                }
            }
            return __read;
        }
    };

    // @def
    public static final LocationIdentity PRIMARY_SUPERS_LOCATION = NamedLocationIdentity.immutable("PrimarySupers");
    // @def
    public static final LocationIdentity METASPACE_ARRAY_LENGTH_LOCATION = NamedLocationIdentity.immutable("MetaspaceArrayLength");
    // @def
    public static final LocationIdentity SECONDARY_SUPERS_ELEMENT_LOCATION = NamedLocationIdentity.immutable("SecondarySupersElement");
}
