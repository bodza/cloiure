package giraaff.replacements;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.MemoryBarriers;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.CompressionNode.CompressionOp;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FieldLocationIdentity;
import giraaff.nodes.FixedNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.PiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IntegerBelowNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.calc.IntegerEqualsNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.calc.NarrowNode;
import giraaff.nodes.calc.RightShiftNode;
import giraaff.nodes.calc.SignExtendNode;
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.calc.UnpackEndianHalfNode;
import giraaff.nodes.calc.ZeroExtendNode;
import giraaff.nodes.extended.BoxNode;
import giraaff.nodes.extended.FixedValueAnchorNode;
import giraaff.nodes.extended.GuardedUnsafeLoadNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.extended.JavaReadNode;
import giraaff.nodes.extended.JavaWriteNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.extended.MembarNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.extended.RawStoreNode;
import giraaff.nodes.extended.UnboxNode;
import giraaff.nodes.extended.UnsafeMemoryLoadNode;
import giraaff.nodes.extended.UnsafeMemoryStoreNode;
import giraaff.nodes.java.AbstractNewObjectNode;
import giraaff.nodes.java.AccessIndexedNode;
import giraaff.nodes.java.ArrayLengthNode;
import giraaff.nodes.java.AtomicReadAndWriteNode;
import giraaff.nodes.java.FinalFieldBarrierNode;
import giraaff.nodes.java.InstanceOfDynamicNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.nodes.java.LoadIndexedNode;
import giraaff.nodes.java.LogicCompareAndSwapNode;
import giraaff.nodes.java.LoweredAtomicReadAndWriteNode;
import giraaff.nodes.java.MonitorEnterNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.java.NewArrayNode;
import giraaff.nodes.java.NewInstanceNode;
import giraaff.nodes.java.RawMonitorEnterNode;
import giraaff.nodes.java.StoreFieldNode;
import giraaff.nodes.java.StoreIndexedNode;
import giraaff.nodes.java.UnsafeCompareAndSwapNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.WriteNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.AllocatedObjectNode;
import giraaff.nodes.virtual.CommitAllocationNode;
import giraaff.nodes.virtual.VirtualArrayNode;
import giraaff.nodes.virtual.VirtualInstanceNode;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.phases.util.Providers;
import giraaff.replacements.SnippetLowerableMemoryNode.SnippetLowering;
import giraaff.util.GraalError;

///
// VM-independent lowerings for standard Java nodes.
// VM-specific methods are abstract and must be implemented by VM-specific subclasses.
///
// @class DefaultJavaLoweringProvider
public abstract class DefaultJavaLoweringProvider implements LoweringProvider
{
    // @field
    protected final MetaAccessProvider ___metaAccess;
    // @field
    protected final ForeignCallsProvider ___foreignCalls;
    // @field
    protected final TargetDescription ___target;
    // @field
    private final boolean ___useCompressedOops;

    // @field
    private BoxingSnippets.Templates ___boxingSnippets;

    // @cons
    public DefaultJavaLoweringProvider(MetaAccessProvider __metaAccess, ForeignCallsProvider __foreignCalls, TargetDescription __target, boolean __useCompressedOops)
    {
        super();
        this.___metaAccess = __metaAccess;
        this.___foreignCalls = __foreignCalls;
        this.___target = __target;
        this.___useCompressedOops = __useCompressedOops;
    }

    public void initialize(Providers __providers, SnippetReflectionProvider __snippetReflection)
    {
        this.___boxingSnippets = new BoxingSnippets.Templates(__providers, __snippetReflection, this.___target);
    }

    public final TargetDescription getTarget()
    {
        return this.___target;
    }

    @Override
    public void lower(Node __n, LoweringTool __tool)
    {
        StructuredGraph __graph = (StructuredGraph) __n.graph();
        if (__n instanceof LoadFieldNode)
        {
            lowerLoadFieldNode((LoadFieldNode) __n, __tool);
        }
        else if (__n instanceof StoreFieldNode)
        {
            lowerStoreFieldNode((StoreFieldNode) __n, __tool);
        }
        else if (__n instanceof LoadIndexedNode)
        {
            lowerLoadIndexedNode((LoadIndexedNode) __n, __tool);
        }
        else if (__n instanceof StoreIndexedNode)
        {
            lowerStoreIndexedNode((StoreIndexedNode) __n, __tool);
        }
        else if (__n instanceof ArrayLengthNode)
        {
            lowerArrayLengthNode((ArrayLengthNode) __n, __tool);
        }
        else if (__n instanceof LoadHubNode)
        {
            lowerLoadHubNode((LoadHubNode) __n, __tool);
        }
        else if (__n instanceof MonitorEnterNode)
        {
            lowerMonitorEnterNode((MonitorEnterNode) __n, __tool, __graph);
        }
        else if (__n instanceof UnsafeCompareAndSwapNode)
        {
            lowerCompareAndSwapNode((UnsafeCompareAndSwapNode) __n);
        }
        else if (__n instanceof AtomicReadAndWriteNode)
        {
            lowerAtomicReadAndWriteNode((AtomicReadAndWriteNode) __n);
        }
        else if (__n instanceof RawLoadNode)
        {
            lowerUnsafeLoadNode((RawLoadNode) __n, __tool);
        }
        else if (__n instanceof UnsafeMemoryLoadNode)
        {
            lowerUnsafeMemoryLoadNode((UnsafeMemoryLoadNode) __n);
        }
        else if (__n instanceof RawStoreNode)
        {
            lowerUnsafeStoreNode((RawStoreNode) __n);
        }
        else if (__n instanceof UnsafeMemoryStoreNode)
        {
            lowerUnsafeMemoryStoreNode((UnsafeMemoryStoreNode) __n);
        }
        else if (__n instanceof JavaReadNode)
        {
            lowerJavaReadNode((JavaReadNode) __n);
        }
        else if (__n instanceof JavaWriteNode)
        {
            lowerJavaWriteNode((JavaWriteNode) __n);
        }
        else if (__n instanceof CommitAllocationNode)
        {
            lowerCommitAllocationNode((CommitAllocationNode) __n, __tool);
        }
        else if (__n instanceof BoxNode)
        {
            this.___boxingSnippets.lower((BoxNode) __n, __tool);
        }
        else if (__n instanceof UnboxNode)
        {
            this.___boxingSnippets.lower((UnboxNode) __n, __tool);
        }
        else if (__n instanceof UnpackEndianHalfNode)
        {
            lowerSecondHalf((UnpackEndianHalfNode) __n);
        }
        else
        {
            throw GraalError.shouldNotReachHere("Node implementing Lowerable not handled: " + __n);
        }
    }

    private void lowerSecondHalf(UnpackEndianHalfNode __n)
    {
        ByteOrder __byteOrder = this.___target.arch.getByteOrder();
        __n.lower(__byteOrder);
    }

    protected AddressNode createOffsetAddress(StructuredGraph __graph, ValueNode __object, long __offset)
    {
        ValueNode __o = ConstantNode.forIntegerKind(this.___target.wordJavaKind, __offset, __graph);
        return __graph.unique(new OffsetAddressNode(__object, __o));
    }

    protected AddressNode createFieldAddress(StructuredGraph __graph, ValueNode __object, ResolvedJavaField __field)
    {
        int __offset = fieldOffset(__field);
        if (__offset >= 0)
        {
            return createOffsetAddress(__graph, __object, __offset);
        }
        else
        {
            return null;
        }
    }

    protected abstract JavaKind getStorageKind(ResolvedJavaField __field);

    protected void lowerLoadFieldNode(LoadFieldNode __loadField, LoweringTool __tool)
    {
        StructuredGraph __graph = __loadField.graph();
        ResolvedJavaField __field = __loadField.field();
        ValueNode __object = __loadField.isStatic() ? staticFieldBase(__graph, __field) : __loadField.object();
        __object = createNullCheckedValue(__object, __loadField, __tool);
        Stamp __loadStamp = loadStamp(__loadField.stamp(NodeView.DEFAULT), getStorageKind(__field));

        AddressNode __address = createFieldAddress(__graph, __object, __field);

        ReadNode __memoryRead = __graph.add(new ReadNode(__address, fieldLocationIdentity(__field), __loadStamp, fieldLoadBarrierType(__field)));
        ValueNode __readValue = implicitLoadConvert(__graph, getStorageKind(__field), __memoryRead);
        __loadField.replaceAtUsages(__readValue);
        __graph.replaceFixed(__loadField, __memoryRead);

        if (__loadField.isVolatile())
        {
            MembarNode __preMembar = __graph.add(new MembarNode(MemoryBarriers.JMM_PRE_VOLATILE_READ));
            __graph.addBeforeFixed(__memoryRead, __preMembar);
            MembarNode __postMembar = __graph.add(new MembarNode(MemoryBarriers.JMM_POST_VOLATILE_READ));
            __graph.addAfterFixed(__memoryRead, __postMembar);
        }
    }

    protected void lowerStoreFieldNode(StoreFieldNode __storeField, LoweringTool __tool)
    {
        StructuredGraph __graph = __storeField.graph();
        ResolvedJavaField __field = __storeField.field();
        ValueNode __object = __storeField.isStatic() ? staticFieldBase(__graph, __field) : __storeField.object();
        __object = createNullCheckedValue(__object, __storeField, __tool);
        ValueNode __value = implicitStoreConvert(__graph, getStorageKind(__storeField.field()), __storeField.value());
        AddressNode __address = createFieldAddress(__graph, __object, __field);

        WriteNode __memoryWrite = __graph.add(new WriteNode(__address, fieldLocationIdentity(__field), __value, fieldStoreBarrierType(__storeField.field())));
        __memoryWrite.setStateAfter(__storeField.stateAfter());
        __graph.replaceFixedWithFixed(__storeField, __memoryWrite);

        if (__storeField.isVolatile())
        {
            MembarNode __preMembar = __graph.add(new MembarNode(MemoryBarriers.JMM_PRE_VOLATILE_WRITE));
            __graph.addBeforeFixed(__memoryWrite, __preMembar);
            MembarNode __postMembar = __graph.add(new MembarNode(MemoryBarriers.JMM_POST_VOLATILE_WRITE));
            __graph.addAfterFixed(__memoryWrite, __postMembar);
        }
    }

    ///
    // Create a PiNode on the index proving that the index is positive. On some platforms this is
    // important to allow the index to be used as an int in the address mode.
    ///
    public AddressNode createArrayIndexAddress(StructuredGraph __graph, ValueNode __array, JavaKind __elementKind, ValueNode __index, GuardingNode __boundsCheck)
    {
        IntegerStamp __indexStamp = StampFactory.forInteger(32, 0, Integer.MAX_VALUE - 1);
        ValueNode __positiveIndex = __graph.maybeAddOrUnique(PiNode.create(__index, __indexStamp, __boundsCheck != null ? __boundsCheck.asNode() : null));
        return createArrayAddress(__graph, __array, __elementKind, __positiveIndex);
    }

    public AddressNode createArrayAddress(StructuredGraph __graph, ValueNode __array, JavaKind __elementKind, ValueNode __index)
    {
        ValueNode __wordIndex;
        if (this.___target.wordSize > 4)
        {
            __wordIndex = __graph.unique(new SignExtendNode(__index, this.___target.wordSize * 8));
        }
        else
        {
            __wordIndex = __index;
        }

        int __shift = CodeUtil.log2(arrayScalingFactor(__elementKind));
        ValueNode __scaledIndex = __graph.unique(new LeftShiftNode(__wordIndex, ConstantNode.forInt(__shift, __graph)));

        int __base = arrayBaseOffset(__elementKind);
        ValueNode __offset = __graph.unique(new AddNode(__scaledIndex, ConstantNode.forIntegerKind(this.___target.wordJavaKind, __base, __graph)));

        return __graph.unique(new OffsetAddressNode(__array, __offset));
    }

    protected void lowerLoadIndexedNode(LoadIndexedNode __loadIndexed, LoweringTool __tool)
    {
        StructuredGraph __graph = __loadIndexed.graph();
        ValueNode __array = __loadIndexed.array();
        __array = createNullCheckedValue(__array, __loadIndexed, __tool);
        JavaKind __elementKind = __loadIndexed.elementKind();
        Stamp __loadStamp = loadStamp(__loadIndexed.stamp(NodeView.DEFAULT), __elementKind);

        GuardingNode __boundsCheck = getBoundsCheck(__loadIndexed, __array, __tool);
        AddressNode __address = createArrayIndexAddress(__graph, __array, __elementKind, __loadIndexed.index(), __boundsCheck);
        ReadNode __memoryRead = __graph.add(new ReadNode(__address, NamedLocationIdentity.getArrayLocation(__elementKind), __loadStamp, BarrierType.NONE));
        __memoryRead.setGuard(__boundsCheck);
        ValueNode __readValue = implicitLoadConvert(__graph, __elementKind, __memoryRead);

        __loadIndexed.replaceAtUsages(__readValue);
        __graph.replaceFixed(__loadIndexed, __memoryRead);
    }

    protected void lowerStoreIndexedNode(StoreIndexedNode __storeIndexed, LoweringTool __tool)
    {
        StructuredGraph __graph = __storeIndexed.graph();

        ValueNode __value = __storeIndexed.value();
        ValueNode __array = __storeIndexed.array();

        __array = this.createNullCheckedValue(__array, __storeIndexed, __tool);

        GuardingNode __boundsCheck = getBoundsCheck(__storeIndexed, __array, __tool);

        JavaKind __elementKind = __storeIndexed.elementKind();

        LogicNode __condition = null;
        if (__elementKind == JavaKind.Object && !StampTool.isPointerAlwaysNull(__value))
        {
            // Array store check.
            TypeReference __arrayType = StampTool.typeReferenceOrNull(__array);
            if (__arrayType != null && __arrayType.isExact())
            {
                ResolvedJavaType __elementType = __arrayType.getType().getComponentType();
                if (!__elementType.isJavaLangObject())
                {
                    TypeReference __typeReference = TypeReference.createTrusted(__storeIndexed.graph().getAssumptions(), __elementType);
                    LogicNode __typeTest = __graph.addOrUniqueWithInputs(InstanceOfNode.create(__typeReference, __value));
                    __condition = LogicNode.or(__graph.unique(IsNullNode.create(__value)), __typeTest, GraalDirectives.UNLIKELY_PROBABILITY);
                }
            }
            else
            {
                // The guard on the read hub should be the null check of the array that was introduced earlier.
                ValueNode __arrayClass = createReadHub(__graph, __array, __tool);
                ValueNode __componentHub = createReadArrayComponentHub(__graph, __arrayClass, __storeIndexed);
                LogicNode __typeTest = __graph.unique(InstanceOfDynamicNode.create(__graph.getAssumptions(), __tool.getConstantReflection(), __componentHub, __value, false));
                __condition = LogicNode.or(__graph.unique(IsNullNode.create(__value)), __typeTest, GraalDirectives.UNLIKELY_PROBABILITY);
            }
        }

        AddressNode __address = createArrayIndexAddress(__graph, __array, __elementKind, __storeIndexed.index(), __boundsCheck);
        WriteNode __memoryWrite = __graph.add(new WriteNode(__address, NamedLocationIdentity.getArrayLocation(__elementKind), implicitStoreConvert(__graph, __elementKind, __value), arrayStoreBarrierType(__storeIndexed.elementKind())));
        __memoryWrite.setGuard(__boundsCheck);
        if (__condition != null)
        {
            __tool.createGuard(__storeIndexed, __condition, DeoptimizationReason.ArrayStoreException, DeoptimizationAction.InvalidateReprofile);
        }
        __memoryWrite.setStateAfter(__storeIndexed.stateAfter());
        __graph.replaceFixedWithFixed(__storeIndexed, __memoryWrite);
    }

    protected void lowerArrayLengthNode(ArrayLengthNode __arrayLengthNode, LoweringTool __tool)
    {
        __arrayLengthNode.replaceAtUsages(createReadArrayLength(__arrayLengthNode.array(), __arrayLengthNode, __tool));
        StructuredGraph __graph = __arrayLengthNode.graph();
        __graph.removeFixed(__arrayLengthNode);
    }

    ///
    // Creates a read node that read the array length and is guarded by a null-check.
    //
    // The created node is placed before {@code before} in the CFG.
    ///
    protected ReadNode createReadArrayLength(ValueNode __array, FixedNode __before, LoweringTool __tool)
    {
        StructuredGraph __graph = __array.graph();
        ValueNode __canonicalArray = this.createNullCheckedValue(GraphUtil.skipPiWhileNonNull(__array), __before, __tool);
        AddressNode __address = createOffsetAddress(__graph, __canonicalArray, HotSpotRuntime.arrayLengthOffset);
        ReadNode __readArrayLength = __graph.add(new ReadNode(__address, NamedLocationIdentity.ARRAY_LENGTH_LOCATION, StampFactory.positiveInt(), BarrierType.NONE));
        __graph.addBeforeFixed(__before, __readArrayLength);
        return __readArrayLength;
    }

    protected void lowerLoadHubNode(LoadHubNode __loadHub, LoweringTool __tool)
    {
        StructuredGraph __graph = __loadHub.graph();
        if (__tool.getLoweringStage() != LoweringTool.StandardLoweringStage.LOW_TIER)
        {
            return;
        }
        if (__graph.getGuardsStage().allowsFloatingGuards())
        {
            return;
        }
        ValueNode __hub = createReadHub(__graph, __loadHub.getValue(), __tool);
        __loadHub.replaceAtUsagesAndDelete(__hub);
    }

    protected void lowerMonitorEnterNode(MonitorEnterNode __monitorEnter, LoweringTool __tool, StructuredGraph __graph)
    {
        ValueNode __object = createNullCheckedValue(__monitorEnter.object(), __monitorEnter, __tool);
        ValueNode __hub = __graph.addOrUnique(LoadHubNode.create(__object, __tool.getStampProvider(), __tool.getMetaAccess(), __tool.getConstantReflection()));
        RawMonitorEnterNode __rawMonitorEnter = __graph.add(new RawMonitorEnterNode(__object, __hub, __monitorEnter.getMonitorId()));
        __rawMonitorEnter.setStateBefore(__monitorEnter.stateBefore());
        __rawMonitorEnter.setStateAfter(__monitorEnter.stateAfter());
        __graph.replaceFixedWithFixed(__monitorEnter, __rawMonitorEnter);
    }

    protected void lowerCompareAndSwapNode(UnsafeCompareAndSwapNode __cas)
    {
        StructuredGraph __graph = __cas.graph();
        JavaKind __valueKind = __cas.getValueKind();

        ValueNode __expectedValue = implicitStoreConvert(__graph, __valueKind, __cas.expected());
        ValueNode __newValue = implicitStoreConvert(__graph, __valueKind, __cas.newValue());

        AddressNode __address = __graph.unique(new OffsetAddressNode(__cas.object(), __cas.offset()));
        BarrierType __barrierType = storeBarrierType(__cas.object(), __expectedValue);
        LogicCompareAndSwapNode __atomicNode = __graph.add(new LogicCompareAndSwapNode(__address, __cas.getLocationIdentity(), __expectedValue, __newValue, __barrierType));
        __atomicNode.setStateAfter(__cas.stateAfter());
        __graph.replaceFixedWithFixed(__cas, __atomicNode);
    }

    protected void lowerAtomicReadAndWriteNode(AtomicReadAndWriteNode __n)
    {
        StructuredGraph __graph = __n.graph();
        JavaKind __valueKind = __n.getValueKind();

        ValueNode __newValue = implicitStoreConvert(__graph, __valueKind, __n.newValue());

        AddressNode __address = __graph.unique(new OffsetAddressNode(__n.object(), __n.offset()));
        BarrierType __barrierType = storeBarrierType(__n.object(), __n.newValue());
        LoweredAtomicReadAndWriteNode __memoryRead = __graph.add(new LoweredAtomicReadAndWriteNode(__address, __n.getLocationIdentity(), __newValue, __barrierType));
        __memoryRead.setStateAfter(__n.stateAfter());

        ValueNode __readValue = implicitLoadConvert(__graph, __valueKind, __memoryRead);
        __n.stateAfter().replaceFirstInput(__n, __memoryRead);
        __n.replaceAtUsages(__readValue);
        __graph.replaceFixedWithFixed(__n, __memoryRead);
    }

    ///
    // @param tool utility for performing the lowering
    ///
    protected void lowerUnsafeLoadNode(RawLoadNode __load, LoweringTool __tool)
    {
        StructuredGraph __graph = __load.graph();
        if (__load instanceof GuardedUnsafeLoadNode)
        {
            GuardedUnsafeLoadNode __guardedLoad = (GuardedUnsafeLoadNode) __load;
            GuardingNode __guard = __guardedLoad.getGuard();
            if (__guard == null)
            {
                // can float freely if the guard folded away
                ReadNode __memoryRead = createUnsafeRead(__graph, __load, null);
                __memoryRead.setForceFixed(false);
                __graph.replaceFixedWithFixed(__load, __memoryRead);
            }
            else
            {
                // must be guarded, but flows below the guard
                ReadNode __memoryRead = createUnsafeRead(__graph, __load, __guard);
                __graph.replaceFixedWithFixed(__load, __memoryRead);
            }
        }
        else
        {
            // never had a guarding condition so it must be fixed, creation of the read will force
            // it to be fixed
            ReadNode __memoryRead = createUnsafeRead(__graph, __load, null);
            __graph.replaceFixedWithFixed(__load, __memoryRead);
        }
    }

    protected AddressNode createUnsafeAddress(StructuredGraph __graph, ValueNode __object, ValueNode __offset)
    {
        if (__object.isConstant() && __object.asConstant().isDefaultForKind())
        {
            return __graph.addOrUniqueWithInputs(OffsetAddressNode.create(__offset));
        }
        else
        {
            return __graph.unique(new OffsetAddressNode(__object, __offset));
        }
    }

    protected ReadNode createUnsafeRead(StructuredGraph __graph, RawLoadNode __load, GuardingNode __guard)
    {
        boolean __compressible = __load.accessKind() == JavaKind.Object;
        JavaKind __readKind = __load.accessKind();
        Stamp __loadStamp = loadStamp(__load.stamp(NodeView.DEFAULT), __readKind, __compressible);
        AddressNode __address = createUnsafeAddress(__graph, __load.object(), __load.offset());
        ReadNode __memoryRead = __graph.add(new ReadNode(__address, __load.getLocationIdentity(), __loadStamp, BarrierType.NONE));
        if (__guard == null)
        {
            // An unsafe read must not float, otherwise it may float above a test guaranteeing the read is safe.
            __memoryRead.setForceFixed(true);
        }
        else
        {
            __memoryRead.setGuard(__guard);
        }
        ValueNode __readValue = performBooleanCoercionIfNecessary(implicitLoadConvert(__graph, __readKind, __memoryRead, __compressible), __readKind);
        __load.replaceAtUsages(__readValue);
        return __memoryRead;
    }

    protected void lowerUnsafeMemoryLoadNode(UnsafeMemoryLoadNode __load)
    {
        StructuredGraph __graph = __load.graph();
        JavaKind __readKind = __load.getKind();
        Stamp __loadStamp = loadStamp(__load.stamp(NodeView.DEFAULT), __readKind, false);
        AddressNode __address = __graph.addOrUniqueWithInputs(OffsetAddressNode.create(__load.getAddress()));
        ReadNode __memoryRead = __graph.add(new ReadNode(__address, __load.getLocationIdentity(), __loadStamp, BarrierType.NONE));
        // An unsafe read must not float, otherwise it may float above a test guaranteeing the read is safe.
        __memoryRead.setForceFixed(true);
        ValueNode __readValue = performBooleanCoercionIfNecessary(implicitLoadConvert(__graph, __readKind, __memoryRead, false), __readKind);
        __load.replaceAtUsages(__readValue);
        __graph.replaceFixedWithFixed(__load, __memoryRead);
    }

    private static ValueNode performBooleanCoercionIfNecessary(ValueNode __readValue, JavaKind __readKind)
    {
        if (__readKind == JavaKind.Boolean)
        {
            StructuredGraph __graph = __readValue.graph();
            IntegerEqualsNode __eq = __graph.addOrUnique(new IntegerEqualsNode(__readValue, ConstantNode.forInt(0, __graph)));
            return __graph.addOrUnique(new ConditionalNode(__eq, ConstantNode.forBoolean(false, __graph), ConstantNode.forBoolean(true, __graph)));
        }
        return __readValue;
    }

    protected void lowerUnsafeStoreNode(RawStoreNode __store)
    {
        StructuredGraph __graph = __store.graph();
        boolean __compressible = __store.value().getStackKind() == JavaKind.Object;
        JavaKind __valueKind = __store.accessKind();
        ValueNode __value = implicitStoreConvert(__graph, __valueKind, __store.value(), __compressible);
        AddressNode __address = createUnsafeAddress(__graph, __store.object(), __store.offset());
        WriteNode __write = __graph.add(new WriteNode(__address, __store.getLocationIdentity(), __value, unsafeStoreBarrierType(__store)));
        __write.setStateAfter(__store.stateAfter());
        __graph.replaceFixedWithFixed(__store, __write);
    }

    protected void lowerUnsafeMemoryStoreNode(UnsafeMemoryStoreNode __store)
    {
        StructuredGraph __graph = __store.graph();
        JavaKind __valueKind = __store.getKind();
        ValueNode __value = implicitStoreConvert(__graph, __valueKind, __store.getValue(), false);
        AddressNode __address = __graph.addOrUniqueWithInputs(OffsetAddressNode.create(__store.getAddress()));
        WriteNode __write = __graph.add(new WriteNode(__address, __store.getLocationIdentity(), __value, BarrierType.NONE));
        __write.setStateAfter(__store.stateAfter());
        __graph.replaceFixedWithFixed(__store, __write);
    }

    protected void lowerJavaReadNode(JavaReadNode __read)
    {
        StructuredGraph __graph = __read.graph();
        JavaKind __valueKind = __read.getReadKind();
        Stamp __loadStamp = loadStamp(__read.stamp(NodeView.DEFAULT), __valueKind, __read.isCompressible());

        ReadNode __memoryRead = __graph.add(new ReadNode(__read.getAddress(), __read.getLocationIdentity(), __loadStamp, __read.getBarrierType()));
        GuardingNode __guard = __read.getGuard();
        ValueNode __readValue = implicitLoadConvert(__graph, __valueKind, __memoryRead, __read.isCompressible());
        if (__guard == null)
        {
            // An unsafe read must not float, otherwise it may float above a test guaranteeing the read is safe.
            __memoryRead.setForceFixed(true);
        }
        else
        {
            __memoryRead.setGuard(__guard);
        }
        __read.replaceAtUsages(__readValue);
        __graph.replaceFixed(__read, __memoryRead);
    }

    protected void lowerJavaWriteNode(JavaWriteNode __write)
    {
        StructuredGraph __graph = __write.graph();
        ValueNode __value = implicitStoreConvert(__graph, __write.getWriteKind(), __write.value(), __write.isCompressible());
        WriteNode __memoryWrite = __graph.add(new WriteNode(__write.getAddress(), __write.getLocationIdentity(), __value, __write.getBarrierType()));
        __memoryWrite.setStateAfter(__write.stateAfter());
        __graph.replaceFixedWithFixed(__write, __memoryWrite);
        __memoryWrite.setGuard(__write.getGuard());
    }

    protected void lowerCommitAllocationNode(CommitAllocationNode __commit, LoweringTool __tool)
    {
        StructuredGraph __graph = __commit.graph();
        if (__graph.getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            List<AbstractNewObjectNode> __recursiveLowerings = new ArrayList<>();

            ValueNode[] __allocations = new ValueNode[__commit.getVirtualObjects().size()];
            BitSet __omittedValues = new BitSet();
            int __valuePos = 0;
            for (int __objIndex = 0; __objIndex < __commit.getVirtualObjects().size(); __objIndex++)
            {
                VirtualObjectNode __virtual = __commit.getVirtualObjects().get(__objIndex);
                int __entryCount = __virtual.entryCount();
                AbstractNewObjectNode __newObject;
                if (__virtual instanceof VirtualInstanceNode)
                {
                    __newObject = __graph.add(createNewInstanceFromVirtual(__virtual));
                }
                else
                {
                    __newObject = __graph.add(createNewArrayFromVirtual(__virtual, ConstantNode.forInt(__entryCount, __graph)));
                }

                __recursiveLowerings.add(__newObject);
                __graph.addBeforeFixed(__commit, __newObject);
                __allocations[__objIndex] = __newObject;
                for (int __i = 0; __i < __entryCount; __i++)
                {
                    ValueNode __value = __commit.getValues().get(__valuePos);
                    if (__value instanceof VirtualObjectNode)
                    {
                        __value = __allocations[__commit.getVirtualObjects().indexOf(__value)];
                    }
                    if (__value == null)
                    {
                        __omittedValues.set(__valuePos);
                    }
                    else if (!(__value.isConstant() && __value.asConstant().isDefaultForKind()))
                    {
                        // Constant.illegal is always the defaultForKind, so it is skipped
                        JavaKind __valueKind = __value.getStackKind();
                        JavaKind __entryKind = __virtual.entryKind(__i);

                        AddressNode __address = null;
                        BarrierType __barrierType = null;
                        if (__virtual instanceof VirtualInstanceNode)
                        {
                            ResolvedJavaField __field = ((VirtualInstanceNode) __virtual).field(__i);
                            long __offset = fieldOffset(__field);
                            if (__offset >= 0)
                            {
                                __address = createOffsetAddress(__graph, __newObject, __offset);
                                __barrierType = fieldInitializationBarrier(__entryKind);
                            }
                        }
                        else
                        {
                            __address = createOffsetAddress(__graph, __newObject, arrayBaseOffset(__entryKind) + __i * arrayScalingFactor(__entryKind));
                            __barrierType = arrayInitializationBarrier(__entryKind);
                        }
                        if (__address != null)
                        {
                            WriteNode __write = new WriteNode(__address, LocationIdentity.init(), implicitStoreConvert(__graph, __entryKind, __value), __barrierType);
                            __graph.addAfterFixed(__newObject, __graph.add(__write));
                        }
                    }
                    __valuePos++;
                }
            }
            __valuePos = 0;

            for (int __objIndex = 0; __objIndex < __commit.getVirtualObjects().size(); __objIndex++)
            {
                VirtualObjectNode __virtual = __commit.getVirtualObjects().get(__objIndex);
                int __entryCount = __virtual.entryCount();
                ValueNode __newObject = __allocations[__objIndex];
                for (int __i = 0; __i < __entryCount; __i++)
                {
                    if (__omittedValues.get(__valuePos))
                    {
                        ValueNode __value = __commit.getValues().get(__valuePos);
                        ValueNode __allocValue = __allocations[__commit.getVirtualObjects().indexOf(__value)];
                        if (!(__allocValue.isConstant() && __allocValue.asConstant().isDefaultForKind()))
                        {
                            AddressNode __address;
                            BarrierType __barrierType;
                            if (__virtual instanceof VirtualInstanceNode)
                            {
                                VirtualInstanceNode __virtualInstance = (VirtualInstanceNode) __virtual;
                                __address = createFieldAddress(__graph, __newObject, __virtualInstance.field(__i));
                                __barrierType = BarrierType.IMPRECISE;
                            }
                            else
                            {
                                __address = createArrayAddress(__graph, __newObject, __virtual.entryKind(__i), ConstantNode.forInt(__i, __graph));
                                __barrierType = BarrierType.PRECISE;
                            }
                            if (__address != null)
                            {
                                WriteNode __write = new WriteNode(__address, LocationIdentity.init(), implicitStoreConvert(__graph, JavaKind.Object, __allocValue), __barrierType);
                                __graph.addBeforeFixed(__commit, __graph.add(__write));
                            }
                        }
                    }
                    __valuePos++;
                }
            }

            finishAllocatedObjects(__tool, __commit, __allocations);
            __graph.removeFixed(__commit);

            for (AbstractNewObjectNode __recursiveLowering : __recursiveLowerings)
            {
                __recursiveLowering.lower(__tool);
            }
        }
    }

    public NewInstanceNode createNewInstanceFromVirtual(VirtualObjectNode __virtual)
    {
        return new NewInstanceNode(__virtual.type(), true);
    }

    protected NewArrayNode createNewArrayFromVirtual(VirtualObjectNode __virtual, ValueNode __length)
    {
        return new NewArrayNode(((VirtualArrayNode) __virtual).componentType(), __length, true);
    }

    public void finishAllocatedObjects(LoweringTool __tool, CommitAllocationNode __commit, ValueNode[] __allocations)
    {
        StructuredGraph __graph = __commit.graph();
        for (int __objIndex = 0; __objIndex < __commit.getVirtualObjects().size(); __objIndex++)
        {
            FixedValueAnchorNode __anchor = __graph.add(new FixedValueAnchorNode(__allocations[__objIndex]));
            __allocations[__objIndex] = __anchor;
            __graph.addBeforeFixed(__commit, __anchor);
        }
        // Note that the FrameState that is assigned to these MonitorEnterNodes isn't the correct state.
        // It will be the state from before the allocation occurred instead of a valid state after the
        // locking is performed. In practice this should be fine since these are newly allocated objects.
        // The bytecodes themselves permit allocating an object, doing a monitorenter and then dropping
        // all references to the object which would produce the same state, though that would normally
        // produce an IllegalMonitorStateException. In HotSpot some form of fast path locking should
        // always occur so the FrameState should never actually be used.
        ArrayList<MonitorEnterNode> __enters = null;
        for (int __objIndex = 0; __objIndex < __commit.getVirtualObjects().size(); __objIndex++)
        {
            List<MonitorIdNode> __locks = __commit.getLocks(__objIndex);
            if (__locks.size() > 1)
            {
                // ensure that the lock operations are performed in lock depth order
                ArrayList<MonitorIdNode> __newList = new ArrayList<>(__locks);
                __newList.sort((__a, __b) -> Integer.compare(__a.getLockDepth(), __b.getLockDepth()));
                __locks = __newList;
            }
            int __lastDepth = -1;
            for (MonitorIdNode __monitorId : __locks)
            {
                __lastDepth = __monitorId.getLockDepth();
                MonitorEnterNode __enter = __graph.add(new MonitorEnterNode(__allocations[__objIndex], __monitorId));
                __graph.addBeforeFixed(__commit, __enter);
                if (__enters == null)
                {
                    __enters = new ArrayList<>();
                }
                __enters.add(__enter);
            }
        }
        for (Node __usage : __commit.usages().snapshot())
        {
            if (__usage instanceof AllocatedObjectNode)
            {
                AllocatedObjectNode __addObject = (AllocatedObjectNode) __usage;
                int __index = __commit.getVirtualObjects().indexOf(__addObject.getVirtualObject());
                __addObject.replaceAtUsagesAndDelete(__allocations[__index]);
            }
            else
            {
                __commit.replaceAtUsages(InputType.Memory, __enters.get(__enters.size() - 1));
            }
        }
        if (__enters != null)
        {
            for (MonitorEnterNode __enter : __enters)
            {
                __enter.lower(__tool);
            }
        }
        insertAllocationBarrier(__commit, __graph);
    }

    ///
    // Insert the required {@link MemoryBarriers#STORE_STORE} barrier for an allocation and also
    // include the {@link MemoryBarriers#LOAD_STORE} required for final fields if any final fields
    // are being written, as if {@link FinalFieldBarrierNode} were emitted.
    ///
    private static void insertAllocationBarrier(CommitAllocationNode __commit, StructuredGraph __graph)
    {
        int __barrier = MemoryBarriers.STORE_STORE;
        outer: for (VirtualObjectNode vobj : __commit.getVirtualObjects())
        {
            for (ResolvedJavaField __field : vobj.type().getInstanceFields(true))
            {
                if (__field.isFinal())
                {
                    __barrier = __barrier | MemoryBarriers.LOAD_STORE;
                    break outer;
                }
            }
        }
        __graph.addAfterFixed(__commit, __graph.add(new MembarNode(__barrier, LocationIdentity.init())));
    }

    ///
    // @param field the field whose barrier type should be returned
    ///
    protected BarrierType fieldLoadBarrierType(ResolvedJavaField __field)
    {
        return BarrierType.NONE;
    }

    protected BarrierType fieldStoreBarrierType(ResolvedJavaField __field)
    {
        if (__field.getJavaKind() == JavaKind.Object)
        {
            return BarrierType.IMPRECISE;
        }
        return BarrierType.NONE;
    }

    protected BarrierType arrayStoreBarrierType(JavaKind __elementKind)
    {
        if (__elementKind == JavaKind.Object)
        {
            return BarrierType.PRECISE;
        }
        return BarrierType.NONE;
    }

    public BarrierType fieldInitializationBarrier(JavaKind __entryKind)
    {
        return __entryKind == JavaKind.Object ? BarrierType.IMPRECISE : BarrierType.NONE;
    }

    public BarrierType arrayInitializationBarrier(JavaKind __entryKind)
    {
        return __entryKind == JavaKind.Object ? BarrierType.PRECISE : BarrierType.NONE;
    }

    private static BarrierType unsafeStoreBarrierType(RawStoreNode __store)
    {
        if (!__store.needsBarrier())
        {
            return BarrierType.NONE;
        }
        return storeBarrierType(__store.object(), __store.value());
    }

    private static BarrierType storeBarrierType(ValueNode __object, ValueNode __value)
    {
        if (__value.getStackKind() == JavaKind.Object && __object.getStackKind() == JavaKind.Object)
        {
            ResolvedJavaType __type = StampTool.typeOrNull(__object);
            if (__type != null && !__type.isArray())
            {
                return BarrierType.IMPRECISE;
            }
            else
            {
                return BarrierType.PRECISE;
            }
        }
        return BarrierType.NONE;
    }

    public abstract int fieldOffset(ResolvedJavaField __field);

    public FieldLocationIdentity fieldLocationIdentity(ResolvedJavaField __field)
    {
        return new FieldLocationIdentity(__field);
    }

    public abstract ValueNode staticFieldBase(StructuredGraph __graph, ResolvedJavaField __field);

    @Override
    public int arrayScalingFactor(JavaKind __elementKind)
    {
        return this.___target.arch.getPlatformKind(__elementKind).getSizeInBytes();
    }

    public Stamp loadStamp(Stamp __stamp, JavaKind __kind)
    {
        return loadStamp(__stamp, __kind, true);
    }

    private boolean useCompressedOops(JavaKind __kind, boolean __compressible)
    {
        return __kind == JavaKind.Object && __compressible && this.___useCompressedOops;
    }

    protected abstract Stamp loadCompressedStamp(ObjectStamp __stamp);

    ///
    // @param compressible whether the stamp should be compressible
    ///
    protected Stamp loadStamp(Stamp __stamp, JavaKind __kind, boolean __compressible)
    {
        if (useCompressedOops(__kind, __compressible))
        {
            return loadCompressedStamp((ObjectStamp) __stamp);
        }

        switch (__kind)
        {
            case Boolean:
            case Byte:
                return IntegerStamp.OPS.getNarrow().foldStamp(32, 8, __stamp);
            case Char:
            case Short:
                return IntegerStamp.OPS.getNarrow().foldStamp(32, 16, __stamp);
        }
        return __stamp;
    }

    public final ValueNode implicitLoadConvert(StructuredGraph __graph, JavaKind __kind, ValueNode __value)
    {
        return implicitLoadConvert(__graph, __kind, __value, true);
    }

    public ValueNode implicitLoadConvert(JavaKind __kind, ValueNode __value)
    {
        return implicitLoadConvert(__kind, __value, true);
    }

    protected final ValueNode implicitLoadConvert(StructuredGraph __graph, JavaKind __kind, ValueNode __value, boolean __compressible)
    {
        ValueNode __ret = implicitLoadConvert(__kind, __value, __compressible);
        if (!__ret.isAlive())
        {
            __ret = __graph.addOrUnique(__ret);
        }
        return __ret;
    }

    protected abstract ValueNode newCompressionNode(CompressionOp __op, ValueNode __value);

    ///
    // @param compressible whether the convert should be compressible
    ///
    protected ValueNode implicitLoadConvert(JavaKind __kind, ValueNode __value, boolean __compressible)
    {
        if (useCompressedOops(__kind, __compressible))
        {
            return newCompressionNode(CompressionOp.Uncompress, __value);
        }

        switch (__kind)
        {
            case Byte:
            case Short:
                return new SignExtendNode(__value, 32);
            case Boolean:
            case Char:
                return new ZeroExtendNode(__value, 32);
        }
        return __value;
    }

    public final ValueNode implicitStoreConvert(StructuredGraph __graph, JavaKind __kind, ValueNode __value)
    {
        return implicitStoreConvert(__graph, __kind, __value, true);
    }

    public ValueNode implicitStoreConvert(JavaKind __kind, ValueNode __value)
    {
        return implicitStoreConvert(__kind, __value, true);
    }

    protected final ValueNode implicitStoreConvert(StructuredGraph __graph, JavaKind __kind, ValueNode __value, boolean __compressible)
    {
        ValueNode __ret = implicitStoreConvert(__kind, __value, __compressible);
        if (!__ret.isAlive())
        {
            __ret = __graph.addOrUnique(__ret);
        }
        return __ret;
    }

    ///
    // @param compressible whether the covert should be compressible
    ///
    protected ValueNode implicitStoreConvert(JavaKind __kind, ValueNode __value, boolean __compressible)
    {
        if (useCompressedOops(__kind, __compressible))
        {
            return newCompressionNode(CompressionOp.Compress, __value);
        }

        switch (__kind)
        {
            case Boolean:
            case Byte:
                return new NarrowNode(__value, 8);
            case Char:
            case Short:
                return new NarrowNode(__value, 16);
        }
        return __value;
    }

    protected abstract ValueNode createReadHub(StructuredGraph __graph, ValueNode __object, LoweringTool __tool);

    protected abstract ValueNode createReadArrayComponentHub(StructuredGraph __graph, ValueNode __arrayHub, FixedNode __anchor);

    protected GuardingNode getBoundsCheck(AccessIndexedNode __n, ValueNode __array, LoweringTool __tool)
    {
        StructuredGraph __graph = __n.graph();
        ValueNode __arrayLength = ArrayLengthNode.readArrayLength(__array, __tool.getConstantReflection());
        if (__arrayLength == null)
        {
            __arrayLength = createReadArrayLength(__array, __n, __tool);
        }
        else
        {
            __arrayLength = __arrayLength.isAlive() ? __arrayLength : __graph.addOrUniqueWithInputs(__arrayLength);
        }

        LogicNode __boundsCheck = IntegerBelowNode.create(__n.index(), __arrayLength, NodeView.DEFAULT);
        if (__boundsCheck.isTautology())
        {
            return null;
        }
        return __tool.createGuard(__n, __graph.addOrUniqueWithInputs(__boundsCheck), DeoptimizationReason.BoundsCheckException, DeoptimizationAction.InvalidateReprofile);
    }

    protected GuardingNode createNullCheck(ValueNode __object, FixedNode __before, LoweringTool __tool)
    {
        if (StampTool.isPointerNonNull(__object))
        {
            return null;
        }
        return __tool.createGuard(__before, __before.graph().unique(IsNullNode.create(__object)), DeoptimizationReason.NullCheckException, DeoptimizationAction.InvalidateReprofile, JavaConstant.NULL_POINTER, true);
    }

    protected ValueNode createNullCheckedValue(ValueNode __object, FixedNode __before, LoweringTool __tool)
    {
        GuardingNode __nullCheck = createNullCheck(__object, __before, __tool);
        if (__nullCheck == null)
        {
            return __object;
        }
        return __before.graph().maybeAddOrUnique(PiNode.create(__object, (__object.stamp(NodeView.DEFAULT)).join(StampFactory.objectNonNull()), (ValueNode) __nullCheck));
    }

    @Override
    public ValueNode reconstructArrayIndex(JavaKind __elementKind, AddressNode __address)
    {
        StructuredGraph __graph = __address.graph();
        ValueNode __offset = ((OffsetAddressNode) __address).getOffset();

        int __base = arrayBaseOffset(__elementKind);
        ValueNode __scaledIndex = __graph.unique(new SubNode(__offset, ConstantNode.forIntegerStamp(__offset.stamp(NodeView.DEFAULT), __base, __graph)));

        int __shift = CodeUtil.log2(arrayScalingFactor(__elementKind));
        ValueNode __ret = __graph.unique(new RightShiftNode(__scaledIndex, ConstantNode.forInt(__shift, __graph)));
        return IntegerConvertNode.convert(__ret, StampFactory.forKind(JavaKind.Int), __graph, NodeView.DEFAULT);
    }
}
