package giraaff.hotspot.meta;

import java.lang.ref.Reference;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaField;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import org.graalvm.word.LocationIdentity;

import giraaff.api.directives.GraalDirectives;
import giraaff.core.common.CompressEncoding;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.graph.Node;
import giraaff.graph.NodeInputList;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
import giraaff.hotspot.nodes.BeginLockScopeNode;
import giraaff.hotspot.nodes.ComputeObjectAddressNode;
import giraaff.hotspot.nodes.G1ArrayRangePostWriteBarrier;
import giraaff.hotspot.nodes.G1ArrayRangePreWriteBarrier;
import giraaff.hotspot.nodes.G1PostWriteBarrier;
import giraaff.hotspot.nodes.G1PreWriteBarrier;
import giraaff.hotspot.nodes.G1ReferentFieldReadBarrier;
import giraaff.hotspot.nodes.GetObjectAddressNode;
import giraaff.hotspot.nodes.HotSpotCompressionNode;
import giraaff.hotspot.nodes.HotSpotDirectCallTargetNode;
import giraaff.hotspot.nodes.HotSpotIndirectCallTargetNode;
import giraaff.hotspot.nodes.SerialArrayRangeWriteBarrier;
import giraaff.hotspot.nodes.SerialWriteBarrier;
import giraaff.hotspot.nodes.aot.InitializeKlassNode;
import giraaff.hotspot.nodes.aot.ResolveConstantNode;
import giraaff.hotspot.nodes.aot.ResolveDynamicConstantNode;
import giraaff.hotspot.nodes.aot.ResolveMethodAndLoadCountersNode;
import giraaff.hotspot.nodes.type.HotSpotNarrowOopStamp;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.nodes.type.MethodPointerStamp;
import giraaff.hotspot.replacements.ClassGetHubNode;
import giraaff.hotspot.replacements.HashCodeSnippets;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.replacements.HubGetClassNode;
import giraaff.hotspot.replacements.IdentityHashCodeNode;
import giraaff.hotspot.replacements.InstanceOfSnippets;
import giraaff.hotspot.replacements.KlassLayoutHelperNode;
import giraaff.hotspot.replacements.LoadExceptionObjectSnippets;
import giraaff.hotspot.replacements.MonitorSnippets;
import giraaff.hotspot.replacements.NewObjectSnippets;
import giraaff.hotspot.replacements.StringToBytesSnippets;
import giraaff.hotspot.replacements.UnsafeLoadSnippets;
import giraaff.hotspot.replacements.WriteBarrierSnippets;
import giraaff.hotspot.replacements.aot.ResolveConstantSnippets;
import giraaff.hotspot.replacements.arraycopy.ArrayCopyNode;
import giraaff.hotspot.replacements.arraycopy.ArrayCopySnippets;
import giraaff.hotspot.replacements.arraycopy.ArrayCopyWithSlowPathNode;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractDeoptimizeNode;
import giraaff.nodes.CompressionNode.CompressionOp;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoweredCallTargetNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.SafepointNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.UnwindNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.calc.IntegerDivRemNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.calc.RemNode;
import giraaff.nodes.debug.StringToBytesNode;
import giraaff.nodes.extended.BytecodeExceptionNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.extended.GetClassNode;
import giraaff.nodes.extended.GuardedUnsafeLoadNode;
import giraaff.nodes.extended.LoadHubNode;
import giraaff.nodes.extended.LoadMethodNode;
import giraaff.nodes.extended.OSRLocalNode;
import giraaff.nodes.extended.OSRLockNode;
import giraaff.nodes.extended.OSRMonitorEnterNode;
import giraaff.nodes.extended.OSRStartNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.extended.StoreHubNode;
import giraaff.nodes.java.ClassIsAssignableFromNode;
import giraaff.nodes.java.DynamicNewArrayNode;
import giraaff.nodes.java.DynamicNewInstanceNode;
import giraaff.nodes.java.InstanceOfDynamicNode;
import giraaff.nodes.java.InstanceOfNode;
import giraaff.nodes.java.LoadExceptionObjectNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.java.MonitorExitNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.java.NewArrayNode;
import giraaff.nodes.java.NewInstanceNode;
import giraaff.nodes.java.NewMultiArrayNode;
import giraaff.nodes.java.RawMonitorEnterNode;
import giraaff.nodes.memory.FloatingReadNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.memory.ReadNode;
import giraaff.nodes.memory.WriteNode;
import giraaff.nodes.memory.address.AddressNode;
import giraaff.nodes.spi.LoweringProvider;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.StampProvider;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.replacements.DefaultJavaLoweringProvider;
import giraaff.util.GraalError;

/**
 * HotSpot implementation of {@link LoweringProvider}.
 */
// @class DefaultHotSpotLoweringProvider
public class DefaultHotSpotLoweringProvider extends DefaultJavaLoweringProvider implements HotSpotLoweringProvider
{
    // @field
    protected final HotSpotGraalRuntime runtime;
    // @field
    protected final HotSpotRegistersProvider registers;
    // @field
    protected final HotSpotConstantReflectionProvider constantReflection;

    // @field
    protected InstanceOfSnippets.Templates instanceofSnippets;
    // @field
    protected NewObjectSnippets.Templates newObjectSnippets;
    // @field
    protected MonitorSnippets.Templates monitorSnippets;
    // @field
    protected WriteBarrierSnippets.Templates writeBarrierSnippets;
    // @field
    protected LoadExceptionObjectSnippets.Templates exceptionObjectSnippets;
    // @field
    protected UnsafeLoadSnippets.Templates unsafeLoadSnippets;
    // @field
    protected ArrayCopySnippets.Templates arraycopySnippets;
    // @field
    protected StringToBytesSnippets.Templates stringToBytesSnippets;
    // @field
    protected HashCodeSnippets.Templates hashCodeSnippets;
    // @field
    protected ResolveConstantSnippets.Templates resolveConstantSnippets;

    // @cons
    public DefaultHotSpotLoweringProvider(HotSpotGraalRuntime __runtime, MetaAccessProvider __metaAccess, ForeignCallsProvider __foreignCalls, HotSpotRegistersProvider __registers, HotSpotConstantReflectionProvider __constantReflection, TargetDescription __target)
    {
        super(__metaAccess, __foreignCalls, __target, HotSpotRuntime.useCompressedOops);
        this.runtime = __runtime;
        this.registers = __registers;
        this.constantReflection = __constantReflection;
    }

    @Override
    public void initialize(HotSpotProviders __providers)
    {
        super.initialize(__providers, __providers.getSnippetReflection());

        instanceofSnippets = new InstanceOfSnippets.Templates(__providers, target);
        newObjectSnippets = new NewObjectSnippets.Templates(__providers, target);
        monitorSnippets = new MonitorSnippets.Templates(__providers, target, HotSpotRuntime.useFastLocking);
        writeBarrierSnippets = new WriteBarrierSnippets.Templates(__providers, target, HotSpotRuntime.useCompressedOops ? HotSpotRuntime.oopEncoding : null);
        exceptionObjectSnippets = new LoadExceptionObjectSnippets.Templates(__providers, target);
        unsafeLoadSnippets = new UnsafeLoadSnippets.Templates(__providers, target);
        arraycopySnippets = new ArrayCopySnippets.Templates(__providers, target);
        stringToBytesSnippets = new StringToBytesSnippets.Templates(__providers, target);
        hashCodeSnippets = new HashCodeSnippets.Templates(__providers, target);
        resolveConstantSnippets = new ResolveConstantSnippets.Templates(__providers, target);
    }

    public MonitorSnippets.Templates getMonitorSnippets()
    {
        return monitorSnippets;
    }

    @Override
    public void lower(Node __n, LoweringTool __tool)
    {
        StructuredGraph __graph = (StructuredGraph) __n.graph();
        if (__n instanceof Invoke)
        {
            lowerInvoke((Invoke) __n, __tool, __graph);
        }
        else if (__n instanceof LoadMethodNode)
        {
            lowerLoadMethodNode((LoadMethodNode) __n);
        }
        else if (__n instanceof GetClassNode)
        {
            lowerGetClassNode((GetClassNode) __n, __tool, __graph);
        }
        else if (__n instanceof StoreHubNode)
        {
            lowerStoreHubNode((StoreHubNode) __n, __graph);
        }
        else if (__n instanceof OSRStartNode)
        {
            lowerOSRStartNode((OSRStartNode) __n);
        }
        else if (__n instanceof BytecodeExceptionNode)
        {
            lowerBytecodeExceptionNode((BytecodeExceptionNode) __n);
        }
        else if (__n instanceof InstanceOfNode)
        {
            InstanceOfNode __instanceOfNode = (InstanceOfNode) __n;
            if (__graph.getGuardsStage().areDeoptsFixed())
            {
                instanceofSnippets.lower(__instanceOfNode, __tool);
            }
            else
            {
                if (__instanceOfNode.allowsNull())
                {
                    ValueNode __object = __instanceOfNode.getValue();
                    LogicNode __newTypeCheck = __graph.addOrUniqueWithInputs(InstanceOfNode.create(__instanceOfNode.type(), __object, __instanceOfNode.profile(), __instanceOfNode.getAnchor()));
                    LogicNode __newNode = LogicNode.or(__graph.unique(IsNullNode.create(__object)), __newTypeCheck, GraalDirectives.UNLIKELY_PROBABILITY);
                    __instanceOfNode.replaceAndDelete(__newNode);
                }
            }
        }
        else if (__n instanceof InstanceOfDynamicNode)
        {
            InstanceOfDynamicNode __instanceOfDynamicNode = (InstanceOfDynamicNode) __n;
            if (__graph.getGuardsStage().areDeoptsFixed())
            {
                instanceofSnippets.lower(__instanceOfDynamicNode, __tool);
            }
            else
            {
                ValueNode __mirror = __instanceOfDynamicNode.getMirrorOrHub();
                if (__mirror.stamp(NodeView.DEFAULT).getStackKind() == JavaKind.Object)
                {
                    ClassGetHubNode __classGetHub = __graph.unique(new ClassGetHubNode(__mirror));
                    __instanceOfDynamicNode.setMirror(__classGetHub);
                }

                if (__instanceOfDynamicNode.allowsNull())
                {
                    ValueNode __object = __instanceOfDynamicNode.getObject();
                    LogicNode __newTypeCheck = __graph.addOrUniqueWithInputs(InstanceOfDynamicNode.create(__graph.getAssumptions(), __tool.getConstantReflection(), __instanceOfDynamicNode.getMirrorOrHub(), __object, false));
                    LogicNode __newNode = LogicNode.or(__graph.unique(IsNullNode.create(__object)), __newTypeCheck, GraalDirectives.UNLIKELY_PROBABILITY);
                    __instanceOfDynamicNode.replaceAndDelete(__newNode);
                }
            }
        }
        else if (__n instanceof ClassIsAssignableFromNode)
        {
            if (__graph.getGuardsStage().areDeoptsFixed())
            {
                instanceofSnippets.lower((ClassIsAssignableFromNode) __n, __tool);
            }
        }
        else if (__n instanceof NewInstanceNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower((NewInstanceNode) __n, registers, __tool);
            }
        }
        else if (__n instanceof DynamicNewInstanceNode)
        {
            DynamicNewInstanceNode __newInstanceNode = (DynamicNewInstanceNode) __n;
            if (__newInstanceNode.getClassClass() == null)
            {
                JavaConstant __classClassMirror = constantReflection.forObject(Class.class);
                ConstantNode __classClass = ConstantNode.forConstant(__classClassMirror, __tool.getMetaAccess(), __graph);
                __newInstanceNode.setClassClass(__classClass);
            }
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower(__newInstanceNode, registers, __tool);
            }
        }
        else if (__n instanceof NewArrayNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower((NewArrayNode) __n, registers, __tool);
            }
        }
        else if (__n instanceof DynamicNewArrayNode)
        {
            DynamicNewArrayNode __dynamicNewArrayNode = (DynamicNewArrayNode) __n;
            if (__dynamicNewArrayNode.getVoidClass() == null)
            {
                JavaConstant __voidClassMirror = constantReflection.forObject(void.class);
                ConstantNode __voidClass = ConstantNode.forConstant(__voidClassMirror, __tool.getMetaAccess(), __graph);
                __dynamicNewArrayNode.setVoidClass(__voidClass);
            }
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower(__dynamicNewArrayNode, registers, __tool);
            }
        }
        else if (__n instanceof RawMonitorEnterNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                monitorSnippets.lower((RawMonitorEnterNode) __n, registers, __tool);
            }
        }
        else if (__n instanceof MonitorExitNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                monitorSnippets.lower((MonitorExitNode) __n, registers, __tool);
            }
        }
        else if (__n instanceof ArrayCopyNode)
        {
            arraycopySnippets.lower((ArrayCopyNode) __n, __tool);
        }
        else if (__n instanceof ArrayCopyWithSlowPathNode)
        {
            arraycopySnippets.lower((ArrayCopyWithSlowPathNode) __n, __tool);
        }
        else if (__n instanceof G1PreWriteBarrier)
        {
            writeBarrierSnippets.lower((G1PreWriteBarrier) __n, registers, __tool);
        }
        else if (__n instanceof G1PostWriteBarrier)
        {
            writeBarrierSnippets.lower((G1PostWriteBarrier) __n, registers, __tool);
        }
        else if (__n instanceof G1ReferentFieldReadBarrier)
        {
            writeBarrierSnippets.lower((G1ReferentFieldReadBarrier) __n, registers, __tool);
        }
        else if (__n instanceof SerialWriteBarrier)
        {
            writeBarrierSnippets.lower((SerialWriteBarrier) __n, __tool);
        }
        else if (__n instanceof SerialArrayRangeWriteBarrier)
        {
            writeBarrierSnippets.lower((SerialArrayRangeWriteBarrier) __n, __tool);
        }
        else if (__n instanceof G1ArrayRangePreWriteBarrier)
        {
            writeBarrierSnippets.lower((G1ArrayRangePreWriteBarrier) __n, registers, __tool);
        }
        else if (__n instanceof G1ArrayRangePostWriteBarrier)
        {
            writeBarrierSnippets.lower((G1ArrayRangePostWriteBarrier) __n, registers, __tool);
        }
        else if (__n instanceof NewMultiArrayNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower((NewMultiArrayNode) __n, __tool);
            }
        }
        else if (__n instanceof LoadExceptionObjectNode)
        {
            exceptionObjectSnippets.lower((LoadExceptionObjectNode) __n, registers, __tool);
        }
        else if (__n instanceof StringToBytesNode)
        {
            if (__graph.getGuardsStage().areDeoptsFixed())
            {
                stringToBytesSnippets.lower((StringToBytesNode) __n, __tool);
            }
        }
        else if (__n instanceof IntegerDivRemNode)
        {
            // Nothing to do for division nodes. The HotSpot signal handler catches divisions by zero and the MIN_VALUE / -1 cases.
        }
        else if (__n instanceof AbstractDeoptimizeNode || __n instanceof UnwindNode || __n instanceof RemNode || __n instanceof SafepointNode)
        {
            // No lowering, we generate LIR directly for these nodes.
        }
        else if (__n instanceof ClassGetHubNode)
        {
            lowerClassGetHubNode((ClassGetHubNode) __n, __tool);
        }
        else if (__n instanceof HubGetClassNode)
        {
            lowerHubGetClassNode((HubGetClassNode) __n, __tool);
        }
        else if (__n instanceof KlassLayoutHelperNode)
        {
            lowerKlassLayoutHelperNode((KlassLayoutHelperNode) __n, __tool);
        }
        else if (__n instanceof ComputeObjectAddressNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                lowerComputeObjectAddressNode((ComputeObjectAddressNode) __n);
            }
        }
        else if (__n instanceof IdentityHashCodeNode)
        {
            hashCodeSnippets.lower((IdentityHashCodeNode) __n, __tool);
        }
        else if (__n instanceof ResolveDynamicConstantNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((ResolveDynamicConstantNode) __n, __tool);
            }
        }
        else if (__n instanceof ResolveConstantNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((ResolveConstantNode) __n, __tool);
            }
        }
        else if (__n instanceof ResolveMethodAndLoadCountersNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((ResolveMethodAndLoadCountersNode) __n, __tool);
            }
        }
        else if (__n instanceof InitializeKlassNode)
        {
            if (__graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((InitializeKlassNode) __n, __tool);
            }
        }
        else
        {
            super.lower(__n, __tool);
        }
    }

    private static void lowerComputeObjectAddressNode(ComputeObjectAddressNode __n)
    {
        /*
         * Lower the node into a ComputeObjectAddress node and an Add but ensure that it's below any
         * potential safepoints and above it's uses.
         */
        for (Node __use : __n.usages().snapshot())
        {
            if (__use instanceof FixedNode)
            {
                FixedNode __fixed = (FixedNode) __use;
                StructuredGraph __graph = __n.graph();
                GetObjectAddressNode __address = __graph.add(new GetObjectAddressNode(__n.getObject()));
                __graph.addBeforeFixed(__fixed, __address);
                AddNode __add = __graph.addOrUnique(new AddNode(__address, __n.getOffset()));
                __use.replaceFirstInput(__n, __add);
            }
            else
            {
                throw GraalError.shouldNotReachHere("Unexpected floating use of ComputeObjectAddressNode " + __n);
            }
        }
        GraphUtil.unlinkFixedNode(__n);
        __n.safeDelete();
    }

    private void lowerKlassLayoutHelperNode(KlassLayoutHelperNode __n, LoweringTool __tool)
    {
        if (__tool.getLoweringStage() == LoweringTool.StandardLoweringStage.HIGH_TIER)
        {
            return;
        }
        StructuredGraph __graph = __n.graph();
        AddressNode __address = createOffsetAddress(__graph, __n.getHub(), HotSpotRuntime.klassLayoutHelperOffset);
        __n.replaceAtUsagesAndDelete(__graph.unique(new FloatingReadNode(__address, HotSpotReplacementsUtil.KLASS_LAYOUT_HELPER_LOCATION, null, __n.stamp(NodeView.DEFAULT), null, BarrierType.NONE)));
    }

    private void lowerHubGetClassNode(HubGetClassNode __n, LoweringTool __tool)
    {
        if (__tool.getLoweringStage() == LoweringTool.StandardLoweringStage.HIGH_TIER)
        {
            return;
        }

        StructuredGraph __graph = __n.graph();
        AddressNode __address = createOffsetAddress(__graph, __n.getHub(), HotSpotRuntime.classMirrorOffset);
        FloatingReadNode __read = __graph.unique(new FloatingReadNode(__address, HotSpotReplacementsUtil.CLASS_MIRROR_LOCATION, null, StampFactory.forKind(target.wordJavaKind), null, BarrierType.NONE));
        __address = createOffsetAddress(__graph, __read, 0);
        __read = __graph.unique(new FloatingReadNode(__address, HotSpotReplacementsUtil.CLASS_MIRROR_HANDLE_LOCATION, null, __n.stamp(NodeView.DEFAULT), null, BarrierType.NONE));
        __n.replaceAtUsagesAndDelete(__read);
    }

    private void lowerClassGetHubNode(ClassGetHubNode __n, LoweringTool __tool)
    {
        if (__tool.getLoweringStage() == LoweringTool.StandardLoweringStage.HIGH_TIER)
        {
            return;
        }

        StructuredGraph __graph = __n.graph();
        AddressNode __address = createOffsetAddress(__graph, __n.getValue(), HotSpotRuntime.klassOffset);
        FloatingReadNode __read = __graph.unique(new FloatingReadNode(__address, HotSpotReplacementsUtil.CLASS_KLASS_LOCATION, null, __n.stamp(NodeView.DEFAULT), null, BarrierType.NONE));
        __n.replaceAtUsagesAndDelete(__read);
    }

    private void lowerInvoke(Invoke __invoke, LoweringTool __tool, StructuredGraph __graph)
    {
        if (__invoke.callTarget() instanceof MethodCallTargetNode)
        {
            MethodCallTargetNode __callTarget = (MethodCallTargetNode) __invoke.callTarget();
            NodeInputList<ValueNode> __parameters = __callTarget.arguments();
            ValueNode __receiver = __parameters.size() <= 0 ? null : __parameters.get(0);
            if (!__callTarget.isStatic() && __receiver.stamp(NodeView.DEFAULT) instanceof ObjectStamp && !StampTool.isPointerNonNull(__receiver))
            {
                ValueNode __nonNullReceiver = createNullCheckedValue(__receiver, __invoke.asNode(), __tool);
                __parameters.set(0, __nonNullReceiver);
                __receiver = __nonNullReceiver;
            }
            JavaType[] __signature = __callTarget.targetMethod().getSignature().toParameterTypes(__callTarget.isStatic() ? null : __callTarget.targetMethod().getDeclaringClass());

            LoweredCallTargetNode __loweredCallTarget = null;
            if (GraalOptions.inlineVTableStubs && __callTarget.invokeKind().isIndirect() && (GraalOptions.alwaysInlineVTableStubs || __invoke.isPolymorphic()))
            {
                HotSpotResolvedJavaMethod __hsMethod = (HotSpotResolvedJavaMethod) __callTarget.targetMethod();
                ResolvedJavaType __receiverType = __invoke.getReceiverType();
                if (__hsMethod.isInVirtualMethodTable(__receiverType))
                {
                    JavaKind __wordKind = runtime.getTarget().wordJavaKind;
                    ValueNode __hub = createReadHub(__graph, __receiver, __tool);

                    ReadNode __metaspaceMethod = createReadVirtualMethod(__graph, __hub, __hsMethod, __receiverType);
                    // We use LocationNode.ANY_LOCATION for the reads that access the compiled
                    // code entry as HotSpot does not guarantee they are final values.
                    AddressNode __address = createOffsetAddress(__graph, __metaspaceMethod, HotSpotRuntime.methodCompiledEntryOffset);
                    ReadNode __compiledEntry = __graph.add(new ReadNode(__address, LocationIdentity.any(), StampFactory.forKind(__wordKind), BarrierType.NONE));

                    __loweredCallTarget = __graph.add(new HotSpotIndirectCallTargetNode(__metaspaceMethod, __compiledEntry, __parameters.toArray(new ValueNode[__parameters.size()]), __callTarget.returnStamp(), __signature, __callTarget.targetMethod(), HotSpotCallingConventionType.JavaCall, __callTarget.invokeKind()));

                    __graph.addBeforeFixed(__invoke.asNode(), __metaspaceMethod);
                    __graph.addAfterFixed(__metaspaceMethod, __compiledEntry);
                }
            }

            if (__loweredCallTarget == null)
            {
                __loweredCallTarget = __graph.add(new HotSpotDirectCallTargetNode(__parameters.toArray(new ValueNode[__parameters.size()]), __callTarget.returnStamp(), __signature, __callTarget.targetMethod(), HotSpotCallingConventionType.JavaCall, __callTarget.invokeKind()));
            }
            __callTarget.replaceAndDelete(__loweredCallTarget);
        }
    }

    @Override
    protected Stamp loadCompressedStamp(ObjectStamp __stamp)
    {
        return HotSpotNarrowOopStamp.compressed(__stamp, HotSpotRuntime.oopEncoding);
    }

    @Override
    protected ValueNode newCompressionNode(CompressionOp __op, ValueNode __value)
    {
        return new HotSpotCompressionNode(__op, __value, HotSpotRuntime.oopEncoding);
    }

    @Override
    public ValueNode staticFieldBase(StructuredGraph __graph, ResolvedJavaField __f)
    {
        HotSpotResolvedJavaField __field = (HotSpotResolvedJavaField) __f;
        JavaConstant __base = constantReflection.asJavaClass(__field.getDeclaringClass());
        return ConstantNode.forConstant(__base, metaAccess, __graph);
    }

    @Override
    protected ValueNode createReadArrayComponentHub(StructuredGraph __graph, ValueNode __arrayHub, FixedNode __anchor)
    {
        /*
         * Anchor the read of the element klass to the cfg, because it is only valid when arrayClass
         * is an object class, which might not be the case in other parts of the compiled method.
         */
        AddressNode __address = createOffsetAddress(__graph, __arrayHub, HotSpotRuntime.arrayClassElementOffset);
        return __graph.unique(new FloatingReadNode(__address, HotSpotReplacementsUtil.OBJ_ARRAY_KLASS_ELEMENT_KLASS_LOCATION, null, KlassPointerStamp.klassNonNull(), AbstractBeginNode.prevBegin(__anchor)));
    }

    @Override
    protected void lowerUnsafeLoadNode(RawLoadNode __load, LoweringTool __tool)
    {
        StructuredGraph __graph = __load.graph();
        if (!(__load instanceof GuardedUnsafeLoadNode) && !__graph.getGuardsStage().allowsFloatingGuards() && addReadBarrier(__load))
        {
            unsafeLoadSnippets.lower(__load, __tool);
        }
        else
        {
            super.lowerUnsafeLoadNode(__load, __tool);
        }
    }

    private void lowerLoadMethodNode(LoadMethodNode __loadMethodNode)
    {
        StructuredGraph __graph = __loadMethodNode.graph();
        HotSpotResolvedJavaMethod __method = (HotSpotResolvedJavaMethod) __loadMethodNode.getMethod();
        ReadNode __metaspaceMethod = createReadVirtualMethod(__graph, __loadMethodNode.getHub(), __method, __loadMethodNode.getReceiverType());
        __graph.replaceFixed(__loadMethodNode, __metaspaceMethod);
    }

    private static void lowerGetClassNode(GetClassNode __getClass, LoweringTool __tool, StructuredGraph __graph)
    {
        StampProvider __stampProvider = __tool.getStampProvider();
        LoadHubNode __hub = __graph.unique(new LoadHubNode(__stampProvider, __getClass.getObject()));
        HubGetClassNode __hubGetClass = __graph.unique(new HubGetClassNode(__tool.getMetaAccess(), __hub));
        __getClass.replaceAtUsagesAndDelete(__hubGetClass);
        __hub.lower(__tool);
        __hubGetClass.lower(__tool);
    }

    private void lowerStoreHubNode(StoreHubNode __storeHub, StructuredGraph __graph)
    {
        WriteNode __hub = createWriteHub(__graph, __storeHub.getObject(), __storeHub.getValue());
        __graph.replaceFixed(__storeHub, __hub);
    }

    @Override
    public BarrierType fieldInitializationBarrier(JavaKind __entryKind)
    {
        return (__entryKind == JavaKind.Object && !HotSpotRuntime.useDeferredInitBarriers) ? BarrierType.IMPRECISE : BarrierType.NONE;
    }

    @Override
    public BarrierType arrayInitializationBarrier(JavaKind __entryKind)
    {
        return (__entryKind == JavaKind.Object && !HotSpotRuntime.useDeferredInitBarriers) ? BarrierType.PRECISE : BarrierType.NONE;
    }

    private void lowerOSRStartNode(OSRStartNode __osrStart)
    {
        StructuredGraph __graph = __osrStart.graph();
        if (__graph.getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            StartNode __newStart = __graph.add(new StartNode());
            ParameterNode __buffer = __graph.addWithoutUnique(new ParameterNode(0, StampPair.createSingle(StampFactory.forKind(runtime.getTarget().wordJavaKind))));
            ForeignCallNode __migrationEnd = __graph.add(new ForeignCallNode(foreignCalls, HotSpotForeignCallsProviderImpl.OSR_MIGRATION_END, __buffer));
            __migrationEnd.setStateAfter(__osrStart.stateAfter());
            __newStart.setNext(__migrationEnd);
            FixedNode __next = __osrStart.next();
            __osrStart.setNext(null);
            __migrationEnd.setNext(__next);
            __graph.setStart(__newStart);

            final int __wordSize = target.wordSize;

            // taken from c2 locals_addr = osr_buf + (max_locals-1)*wordSize)
            int __localsOffset = (__graph.method().getMaxLocals() - 1) * __wordSize;
            for (OSRLocalNode __osrLocal : __graph.getNodes(OSRLocalNode.TYPE))
            {
                int __size = __osrLocal.getStackKind().getSlotCount();
                int __offset = __localsOffset - (__osrLocal.index() + __size - 1) * __wordSize;
                AddressNode __address = createOffsetAddress(__graph, __buffer, __offset);
                ReadNode __load = __graph.add(new ReadNode(__address, LocationIdentity.any(), __osrLocal.stamp(NodeView.DEFAULT), BarrierType.NONE));
                __osrLocal.replaceAndDelete(__load);
                __graph.addBeforeFixed(__migrationEnd, __load);
            }

            // taken from c2 monitors_addr = osr_buf + (max_locals+mcnt*2-1)*wordSize)
            final int __lockCount = __osrStart.stateAfter().locksSize();
            final int __locksOffset = (__graph.method().getMaxLocals() + __lockCount * 2 - 1) * __wordSize;

            // first initialize the lock slots for all enters with the displaced marks read from the buffer
            for (OSRMonitorEnterNode __osrMonitorEnter : __graph.getNodes(OSRMonitorEnterNode.TYPE))
            {
                MonitorIdNode __monitorID = __osrMonitorEnter.getMonitorId();
                OSRLockNode __lock = (OSRLockNode) __osrMonitorEnter.object();
                final int __index = __lock.index();

                final int __offsetDisplacedHeader = __locksOffset - ((__index * 2) + 1) * __wordSize;
                final int __offsetLockObject = __locksOffset - __index * 2 * __wordSize;

                // load the displaced mark from the osr buffer
                AddressNode __addressDisplacedHeader = createOffsetAddress(__graph, __buffer, __offsetDisplacedHeader);
                ReadNode __loadDisplacedHeader = __graph.add(new ReadNode(__addressDisplacedHeader, LocationIdentity.any(), __lock.stamp(NodeView.DEFAULT), BarrierType.NONE));
                __graph.addBeforeFixed(__migrationEnd, __loadDisplacedHeader);

                // we need to initialize the stack slot for the lock
                BeginLockScopeNode __beginLockScope = __graph.add(new BeginLockScopeNode(__lock.getStackKind(), __monitorID.getLockDepth()));
                __graph.addBeforeFixed(__migrationEnd, __beginLockScope);

                // write the displaced mark to the correct stack slot
                AddressNode __addressDisplacedMark = createOffsetAddress(__graph, __beginLockScope, HotSpotRuntime.lockDisplacedMarkOffset);
                WriteNode __writeStackSlot = __graph.add(new WriteNode(__addressDisplacedMark, HotSpotReplacementsUtil.DISPLACED_MARK_WORD_LOCATION, __loadDisplacedHeader, BarrierType.NONE));
                __graph.addBeforeFixed(__migrationEnd, __writeStackSlot);

                // load the lock object from the osr buffer
                AddressNode __addressLockObject = createOffsetAddress(__graph, __buffer, __offsetLockObject);
                ReadNode __loadObject = __graph.add(new ReadNode(__addressLockObject, LocationIdentity.any(), __lock.stamp(NodeView.DEFAULT), BarrierType.NONE));
                __lock.replaceAndDelete(__loadObject);
                __graph.addBeforeFixed(__migrationEnd, __loadObject);
            }

            __osrStart.replaceAtUsagesAndDelete(__newStart);
        }
    }

    // @class DefaultHotSpotLoweringProvider.Exceptions
    static final class Exceptions
    {
        // @def
        protected static final ArrayIndexOutOfBoundsException cachedArrayIndexOutOfBoundsException;
        // @def
        protected static final NullPointerException cachedNullPointerException;

        static
        {
            cachedArrayIndexOutOfBoundsException = new ArrayIndexOutOfBoundsException();
            cachedArrayIndexOutOfBoundsException.setStackTrace(new StackTraceElement[0]);
            cachedNullPointerException = new NullPointerException();
            cachedNullPointerException.setStackTrace(new StackTraceElement[0]);
        }
    }

    // @class DefaultHotSpotLoweringProvider.RuntimeCalls
    public static final class RuntimeCalls
    {
        // @def
        public static final ForeignCallDescriptor CREATE_ARRAY_STORE_EXCEPTION = new ForeignCallDescriptor("createArrayStoreException", ArrayStoreException.class, Object.class);
        // @def
        public static final ForeignCallDescriptor CREATE_CLASS_CAST_EXCEPTION = new ForeignCallDescriptor("createClassCastException", ClassCastException.class, Object.class, KlassPointer.class);
        // @def
        public static final ForeignCallDescriptor CREATE_NULL_POINTER_EXCEPTION = new ForeignCallDescriptor("createNullPointerException", NullPointerException.class);
        // @def
        public static final ForeignCallDescriptor CREATE_OUT_OF_BOUNDS_EXCEPTION = new ForeignCallDescriptor("createOutOfBoundsException", ArrayIndexOutOfBoundsException.class, int.class);
    }

    private boolean throwCachedException(BytecodeExceptionNode __node)
    {
        Throwable __exception;
        if (__node.getExceptionClass() == NullPointerException.class)
        {
            __exception = Exceptions.cachedNullPointerException;
        }
        else if (__node.getExceptionClass() == ArrayIndexOutOfBoundsException.class)
        {
            __exception = Exceptions.cachedArrayIndexOutOfBoundsException;
        }
        else
        {
            return false;
        }

        StructuredGraph __graph = __node.graph();
        FloatingNode __exceptionNode = ConstantNode.forConstant(constantReflection.forObject(__exception), metaAccess, __graph);
        __graph.replaceFixedWithFloating(__node, __exceptionNode);
        return true;
    }

    private void lowerBytecodeExceptionNode(BytecodeExceptionNode __node)
    {
        if (GraalOptions.omitHotExceptionStacktrace)
        {
            if (throwCachedException(__node))
            {
                return;
            }
        }

        ForeignCallDescriptor __descriptor;
        if (__node.getExceptionClass() == NullPointerException.class)
        {
            __descriptor = RuntimeCalls.CREATE_NULL_POINTER_EXCEPTION;
        }
        else if (__node.getExceptionClass() == ArrayIndexOutOfBoundsException.class)
        {
            __descriptor = RuntimeCalls.CREATE_OUT_OF_BOUNDS_EXCEPTION;
        }
        else if (__node.getExceptionClass() == ArrayStoreException.class)
        {
            __descriptor = RuntimeCalls.CREATE_ARRAY_STORE_EXCEPTION;
        }
        else if (__node.getExceptionClass() == ClassCastException.class)
        {
            __descriptor = RuntimeCalls.CREATE_CLASS_CAST_EXCEPTION;
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }

        StructuredGraph __graph = __node.graph();
        ForeignCallNode __foreignCallNode = __graph.add(new ForeignCallNode(foreignCalls, __descriptor, __node.stamp(NodeView.DEFAULT), __node.getArguments()));
        __graph.replaceFixedWithFixed(__node, __foreignCallNode);
    }

    private boolean addReadBarrier(RawLoadNode __load)
    {
        if (HotSpotRuntime.useG1GC && __load.graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS && __load.object().getStackKind() == JavaKind.Object && __load.accessKind() == JavaKind.Object && !StampTool.isPointerAlwaysNull(__load.object()))
        {
            ResolvedJavaType __type = StampTool.typeOrNull(__load.object());
            if (__type != null && !__type.isArray())
            {
                return true;
            }
        }
        return false;
    }

    private ReadNode createReadVirtualMethod(StructuredGraph __graph, ValueNode __hub, HotSpotResolvedJavaMethod __method, ResolvedJavaType __receiverType)
    {
        return createReadVirtualMethod(__graph, __hub, __method.vtableEntryOffset(__receiverType));
    }

    private ReadNode createReadVirtualMethod(StructuredGraph __graph, ValueNode __hub, int __vtableEntryOffset)
    {
        // We use LocationNode.ANY_LOCATION for the reads that access the vtable
        // entry as HotSpot does not guarantee that this is a final value.
        Stamp __methodStamp = MethodPointerStamp.methodNonNull();
        AddressNode __address = createOffsetAddress(__graph, __hub, __vtableEntryOffset);
        return __graph.add(new ReadNode(__address, LocationIdentity.any(), __methodStamp, BarrierType.NONE));
    }

    @Override
    protected ValueNode createReadHub(StructuredGraph __graph, ValueNode __object, LoweringTool __tool)
    {
        if (__tool.getLoweringStage() != LoweringTool.StandardLoweringStage.LOW_TIER)
        {
            return __graph.unique(new LoadHubNode(__tool.getStampProvider(), __object));
        }

        KlassPointerStamp __hubStamp = KlassPointerStamp.klassNonNull();
        if (HotSpotRuntime.useCompressedClassPointers)
        {
            __hubStamp = __hubStamp.compressed(HotSpotRuntime.klassEncoding);
        }

        AddressNode __address = createOffsetAddress(__graph, __object, HotSpotRuntime.hubOffset);
        LocationIdentity __hubLocation = HotSpotRuntime.useCompressedClassPointers ? HotSpotReplacementsUtil.COMPRESSED_HUB_LOCATION : HotSpotReplacementsUtil.HUB_LOCATION;
        FloatingReadNode __memoryRead = __graph.unique(new FloatingReadNode(__address, __hubLocation, null, __hubStamp, null, BarrierType.NONE));
        if (HotSpotRuntime.useCompressedClassPointers)
        {
            return HotSpotCompressionNode.uncompress(__memoryRead, HotSpotRuntime.klassEncoding);
        }
        else
        {
            return __memoryRead;
        }
    }

    private WriteNode createWriteHub(StructuredGraph __graph, ValueNode __object, ValueNode __value)
    {
        ValueNode __writeValue = __value;
        if (HotSpotRuntime.useCompressedClassPointers)
        {
            __writeValue = HotSpotCompressionNode.compress(__value, HotSpotRuntime.klassEncoding);
        }

        AddressNode __address = createOffsetAddress(__graph, __object, HotSpotRuntime.hubOffset);
        return __graph.add(new WriteNode(__address, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, __writeValue, BarrierType.NONE));
    }

    @Override
    protected BarrierType fieldLoadBarrierType(ResolvedJavaField __f)
    {
        HotSpotResolvedJavaField __loadField = (HotSpotResolvedJavaField) __f;
        BarrierType __barrierType = BarrierType.NONE;
        if (HotSpotRuntime.useG1GC && __loadField.getJavaKind() == JavaKind.Object && metaAccess.lookupJavaType(Reference.class).equals(__loadField.getDeclaringClass()) && __loadField.getName().equals("referent"))
        {
            __barrierType = BarrierType.PRECISE;
        }
        return __barrierType;
    }

    @Override
    public int fieldOffset(ResolvedJavaField __f)
    {
        HotSpotResolvedJavaField __field = (HotSpotResolvedJavaField) __f;
        return __field.offset();
    }

    @Override
    public int arrayScalingFactor(JavaKind __kind)
    {
        if (HotSpotRuntime.useCompressedOops && __kind == JavaKind.Object)
        {
            return super.arrayScalingFactor(JavaKind.Int);
        }
        else
        {
            return super.arrayScalingFactor(__kind);
        }
    }

    @Override
    public int arrayBaseOffset(JavaKind __kind)
    {
        return HotSpotRuntime.getArrayBaseOffset(__kind);
    }

    @Override
    protected final JavaKind getStorageKind(ResolvedJavaField __field)
    {
        return __field.getJavaKind();
    }
}
