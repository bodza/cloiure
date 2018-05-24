package giraaff.hotspot.replacements;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;
import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Fold;
import giraaff.api.replacements.Fold.InjectedParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
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
public class HotSpotReplacementsUtil
{
    abstract static class HotSpotOptimizingLocationIdentity extends NamedLocationIdentity implements CanonicalizableLocation
    {
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

    public static HotSpotJVMCIRuntimeProvider runtime()
    {
        return HotSpotJVMCIRuntime.runtime();
    }

    @Fold
    public static GraalHotSpotVMConfig config(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config;
    }

    @Fold
    public static boolean useTLAB(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.useTLAB;
    }

    public static final LocationIdentity EXCEPTION_OOP_LOCATION = NamedLocationIdentity.mutable("ExceptionOop");

    /**
     * @see GraalHotSpotVMConfig#threadExceptionOopOffset
     */
    @Fold
    public static int threadExceptionOopOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadExceptionOopOffset;
    }

    public static final LocationIdentity EXCEPTION_PC_LOCATION = NamedLocationIdentity.mutable("ExceptionPc");

    @Fold
    public static int threadExceptionPcOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadExceptionPcOffset;
    }

    public static final LocationIdentity TLAB_TOP_LOCATION = NamedLocationIdentity.mutable("TlabTop");

    @Fold
    public static int threadTlabTopOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadTlabTopOffset();
    }

    public static final LocationIdentity TLAB_END_LOCATION = NamedLocationIdentity.mutable("TlabEnd");

    @Fold
    static int threadTlabEndOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadTlabEndOffset();
    }

    public static final LocationIdentity TLAB_START_LOCATION = NamedLocationIdentity.mutable("TlabStart");

    @Fold
    static int threadTlabStartOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadTlabStartOffset();
    }

    public static final LocationIdentity PENDING_EXCEPTION_LOCATION = NamedLocationIdentity.mutable("PendingException");

    /**
     * @see GraalHotSpotVMConfig#pendingExceptionOffset
     */
    @Fold
    static int threadPendingExceptionOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.pendingExceptionOffset;
    }

    public static final LocationIdentity PENDING_DEOPTIMIZATION_LOCATION = NamedLocationIdentity.mutable("PendingDeoptimization");

    /**
     * @see GraalHotSpotVMConfig#pendingDeoptimizationOffset
     */
    @Fold
    static int threadPendingDeoptimizationOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.pendingDeoptimizationOffset;
    }

    public static final LocationIdentity OBJECT_RESULT_LOCATION = NamedLocationIdentity.mutable("ObjectResult");

    @Fold
    static int objectResultOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadObjectResultOffset;
    }

    /**
     * @see GraalHotSpotVMConfig#threadExceptionOopOffset
     */
    public static Object readExceptionOop(Word thread)
    {
        return thread.readObject(threadExceptionOopOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), EXCEPTION_OOP_LOCATION);
    }

    public static Word readExceptionPc(Word thread)
    {
        return thread.readWord(threadExceptionPcOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), EXCEPTION_PC_LOCATION);
    }

    /**
     * @see GraalHotSpotVMConfig#threadExceptionOopOffset
     */
    public static void writeExceptionOop(Word thread, Object value)
    {
        thread.writeObject(threadExceptionOopOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), value, EXCEPTION_OOP_LOCATION);
    }

    public static void writeExceptionPc(Word thread, Word value)
    {
        thread.writeWord(threadExceptionPcOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), value, EXCEPTION_PC_LOCATION);
    }

    public static Word readTlabTop(Word thread)
    {
        return thread.readWord(threadTlabTopOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), TLAB_TOP_LOCATION);
    }

    public static Word readTlabEnd(Word thread)
    {
        return thread.readWord(threadTlabEndOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), TLAB_END_LOCATION);
    }

    public static Word readTlabStart(Word thread)
    {
        return thread.readWord(threadTlabStartOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), TLAB_START_LOCATION);
    }

    public static void writeTlabTop(Word thread, Word top)
    {
        thread.writeWord(threadTlabTopOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), top, TLAB_TOP_LOCATION);
    }

    public static void initializeTlab(Word thread, Word start, Word end)
    {
        thread.writeWord(threadTlabStartOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), start, TLAB_START_LOCATION);
        thread.writeWord(threadTlabTopOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), start, TLAB_TOP_LOCATION);
        thread.writeWord(threadTlabEndOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), end, TLAB_END_LOCATION);
    }

    /**
     * Clears the pending exception for the given thread.
     *
     * @return the pending exception, or null if there was none
     */
    public static Object clearPendingException(Word thread)
    {
        Object result = thread.readObject(threadPendingExceptionOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), PENDING_EXCEPTION_LOCATION);
        thread.writeObject(threadPendingExceptionOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), null, PENDING_EXCEPTION_LOCATION);
        return result;
    }

    /**
     * Reads the pending deoptimization value for the given thread.
     *
     * @return {@code true} if there was a pending deoptimization
     */
    public static int readPendingDeoptimization(Word thread)
    {
        return thread.readInt(threadPendingDeoptimizationOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), PENDING_DEOPTIMIZATION_LOCATION);
    }

    /**
     * Writes the pending deoptimization value for the given thread.
     */
    public static void writePendingDeoptimization(Word thread, int value)
    {
        thread.writeInt(threadPendingDeoptimizationOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), value, PENDING_DEOPTIMIZATION_LOCATION);
    }

    /**
     * Gets and clears the object result from a runtime call stored in a thread local.
     *
     * @return the object that was in the thread local
     */
    public static Object getAndClearObjectResult(Word thread)
    {
        Object result = thread.readObject(objectResultOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), OBJECT_RESULT_LOCATION);
        thread.writeObject(objectResultOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), null, OBJECT_RESULT_LOCATION);
        return result;
    }

    /*
     * As far as Java code is concerned this can be considered immutable: it is set just after the
     * JavaThread is created, before it is published. After that, it is never changed.
     */
    public static final LocationIdentity JAVA_THREAD_THREAD_OBJECT_LOCATION = NamedLocationIdentity.immutable("JavaThread::_threadObj");

    @Fold
    public static int threadObjectOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadObjectOffset;
    }

    public static final LocationIdentity JAVA_THREAD_OSTHREAD_LOCATION = NamedLocationIdentity.mutable("JavaThread::_osthread");

    @Fold
    public static int osThreadOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.osThreadOffset;
    }

    @Fold
    public static int osThreadInterruptedOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.osThreadInterruptedOffset;
    }

    @Fold
    public static JavaKind getWordKind()
    {
        return runtime().getHostJVMCIBackend().getCodeCache().getTarget().wordJavaKind;
    }

    @Fold
    public static int wordSize()
    {
        return runtime().getHostJVMCIBackend().getCodeCache().getTarget().wordSize;
    }

    @Fold
    public static int pageSize()
    {
        return UnsafeAccess.UNSAFE.pageSize();
    }

    @Fold
    public static int heapWordSize(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.heapWordSize;
    }

    public static final LocationIdentity PROTOTYPE_MARK_WORD_LOCATION = NamedLocationIdentity.mutable("PrototypeMarkWord");

    @Fold
    public static int prototypeMarkWordOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.prototypeMarkWordOffset;
    }

    @Fold
    public static long arrayPrototypeMarkWord(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.arrayPrototypeMarkWord();
    }

    public static final LocationIdentity KLASS_ACCESS_FLAGS_LOCATION = NamedLocationIdentity.immutable("Klass::_access_flags");

    @Fold
    public static int klassAccessFlagsOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.klassAccessFlagsOffset;
    }

    @Fold
    public static int jvmAccWrittenFlags(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.jvmAccWrittenFlags;
    }

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

    @Fold
    public static int klassLayoutHelperOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.klassLayoutHelperOffset;
    }

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
    public static boolean klassIsArray(KlassPointer klassNonNull)
    {
        /*
         * The less-than check only works if both values are ints. We use local variables to make
         * sure these are still ints and haven't changed.
         */
        final int layoutHelper = readLayoutHelper(klassNonNull);
        final int layoutHelperNeutralValue = config(GraalHotSpotVMConfig.INJECTED_VMCONFIG).klassLayoutHelperNeutralValue;
        return (layoutHelper < layoutHelperNeutralValue);
    }

    public static final LocationIdentity ARRAY_KLASS_COMPONENT_MIRROR = NamedLocationIdentity.immutable("ArrayKlass::_component_mirror");

    @Fold
    public static int arrayKlassComponentMirrorOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.getFieldOffset("ArrayKlass::_component_mirror", Integer.class, "oop");
    }

    public static final LocationIdentity KLASS_SUPER_KLASS_LOCATION = NamedLocationIdentity.immutable("Klass::_super");

    @Fold
    public static int klassSuperKlassOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.klassSuperKlassOffset;
    }

    public static final LocationIdentity MARK_WORD_LOCATION = NamedLocationIdentity.mutable("MarkWord");

    @Fold
    public static int markOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.markOffset;
    }

    public static final LocationIdentity HUB_WRITE_LOCATION = NamedLocationIdentity.mutable("Hub:write");

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

    @Fold
    static int hubOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.hubOffset;
    }

    public static void initializeObjectHeader(Word memory, Word markWord, KlassPointer hub)
    {
        memory.writeWord(markOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), markWord, MARK_WORD_LOCATION);
        StoreHubNode.write(memory, hub);
    }

    @Fold
    public static int unlockedMask(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.unlockedMask;
    }

    @Fold
    public static int monitorMask(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.monitorMask;
    }

    @Fold
    public static int objectMonitorOwnerOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.objectMonitorOwner;
    }

    @Fold
    public static int objectMonitorRecursionsOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.objectMonitorRecursions;
    }

    @Fold
    public static int objectMonitorCxqOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.objectMonitorCxq;
    }

    @Fold
    public static int objectMonitorEntryListOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.objectMonitorEntryList;
    }

    /**
     * Mask for a biasable, locked or unlocked mark word.
     *
     * <pre>
     * +----------------------------------+-+-+
     * |                                 1|1|1|
     * +----------------------------------+-+-+
     * </pre>
     *
     */
    @Fold
    public static int biasedLockMaskInPlace(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.biasedLockMaskInPlace;
    }

    @Fold
    public static int epochMaskInPlace(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.epochMaskInPlace;
    }

    /**
     * Pattern for a biasable, unlocked mark word.
     *
     * <pre>
     * +----------------------------------+-+-+
     * |                                 1|0|1|
     * +----------------------------------+-+-+
     * </pre>
     *
     */
    @Fold
    public static int biasedLockPattern(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.biasedLockPattern;
    }

    @Fold
    public static int ageMaskInPlace(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.ageMaskInPlace;
    }

    @Fold
    public static int metaspaceArrayLengthOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.metaspaceArrayLengthOffset;
    }

    @Fold
    public static int metaspaceArrayBaseOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.metaspaceArrayBaseOffset;
    }

    @Fold
    public static int arrayLengthOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.arrayOopDescLengthOffset();
    }

    @Fold
    public static int arrayBaseOffset(JavaKind elementKind)
    {
        return HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(elementKind);
    }

    @Fold
    public static int arrayIndexScale(JavaKind elementKind)
    {
        return HotSpotJVMCIRuntimeProvider.getArrayIndexScale(elementKind);
    }

    public static Word arrayStart(int[] a)
    {
        return WordFactory.unsigned(ComputeObjectAddressNode.get(a, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(JavaKind.Int)));
    }

    /**
     * Idiom for making {@link GraalHotSpotVMConfig} a constant.
     */
    @Fold
    public static GraalHotSpotVMConfig getConfig(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config;
    }

    /**
     * Calls {@link #arrayAllocationSize(int, int, int, GraalHotSpotVMConfig)} using an injected VM
     * configuration object.
     */
    public static int arrayAllocationSize(int length, int headerSize, int log2ElementSize)
    {
        return arrayAllocationSize(length, headerSize, log2ElementSize, getConfig(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
    }

    /**
     * Computes the size of the memory chunk allocated for an array. This size accounts for the
     * array header size, body size and any padding after the last element to satisfy object
     * alignment requirements.
     *
     * @param length the number of elements in the array
     * @param headerSize the size of the array header
     * @param log2ElementSize log2 of the size of an element in the array
     * @param config the VM configuration providing the
     *            {@linkplain GraalHotSpotVMConfig#objectAlignment object alignment requirement}
     * @return the size of the memory chunk
     */
    public static int arrayAllocationSize(int length, int headerSize, int log2ElementSize, GraalHotSpotVMConfig config)
    {
        int alignment = config.objectAlignment;
        int size = (length << log2ElementSize) + headerSize + (alignment - 1);
        int mask = ~(alignment - 1);
        return size & mask;
    }

    @Fold
    public static int instanceHeaderSize(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.useCompressedClassPointers ? (2 * wordSize()) - 4 : 2 * wordSize();
    }

    @Fold
    public static byte dirtyCardValue(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.dirtyCardValue;
    }

    @Fold
    public static byte g1YoungCardValue(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.g1YoungCardValue;
    }

    @Fold
    public static int cardTableShift(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.cardtableShift;
    }

    @Fold
    public static long cardTableStart(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.cardtableStartAddress;
    }

    @Fold
    public static int g1CardQueueIndexOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.g1CardQueueIndexOffset;
    }

    @Fold
    public static int g1CardQueueBufferOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.g1CardQueueBufferOffset;
    }

    @Fold
    public static int logOfHeapRegionGrainBytes(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.logOfHRGrainBytes;
    }

    @Fold
    public static int g1SATBQueueMarkingOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.g1SATBQueueMarkingOffset;
    }

    @Fold
    public static int g1SATBQueueIndexOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.g1SATBQueueIndexOffset;
    }

    @Fold
    public static int g1SATBQueueBufferOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.g1SATBQueueBufferOffset;
    }

    public static final LocationIdentity KLASS_SUPER_CHECK_OFFSET_LOCATION = NamedLocationIdentity.immutable("Klass::_super_check_offset");

    @Fold
    public static int superCheckOffsetOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.superCheckOffsetOffset;
    }

    public static final LocationIdentity SECONDARY_SUPER_CACHE_LOCATION = NamedLocationIdentity.mutable("SecondarySuperCache");

    @Fold
    public static int secondarySuperCacheOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.secondarySuperCacheOffset;
    }

    public static final LocationIdentity SECONDARY_SUPERS_LOCATION = NamedLocationIdentity.immutable("SecondarySupers");

    @Fold
    public static int secondarySupersOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.secondarySupersOffset;
    }

    public static final LocationIdentity DISPLACED_MARK_WORD_LOCATION = NamedLocationIdentity.mutable("DisplacedMarkWord");

    public static final LocationIdentity OBJECT_MONITOR_OWNER_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_owner");

    public static final LocationIdentity OBJECT_MONITOR_RECURSION_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_recursions");

    public static final LocationIdentity OBJECT_MONITOR_CXQ_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_cxq");

    public static final LocationIdentity OBJECT_MONITOR_ENTRY_LIST_LOCATION = NamedLocationIdentity.mutable("ObjectMonitor::_EntryList");

    @Fold
    public static int lockDisplacedMarkOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.basicLockDisplacedHeaderOffset;
    }

    @Fold
    public static boolean useBiasedLocking(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.useBiasedLocking;
    }

    @Fold
    public static boolean useDeferredInitBarriers(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.useDeferredInitBarriers;
    }

    @Fold
    public static boolean useG1GC(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.useG1GC;
    }

    @Fold
    public static boolean useCMSIncrementalMode(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.cmsIncrementalMode;
    }

    @Fold
    public static boolean useCompressedOops(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.useCompressedOops;
    }

    @Fold
    static int uninitializedIdentityHashCodeValue(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.uninitializedIdentityHashCodeValue;
    }

    @Fold
    static int identityHashCodeShift(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.identityHashCodeShift;
    }

    /**
     * Loads the hub of an object (without null checking it first).
     */
    public static KlassPointer loadHub(Object object)
    {
        return loadHubIntrinsic(object);
    }

    public static Word loadWordFromObject(Object object, int offset)
    {
        return loadWordFromObjectIntrinsic(object, offset, LocationIdentity.any(), getWordKind());
    }

    public static Word loadWordFromObject(Object object, int offset, LocationIdentity identity)
    {
        return loadWordFromObjectIntrinsic(object, offset, identity, getWordKind());
    }

    public static KlassPointer loadKlassFromObject(Object object, int offset, LocationIdentity identity)
    {
        return loadKlassFromObjectIntrinsic(object, offset, identity, getWordKind());
    }

    /**
     * Reads the value of a given register.
     *
     * @param register a register which must not be available to the register allocator
     * @return the value of {@code register} as a word
     */
    public static Word registerAsWord(@ConstantNodeParameter Register register)
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

    @Fold
    public static int log2WordSize()
    {
        return CodeUtil.log2(wordSize());
    }

    public static final LocationIdentity CLASS_STATE_LOCATION = NamedLocationIdentity.mutable("ClassState");

    @Fold
    public static int instanceKlassInitStateOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.instanceKlassInitStateOffset;
    }

    @Fold
    public static int instanceKlassStateFullyInitialized(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.instanceKlassStateFullyInitialized;
    }

    /**
     *
     * @param hub the hub of an InstanceKlass
     * @return true is the InstanceKlass represented by hub is fully initialized
     */
    public static boolean isInstanceKlassFullyInitialized(KlassPointer hub)
    {
        return readInstanceKlassState(hub) == instanceKlassStateFullyInitialized(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
    }

    private static byte readInstanceKlassState(KlassPointer hub)
    {
        return hub.readByte(instanceKlassInitStateOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), CLASS_STATE_LOCATION);
    }

    public static final LocationIdentity KLASS_MODIFIER_FLAGS_LOCATION = NamedLocationIdentity.immutable("Klass::_modifier_flags");

    @Fold
    public static int klassModifierFlagsOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.klassModifierFlagsOffset;
    }

    public static final LocationIdentity CLASS_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("Class._klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            return foldIndirection(read, object, CLASS_MIRROR_LOCATION);
        }
    };

    @Fold
    public static int klassOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.klassOffset;
    }

    public static final LocationIdentity CLASS_ARRAY_KLASS_LOCATION = new HotSpotOptimizingLocationIdentity("Class._array_klass")
    {
        @Override
        public ValueNode canonicalizeRead(ValueNode read, AddressNode location, ValueNode object, CanonicalizerTool tool)
        {
            return foldIndirection(read, object, ARRAY_KLASS_COMPONENT_MIRROR);
        }
    };

    @Fold
    public static int arrayKlassOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.arrayKlassOffset;
    }

    public static final LocationIdentity CLASS_MIRROR_LOCATION = NamedLocationIdentity.immutable("Klass::_java_mirror");

    public static final LocationIdentity CLASS_MIRROR_HANDLE_LOCATION = NamedLocationIdentity.immutable("Klass::_java_mirror handle");

    public static final LocationIdentity HEAP_TOP_LOCATION = NamedLocationIdentity.mutable("HeapTop");

    @Fold
    public static long heapTopAddress(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.heapTopAddress;
    }

    public static final LocationIdentity HEAP_END_LOCATION = NamedLocationIdentity.mutable("HeapEnd");

    @Fold
    public static long heapEndAddress(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.heapEndAddress;
    }

    @Fold
    public static long tlabIntArrayMarkWord(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabIntArrayMarkWord();
    }

    @Fold
    public static boolean inlineContiguousAllocationSupported(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.inlineContiguousAllocationSupported;
    }

    @Fold
    public static int tlabAlignmentReserveInHeapWords(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabAlignmentReserve;
    }

    public static final LocationIdentity TLAB_SIZE_LOCATION = NamedLocationIdentity.mutable("TlabSize");

    @Fold
    public static int threadTlabSizeOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadTlabSizeOffset();
    }

    public static final LocationIdentity TLAB_THREAD_ALLOCATED_BYTES_LOCATION = NamedLocationIdentity.mutable("TlabThreadAllocatedBytes");

    @Fold
    public static int threadAllocatedBytesOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.threadAllocatedBytesOffset;
    }

    public static final LocationIdentity TLAB_REFILL_WASTE_LIMIT_LOCATION = NamedLocationIdentity.mutable("RefillWasteLimit");

    @Fold
    public static int tlabRefillWasteLimitOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabRefillWasteLimitOffset();
    }

    public static final LocationIdentity TLAB_NOF_REFILLS_LOCATION = NamedLocationIdentity.mutable("TlabNOfRefills");

    @Fold
    public static int tlabNumberOfRefillsOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabNumberOfRefillsOffset();
    }

    public static final LocationIdentity TLAB_FAST_REFILL_WASTE_LOCATION = NamedLocationIdentity.mutable("TlabFastRefillWaste");

    @Fold
    public static int tlabFastRefillWasteOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabFastRefillWasteOffset();
    }

    public static final LocationIdentity TLAB_SLOW_ALLOCATIONS_LOCATION = NamedLocationIdentity.mutable("TlabSlowAllocations");

    @Fold
    public static int tlabSlowAllocationsOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabSlowAllocationsOffset();
    }

    @Fold
    public static int tlabRefillWasteIncrement(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabRefillWasteIncrement;
    }

    @Fold
    public static boolean tlabStats(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.tlabStats;
    }

    @Fold
    public static int layoutHelperHeaderSizeShift(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.layoutHelperHeaderSizeShift;
    }

    @Fold
    public static int layoutHelperHeaderSizeMask(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.layoutHelperHeaderSizeMask;
    }

    @Fold
    public static int layoutHelperLog2ElementSizeShift(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.layoutHelperLog2ElementSizeShift;
    }

    @Fold
    public static int layoutHelperLog2ElementSizeMask(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.layoutHelperLog2ElementSizeMask;
    }

    @Fold
    public static int layoutHelperElementTypeShift(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.layoutHelperElementTypeShift;
    }

    @Fold
    public static int layoutHelperElementTypeMask(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.layoutHelperElementTypeMask;
    }

    @Fold
    public static int layoutHelperElementTypePrimitiveInPlace(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.layoutHelperElementTypePrimitiveInPlace();
    }

    @NodeIntrinsic(ForeignCallNode.class)
    public static native int identityHashCode(@ConstantNodeParameter ForeignCallDescriptor descriptor, Object object);

    @Fold
    public static int verifiedEntryPointOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.nmethodEntryOffset;
    }

    @Fold
    public static long referentOffset()
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

    @Fold
    public static int arrayClassElementOffset(@InjectedParameter GraalHotSpotVMConfig config)
    {
        return config.arrayClassElementOffset;
    }

    public static final LocationIdentity PRIMARY_SUPERS_LOCATION = NamedLocationIdentity.immutable("PrimarySupers");

    public static final LocationIdentity METASPACE_ARRAY_LENGTH_LOCATION = NamedLocationIdentity.immutable("MetaspaceArrayLength");

    public static final LocationIdentity SECONDARY_SUPERS_ELEMENT_LOCATION = NamedLocationIdentity.immutable("SecondarySupersElement");
}