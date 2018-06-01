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
        HotSpotOptimizingLocationIdentity(String name)
        {
            super(name, true);
        }

        @Override
        public abstract ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool);

        protected ValueNode findReadHub(ValueNode object)
        {
            ValueNode base = object;
            if (base instanceof CompressionNode)
            {
                base = ((CompressionNode) base).getValue();
            }
            if (base instanceof Access)
            {
                Access access = (Access) base;
                if (access.getLocationIdentity().equals(HUB_LOCATION) || access.getLocationIdentity().equals(COMPRESSED_HUB_LOCATION))
                {
                    AddressNode address = access.getAddress();
                    if (address instanceof OffsetAddressNode)
                    {
                        OffsetAddressNode offset = (OffsetAddressNode) address;
                        return offset.getBase();
                    }
                }
            }
            else if (base instanceof LoadHubNode)
            {
                LoadHubNode loadhub = (LoadHubNode) base;
                return loadhub.getValue();
            }
            return null;
        }

        /**
         * Fold reads that convert from Class -> Hub -> Class or vice versa.
         *
         * @return an earlier read or the original {@code read}
         */
        protected static ValueNode foldIndirection(ValueNode read, ValueNode object, LocationIdentity otherLocation)
        {
            if (object instanceof Access)
            {
                Access access = (Access) object;
                if (access.getLocationIdentity().equals(otherLocation))
                {
                    AddressNode address = access.getAddress();
                    if (address instanceof OffsetAddressNode)
                    {
                        OffsetAddressNode offset = (OffsetAddressNode) address;
                        return offset.getBase();
                    }
                }
            }
            return read;
        }
    }

    public static final LocationIdentity EXCEPTION_OOP_LOCATION = NamedLocationIdentity.mutable("ExceptionOop");
    public static final LocationIdentity EXCEPTION_PC_LOCATION = NamedLocationIdentity.mutable("ExceptionPc");
    public static final LocationIdentity TLAB_TOP_LOCATION = NamedLocationIdentity.mutable("TlabTop");
    public static final LocationIdentity TLAB_END_LOCATION = NamedLocationIdentity.mutable("TlabEnd");
    public static final LocationIdentity TLAB_START_LOCATION = NamedLocationIdentity.mutable("TlabStart");
    public static final LocationIdentity PENDING_EXCEPTION_LOCATION = NamedLocationIdentity.mutable("PendingException");
    public static final LocationIdentity PENDING_DEOPTIMIZATION_LOCATION = NamedLocationIdentity.mutable("PendingDeoptimization");
    public static final LocationIdentity OBJECT_RESULT_LOCATION = NamedLocationIdentity.mutable("ObjectResult");

    public static final Object readExceptionOop(Word thread)
    {
        return thread.readObject(HotSpotRuntime.threadExceptionOopOffset, EXCEPTION_OOP_LOCATION);
    }

    public static final Word readExceptionPc(Word thread)
    {
        return thread.readWord(HotSpotRuntime.threadExceptionPcOffset, EXCEPTION_PC_LOCATION);
    }

    public static final void writeExceptionOop(Word thread, Object value)
    {
        thread.writeObject(HotSpotRuntime.threadExceptionOopOffset, value, EXCEPTION_OOP_LOCATION);
    }

    public static final void writeExceptionPc(Word thread, Word value)
    {
        thread.writeWord(HotSpotRuntime.threadExceptionPcOffset, value, EXCEPTION_PC_LOCATION);
    }

    public static final Word readTlabTop(Word thread)
    {
        return thread.readWord(HotSpotRuntime.threadTlabTopOffset, TLAB_TOP_LOCATION);
    }

    public static final Word readTlabEnd(Word thread)
    {
        return thread.readWord(HotSpotRuntime.threadTlabEndOffset, TLAB_END_LOCATION);
    }

    public static final Word readTlabStart(Word thread)
    {
        return thread.readWord(HotSpotRuntime.threadTlabStartOffset, TLAB_START_LOCATION);
    }

    public static final void writeTlabTop(Word thread, Word top)
    {
        thread.writeWord(HotSpotRuntime.threadTlabTopOffset, top, TLAB_TOP_LOCATION);
    }

    public static final void initializeTlab(Word thread, Word start, Word end)
    {
        thread.writeWord(HotSpotRuntime.threadTlabStartOffset, start, TLAB_START_LOCATION);
        thread.writeWord(HotSpotRuntime.threadTlabTopOffset, start, TLAB_TOP_LOCATION);
        thread.writeWord(HotSpotRuntime.threadTlabEndOffset, end, TLAB_END_LOCATION);
    }

    /**
     * Clears the pending exception for the given thread.
     *
     * @return the pending exception, or null if there was none
     */
    public static final Object clearPendingException(Word thread)
    {
        Object result = thread.readObject(HotSpotRuntime.pendingExceptionOffset, PENDING_EXCEPTION_LOCATION);
        thread.writeObject(HotSpotRuntime.pendingExceptionOffset, null, PENDING_EXCEPTION_LOCATION);
        return result;
    }

    /**
     * Reads the pending deoptimization value for the given thread.
     *
     * @return {@code true} if there was a pending deoptimization
     */
    public static final int readPendingDeoptimization(Word thread)
    {
        return thread.readInt(HotSpotRuntime.pendingDeoptimizationOffset, PENDING_DEOPTIMIZATION_LOCATION);
    }

    /**
     * Writes the pending deoptimization value for the given thread.
     */
    public static final void writePendingDeoptimization(Word thread, int value)
    {
        thread.writeInt(HotSpotRuntime.pendingDeoptimizationOffset, value, PENDING_DEOPTIMIZATION_LOCATION);
    }

    /**
     * Gets and clears the object result from a runtime call stored in a thread local.
     *
     * @return the object that was in the thread local
     */
    public static final Object getAndClearObjectResult(Word thread)
    {
        Object result = thread.readObject(HotSpotRuntime.objectResultOffset, OBJECT_RESULT_LOCATION);
        thread.writeObject(HotSpotRuntime.objectResultOffset, null, OBJECT_RESULT_LOCATION);
        return result;
    }

    /*
     * As far as Java code is concerned this can be considered immutable: it is set just after the
     * JavaThread is created, before it is published. After that, it is never changed.
     */
    public static final LocationIdentity JAVA_THREAD_THREAD_OBJECT_LOCATION = NamedLocationIdentity.immutable("JavaThread::_threadObj");

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

    public static final LocationIdentity PROTOTYPE_MARK_WORD_LOCATION = NamedLocationIdentity.mutable("PrototypeMarkWord");

    public static final LocationIdentity KLASS_ACCESS_FLAGS_LOCATION = NamedLocationIdentity.immutable("Klass::_access_flags");

    // @closure
    public static final LocationIdentity KLASS_LAYOUT_HELPER_LOCATION = new HotSpotOptimizingLocationIdentity("Klass::_layout_helper")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            ValueNode javaObject = findReadHub(object);
            if (javaObject != null)
            {
                if (javaObject.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
                {
                    ObjectStamp stamp = (ObjectStamp) javaObject.stamp(NodeView.DEFAULT);
                    HotSpotResolvedObjectType type = (HotSpotResolvedObjectType) stamp.javaType(tool.getMetaAccess());
                    if (type.isArray() && !type.getComponentType().isPrimitive())
                    {
                        int layout = type.layoutHelper();
                        return ConstantNode.forInt(layout);
                    }
                }
            }
            return read;
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
    public static final boolean klassIsArray(KlassPointer klassNonNull)
    {
        /*
         * The less-than check only works if both values are ints. We use local variables to make
         * sure these are still ints and haven't changed.
         */
        final int layoutHelper = readLayoutHelper(klassNonNull);
        final int layoutHelperNeutralValue = HotSpotRuntime.klassLayoutHelperNeutralValue;
        return (layoutHelper < layoutHelperNeutralValue);
    }

    public static final LocationIdentity ARRAY_KLASS_COMPONENT_MIRROR = NamedLocationIdentity.immutable("ArrayKlass::_component_mirror");

    public static final LocationIdentity KLASS_SUPER_KLASS_LOCATION = NamedLocationIdentity.immutable("Klass::_super");

    public static final LocationIdentity MARK_WORD_LOCATION = NamedLocationIdentity.mutable("MarkWord");

    public static final LocationIdentity HUB_WRITE_LOCATION = NamedLocationIdentity.mutable("Hub:write");

    // @closure
    public static final LocationIdentity HUB_LOCATION = new HotSpotOptimizingLocationIdentity("Hub")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            TypeReference constantType = StampTool.typeReferenceOrNull(object);
            if (constantType != null && constantType.isExact())
            {
                return ConstantNode.forConstant(read.stamp(NodeView.DEFAULT), tool.getConstantReflection().asObjectHub(constantType.getType()), tool.getMetaAccess());
            }
            return read;
        }
    };

    // @closure
    public static final LocationIdentity COMPRESSED_HUB_LOCATION = new HotSpotOptimizingLocationIdentity("CompressedHub")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            TypeReference constantType = StampTool.typeReferenceOrNull(object);
            if (constantType != null && constantType.isExact())
            {
                return ConstantNode.forConstant(read.stamp(NodeView.DEFAULT), ((HotSpotMetaspaceConstant) tool.getConstantReflection().asObjectHub(constantType.getType())).compress(), tool.getMetaAccess());
            }
            return read;
        }
    };

    public static final void initializeObjectHeader(Word memory, Word markWord, KlassPointer hub)
    {
        memory.writeWord(HotSpotRuntime.markOffset, markWord, MARK_WORD_LOCATION);
        StoreHubNode.write(memory, hub);
    }

    // @Fold
    public static final int arrayBaseOffset(JavaKind elementKind)
    {
        return HotSpotRuntime.getArrayBaseOffset(elementKind);
    }

    // @Fold
    public static final int arrayIndexScale(JavaKind elementKind)
    {
        return HotSpotRuntime.getArrayIndexScale(elementKind);
    }

    public static final Word arrayStart(int[] a)
    {
        return WordFactory.unsigned(ComputeObjectAddressNode.get(a, HotSpotRuntime.getArrayBaseOffset(JavaKind.Int)));
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
    public static final int arrayAllocationSize(int length, int headerSize, int log2ElementSize)
    {
        int alignment = HotSpotRuntime.objectAlignment;
        int size = (length << log2ElementSize) + headerSize + (alignment - 1);
        int mask = ~(alignment - 1);
        return size & mask;
    }

    public static final int instanceHeaderSize()
    {
        return HotSpotRuntime.useCompressedClassPointers ? (2 * wordSize()) - 4 : 2 * wordSize();
    }

    public static final LocationIdentity KLASS_SUPER_CHECK_OFFSET_LOCATION = NamedLocationIdentity.immutable("Klass::_super_check_offset");
    public static final LocationIdentity SECONDARY_SUPER_CACHE_LOCATION = NamedLocationIdentity.mutable("SecondarySuperCache");
    public static final LocationIdentity SECONDARY_SUPERS_LOCATION = NamedLocationIdentity.immutable("SecondarySupers");
    public static final LocationIdentity DISPLACED_MARK_WORD_LOCATION = NamedLocationIdentity.mutable("DisplacedMarkWord");
    public static final LocationIdentity OBJECT_MONITOR_OWNER_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_owner");
    public static final LocationIdentity OBJECT_MONITOR_RECURSION_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_recursions");
    public static final LocationIdentity OBJECT_MONITOR_CXQ_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_cxq");
    public static final LocationIdentity OBJECT_MONITOR_ENTRY_LIST_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_EntryList");

    /**
     * Loads the hub of an object (without null checking it first).
     */
    public static final KlassPointer loadHub(Object object)
    {
        return loadHubIntrinsic(object);
    }

    public static final Word loadWordFromObject(Object object, int offset)
    {
        return loadWordFromObjectIntrinsic(object, offset, LocationIdentity.any(), getWordKind());
    }

    public static final Word loadWordFromObject(Object object, int offset, LocationIdentity identity)
    {
        return loadWordFromObjectIntrinsic(object, offset, identity, getWordKind());
    }

    public static final KlassPointer loadKlassFromObject(Object object, int offset, LocationIdentity identity)
    {
        return loadKlassFromObjectIntrinsic(object, offset, identity, getWordKind());
    }

    /**
     * Reads the value of a given register.
     *
     * @param register a register which must not be available to the register allocator
     * @return the value of {@code register} as a word
     */
    public static final Word registerAsWord(@ConstantNodeParameter Register register)
    {
        return registerAsWord(register, true, false);
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

    public static final LocationIdentity CLASS_STATE_LOCATION = NamedLocationIdentity.mutable("ClassState");

    /**
     * @param hub the hub of an InstanceKlass
     * @return true is the InstanceKlass represented by hub is fully initialized
     */
    public static final boolean isInstanceKlassFullyInitialized(KlassPointer hub)
    {
        return readInstanceKlassState(hub) == HotSpotRuntime.instanceKlassStateFullyInitialized;
    }

    private static final byte readInstanceKlassState(KlassPointer hub)
    {
        return hub.readByte(HotSpotRuntime.instanceKlassInitStateOffset, CLASS_STATE_LOCATION);
    }

    public static final LocationIdentity KLASS_MODIFIER_FLAGS_LOCATION = NamedLocationIdentity.immutable("Klass::_modifier_flags");

    // @closure
    public static final LocationIdentity CLASS_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("Class._klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            return foldIndirection(read, object, CLASS_MIRROR_LOCATION);
        }
    };

    // @closure
    public static final LocationIdentity CLASS_ARRAY_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("Class._array_klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            return foldIndirection(read, object, ARRAY_KLASS_COMPONENT_MIRROR);
        }
    };

    public static final LocationIdentity CLASS_MIRROR_LOCATION = NamedLocationIdentity.immutable("Klass::_java_mirror");
    public static final LocationIdentity CLASS_MIRROR_HANDLE_LOCATION = NamedLocationIdentity.immutable("Klass::_java_mirror handle");
    public static final LocationIdentity HEAP_TOP_LOCATION = NamedLocationIdentity.mutable("HeapTop");
    public static final LocationIdentity HEAP_END_LOCATION = NamedLocationIdentity.mutable("HeapEnd");
    public static final LocationIdentity TLAB_SIZE_LOCATION = NamedLocationIdentity.mutable("TlabSize");
    public static final LocationIdentity TLAB_THREAD_ALLOCATED_BYTES_LOCATION = NamedLocationIdentity.mutable("TlabThreadAllocatedBytes");
    public static final LocationIdentity TLAB_REFILL_WASTE_LIMIT_LOCATION = NamedLocationIdentity.mutable("RefillWasteLimit");
    public static final LocationIdentity TLAB_NOF_REFILLS_LOCATION = NamedLocationIdentity.mutable("TlabNOfRefills");
    public static final LocationIdentity TLAB_FAST_REFILL_WASTE_LOCATION = NamedLocationIdentity.mutable("TlabFastRefillWaste");
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
        catch (Exception e)
        {
            throw new GraalError(e);
        }
    }

    // @closure
    public static final LocationIdentity OBJ_ARRAY_KLASS_ELEMENT_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("ObjArrayKlass::_element_klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            ValueNode javaObject = findReadHub(object);
            if (javaObject != null)
            {
                ResolvedJavaType type = StampTool.typeOrNull(javaObject);
                if (type != null && type.isArray())
                {
                    ResolvedJavaType element = type.getComponentType();
                    if (element != null && !element.isPrimitive() && !element.getElementalType().isInterface())
                    {
                        Assumptions assumptions = object.graph().getAssumptions();
                        AssumptionResult<ResolvedJavaType> leafType = element.findLeafConcreteSubtype();
                        if (leafType != null && leafType.canRecordTo(assumptions))
                        {
                            leafType.recordTo(assumptions);
                            return ConstantNode.forConstant(read.stamp(NodeView.DEFAULT), tool.getConstantReflection().asObjectHub(leafType.getResult()), tool.getMetaAccess());
                        }
                    }
                }
            }
            return read;
        }
    };

    public static final LocationIdentity PRIMARY_SUPERS_LOCATION = NamedLocationIdentity.immutable("PrimarySupers");
    public static final LocationIdentity METASPACE_ARRAY_LENGTH_LOCATION = NamedLocationIdentity.immutable("MetaspaceArrayLength");
    public static final LocationIdentity SECONDARY_SUPERS_ELEMENT_LOCATION = NamedLocationIdentity.immutable("SecondarySupersElement");
}
