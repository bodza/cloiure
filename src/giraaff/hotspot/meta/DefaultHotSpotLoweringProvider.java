package giraaff.hotspot.meta;

import java.lang.ref.Reference;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.hotspot.HotSpotCallingConventionType;
import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
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
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
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
import giraaff.options.OptionValues;
import giraaff.replacements.DefaultJavaLoweringProvider;
import giraaff.util.GraalError;

/**
 * HotSpot implementation of {@link LoweringProvider}.
 */
public class DefaultHotSpotLoweringProvider extends DefaultJavaLoweringProvider implements HotSpotLoweringProvider
{
    protected final HotSpotGraalRuntimeProvider runtime;
    protected final HotSpotRegistersProvider registers;
    protected final HotSpotConstantReflectionProvider constantReflection;

    protected InstanceOfSnippets.Templates instanceofSnippets;
    protected NewObjectSnippets.Templates newObjectSnippets;
    protected MonitorSnippets.Templates monitorSnippets;
    protected WriteBarrierSnippets.Templates writeBarrierSnippets;
    protected LoadExceptionObjectSnippets.Templates exceptionObjectSnippets;
    protected UnsafeLoadSnippets.Templates unsafeLoadSnippets;
    protected ArrayCopySnippets.Templates arraycopySnippets;
    protected StringToBytesSnippets.Templates stringToBytesSnippets;
    protected HashCodeSnippets.Templates hashCodeSnippets;
    protected ResolveConstantSnippets.Templates resolveConstantSnippets;

    public DefaultHotSpotLoweringProvider(HotSpotGraalRuntimeProvider runtime, MetaAccessProvider metaAccess, ForeignCallsProvider foreignCalls, HotSpotRegistersProvider registers, HotSpotConstantReflectionProvider constantReflection, TargetDescription target)
    {
        super(metaAccess, foreignCalls, target, GraalHotSpotVMConfig.useCompressedOops);
        this.runtime = runtime;
        this.registers = registers;
        this.constantReflection = constantReflection;
    }

    @Override
    public void initialize(OptionValues options, HotSpotProviders providers)
    {
        super.initialize(options, providers, providers.getSnippetReflection());

        instanceofSnippets = new InstanceOfSnippets.Templates(options, providers, target);
        newObjectSnippets = new NewObjectSnippets.Templates(options, providers, target);
        monitorSnippets = new MonitorSnippets.Templates(options, providers, target, GraalHotSpotVMConfig.useFastLocking);
        writeBarrierSnippets = new WriteBarrierSnippets.Templates(options, providers, target, GraalHotSpotVMConfig.useCompressedOops ? GraalHotSpotVMConfig.oopEncoding : null);
        exceptionObjectSnippets = new LoadExceptionObjectSnippets.Templates(options, providers, target);
        unsafeLoadSnippets = new UnsafeLoadSnippets.Templates(options, providers, target);
        arraycopySnippets = new ArrayCopySnippets.Templates(options, providers, target);
        stringToBytesSnippets = new StringToBytesSnippets.Templates(options, providers, target);
        hashCodeSnippets = new HashCodeSnippets.Templates(options, providers, target);
        resolveConstantSnippets = new ResolveConstantSnippets.Templates(options, providers, target);
    }

    public MonitorSnippets.Templates getMonitorSnippets()
    {
        return monitorSnippets;
    }

    @Override
    public void lower(Node n, LoweringTool tool)
    {
        StructuredGraph graph = (StructuredGraph) n.graph();
        if (n instanceof Invoke)
        {
            lowerInvoke((Invoke) n, tool, graph);
        }
        else if (n instanceof LoadMethodNode)
        {
            lowerLoadMethodNode((LoadMethodNode) n);
        }
        else if (n instanceof GetClassNode)
        {
            lowerGetClassNode((GetClassNode) n, tool, graph);
        }
        else if (n instanceof StoreHubNode)
        {
            lowerStoreHubNode((StoreHubNode) n, graph);
        }
        else if (n instanceof OSRStartNode)
        {
            lowerOSRStartNode((OSRStartNode) n);
        }
        else if (n instanceof BytecodeExceptionNode)
        {
            lowerBytecodeExceptionNode((BytecodeExceptionNode) n);
        }
        else if (n instanceof InstanceOfNode)
        {
            InstanceOfNode instanceOfNode = (InstanceOfNode) n;
            if (graph.getGuardsStage().areDeoptsFixed())
            {
                instanceofSnippets.lower(instanceOfNode, tool);
            }
            else
            {
                if (instanceOfNode.allowsNull())
                {
                    ValueNode object = instanceOfNode.getValue();
                    LogicNode newTypeCheck = graph.addOrUniqueWithInputs(InstanceOfNode.create(instanceOfNode.type(), object, instanceOfNode.profile(), instanceOfNode.getAnchor()));
                    LogicNode newNode = LogicNode.or(graph.unique(IsNullNode.create(object)), newTypeCheck, GraalDirectives.UNLIKELY_PROBABILITY);
                    instanceOfNode.replaceAndDelete(newNode);
                }
            }
        }
        else if (n instanceof InstanceOfDynamicNode)
        {
            InstanceOfDynamicNode instanceOfDynamicNode = (InstanceOfDynamicNode) n;
            if (graph.getGuardsStage().areDeoptsFixed())
            {
                instanceofSnippets.lower(instanceOfDynamicNode, tool);
            }
            else
            {
                ValueNode mirror = instanceOfDynamicNode.getMirrorOrHub();
                if (mirror.stamp(NodeView.DEFAULT).getStackKind() == JavaKind.Object)
                {
                    ClassGetHubNode classGetHub = graph.unique(new ClassGetHubNode(mirror));
                    instanceOfDynamicNode.setMirror(classGetHub);
                }

                if (instanceOfDynamicNode.allowsNull())
                {
                    ValueNode object = instanceOfDynamicNode.getObject();
                    LogicNode newTypeCheck = graph.addOrUniqueWithInputs(InstanceOfDynamicNode.create(graph.getAssumptions(), tool.getConstantReflection(), instanceOfDynamicNode.getMirrorOrHub(), object, false));
                    LogicNode newNode = LogicNode.or(graph.unique(IsNullNode.create(object)), newTypeCheck, GraalDirectives.UNLIKELY_PROBABILITY);
                    instanceOfDynamicNode.replaceAndDelete(newNode);
                }
            }
        }
        else if (n instanceof ClassIsAssignableFromNode)
        {
            if (graph.getGuardsStage().areDeoptsFixed())
            {
                instanceofSnippets.lower((ClassIsAssignableFromNode) n, tool);
            }
        }
        else if (n instanceof NewInstanceNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower((NewInstanceNode) n, registers, tool);
            }
        }
        else if (n instanceof DynamicNewInstanceNode)
        {
            DynamicNewInstanceNode newInstanceNode = (DynamicNewInstanceNode) n;
            if (newInstanceNode.getClassClass() == null)
            {
                JavaConstant classClassMirror = constantReflection.forObject(Class.class);
                ConstantNode classClass = ConstantNode.forConstant(classClassMirror, tool.getMetaAccess(), graph);
                newInstanceNode.setClassClass(classClass);
            }
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower(newInstanceNode, registers, tool);
            }
        }
        else if (n instanceof NewArrayNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower((NewArrayNode) n, registers, tool);
            }
        }
        else if (n instanceof DynamicNewArrayNode)
        {
            DynamicNewArrayNode dynamicNewArrayNode = (DynamicNewArrayNode) n;
            if (dynamicNewArrayNode.getVoidClass() == null)
            {
                JavaConstant voidClassMirror = constantReflection.forObject(void.class);
                ConstantNode voidClass = ConstantNode.forConstant(voidClassMirror, tool.getMetaAccess(), graph);
                dynamicNewArrayNode.setVoidClass(voidClass);
            }
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower(dynamicNewArrayNode, registers, tool);
            }
        }
        else if (n instanceof RawMonitorEnterNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                monitorSnippets.lower((RawMonitorEnterNode) n, registers, tool);
            }
        }
        else if (n instanceof MonitorExitNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                monitorSnippets.lower((MonitorExitNode) n, registers, tool);
            }
        }
        else if (n instanceof ArrayCopyNode)
        {
            arraycopySnippets.lower((ArrayCopyNode) n, tool);
        }
        else if (n instanceof ArrayCopyWithSlowPathNode)
        {
            arraycopySnippets.lower((ArrayCopyWithSlowPathNode) n, tool);
        }
        else if (n instanceof G1PreWriteBarrier)
        {
            writeBarrierSnippets.lower((G1PreWriteBarrier) n, registers, tool);
        }
        else if (n instanceof G1PostWriteBarrier)
        {
            writeBarrierSnippets.lower((G1PostWriteBarrier) n, registers, tool);
        }
        else if (n instanceof G1ReferentFieldReadBarrier)
        {
            writeBarrierSnippets.lower((G1ReferentFieldReadBarrier) n, registers, tool);
        }
        else if (n instanceof SerialWriteBarrier)
        {
            writeBarrierSnippets.lower((SerialWriteBarrier) n, tool);
        }
        else if (n instanceof SerialArrayRangeWriteBarrier)
        {
            writeBarrierSnippets.lower((SerialArrayRangeWriteBarrier) n, tool);
        }
        else if (n instanceof G1ArrayRangePreWriteBarrier)
        {
            writeBarrierSnippets.lower((G1ArrayRangePreWriteBarrier) n, registers, tool);
        }
        else if (n instanceof G1ArrayRangePostWriteBarrier)
        {
            writeBarrierSnippets.lower((G1ArrayRangePostWriteBarrier) n, registers, tool);
        }
        else if (n instanceof NewMultiArrayNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                newObjectSnippets.lower((NewMultiArrayNode) n, tool);
            }
        }
        else if (n instanceof LoadExceptionObjectNode)
        {
            exceptionObjectSnippets.lower((LoadExceptionObjectNode) n, registers, tool);
        }
        else if (n instanceof StringToBytesNode)
        {
            if (graph.getGuardsStage().areDeoptsFixed())
            {
                stringToBytesSnippets.lower((StringToBytesNode) n, tool);
            }
        }
        else if (n instanceof IntegerDivRemNode)
        {
            // Nothing to do for division nodes. The HotSpot signal handler catches divisions by zero and the MIN_VALUE / -1 cases.
        }
        else if (n instanceof AbstractDeoptimizeNode || n instanceof UnwindNode || n instanceof RemNode || n instanceof SafepointNode)
        {
            // No lowering, we generate LIR directly for these nodes.
        }
        else if (n instanceof ClassGetHubNode)
        {
            lowerClassGetHubNode((ClassGetHubNode) n, tool);
        }
        else if (n instanceof HubGetClassNode)
        {
            lowerHubGetClassNode((HubGetClassNode) n, tool);
        }
        else if (n instanceof KlassLayoutHelperNode)
        {
            lowerKlassLayoutHelperNode((KlassLayoutHelperNode) n, tool);
        }
        else if (n instanceof ComputeObjectAddressNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                lowerComputeObjectAddressNode((ComputeObjectAddressNode) n);
            }
        }
        else if (n instanceof IdentityHashCodeNode)
        {
            hashCodeSnippets.lower((IdentityHashCodeNode) n, tool);
        }
        else if (n instanceof ResolveDynamicConstantNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((ResolveDynamicConstantNode) n, tool);
            }
        }
        else if (n instanceof ResolveConstantNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((ResolveConstantNode) n, tool);
            }
        }
        else if (n instanceof ResolveMethodAndLoadCountersNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((ResolveMethodAndLoadCountersNode) n, tool);
            }
        }
        else if (n instanceof InitializeKlassNode)
        {
            if (graph.getGuardsStage().areFrameStatesAtDeopts())
            {
                resolveConstantSnippets.lower((InitializeKlassNode) n, tool);
            }
        }
        else
        {
            super.lower(n, tool);
        }
    }

    private static void lowerComputeObjectAddressNode(ComputeObjectAddressNode n)
    {
        /*
         * Lower the node into a ComputeObjectAddress node and an Add but ensure that it's below any
         * potential safepoints and above it's uses.
         */
        for (Node use : n.usages().snapshot())
        {
            if (use instanceof FixedNode)
            {
                FixedNode fixed = (FixedNode) use;
                StructuredGraph graph = n.graph();
                GetObjectAddressNode address = graph.add(new GetObjectAddressNode(n.getObject()));
                graph.addBeforeFixed(fixed, address);
                AddNode add = graph.addOrUnique(new AddNode(address, n.getOffset()));
                use.replaceFirstInput(n, add);
            }
            else
            {
                throw GraalError.shouldNotReachHere("Unexpected floating use of ComputeObjectAddressNode " + n);
            }
        }
        GraphUtil.unlinkFixedNode(n);
        n.safeDelete();
    }

    private void lowerKlassLayoutHelperNode(KlassLayoutHelperNode n, LoweringTool tool)
    {
        if (tool.getLoweringStage() == LoweringTool.StandardLoweringStage.HIGH_TIER)
        {
            return;
        }
        StructuredGraph graph = n.graph();
        AddressNode address = createOffsetAddress(graph, n.getHub(), GraalHotSpotVMConfig.klassLayoutHelperOffset);
        n.replaceAtUsagesAndDelete(graph.unique(new FloatingReadNode(address, HotSpotReplacementsUtil.KLASS_LAYOUT_HELPER_LOCATION, null, n.stamp(NodeView.DEFAULT), null, BarrierType.NONE)));
    }

    private void lowerHubGetClassNode(HubGetClassNode n, LoweringTool tool)
    {
        if (tool.getLoweringStage() == LoweringTool.StandardLoweringStage.HIGH_TIER)
        {
            return;
        }

        StructuredGraph graph = n.graph();
        AddressNode address = createOffsetAddress(graph, n.getHub(), GraalHotSpotVMConfig.classMirrorOffset);
        FloatingReadNode read = graph.unique(new FloatingReadNode(address, HotSpotReplacementsUtil.CLASS_MIRROR_LOCATION, null, StampFactory.forKind(target.wordJavaKind), null, BarrierType.NONE));
        address = createOffsetAddress(graph, read, 0);
        read = graph.unique(new FloatingReadNode(address, HotSpotReplacementsUtil.CLASS_MIRROR_HANDLE_LOCATION, null, n.stamp(NodeView.DEFAULT), null, BarrierType.NONE));
        n.replaceAtUsagesAndDelete(read);
    }

    private void lowerClassGetHubNode(ClassGetHubNode n, LoweringTool tool)
    {
        if (tool.getLoweringStage() == LoweringTool.StandardLoweringStage.HIGH_TIER)
        {
            return;
        }

        StructuredGraph graph = n.graph();
        AddressNode address = createOffsetAddress(graph, n.getValue(), GraalHotSpotVMConfig.klassOffset);
        FloatingReadNode read = graph.unique(new FloatingReadNode(address, HotSpotReplacementsUtil.CLASS_KLASS_LOCATION, null, n.stamp(NodeView.DEFAULT), null, BarrierType.NONE));
        n.replaceAtUsagesAndDelete(read);
    }

    private void lowerInvoke(Invoke invoke, LoweringTool tool, StructuredGraph graph)
    {
        if (invoke.callTarget() instanceof MethodCallTargetNode)
        {
            MethodCallTargetNode callTarget = (MethodCallTargetNode) invoke.callTarget();
            NodeInputList<ValueNode> parameters = callTarget.arguments();
            ValueNode receiver = parameters.size() <= 0 ? null : parameters.get(0);
            if (!callTarget.isStatic() && receiver.stamp(NodeView.DEFAULT) instanceof ObjectStamp && !StampTool.isPointerNonNull(receiver))
            {
                ValueNode nonNullReceiver = createNullCheckedValue(receiver, invoke.asNode(), tool);
                parameters.set(0, nonNullReceiver);
                receiver = nonNullReceiver;
            }
            JavaType[] signature = callTarget.targetMethod().getSignature().toParameterTypes(callTarget.isStatic() ? null : callTarget.targetMethod().getDeclaringClass());

            LoweredCallTargetNode loweredCallTarget = null;
            OptionValues options = graph.getOptions();
            if (GraalOptions.InlineVTableStubs.getValue(options) && callTarget.invokeKind().isIndirect() && (GraalOptions.AlwaysInlineVTableStubs.getValue(options) || invoke.isPolymorphic()))
            {
                HotSpotResolvedJavaMethod hsMethod = (HotSpotResolvedJavaMethod) callTarget.targetMethod();
                ResolvedJavaType receiverType = invoke.getReceiverType();
                if (hsMethod.isInVirtualMethodTable(receiverType))
                {
                    JavaKind wordKind = runtime.getTarget().wordJavaKind;
                    ValueNode hub = createReadHub(graph, receiver, tool);

                    ReadNode metaspaceMethod = createReadVirtualMethod(graph, hub, hsMethod, receiverType);
                    // We use LocationNode.ANY_LOCATION for the reads that access the compiled
                    // code entry as HotSpot does not guarantee they are final values.
                    AddressNode address = createOffsetAddress(graph, metaspaceMethod, GraalHotSpotVMConfig.methodCompiledEntryOffset);
                    ReadNode compiledEntry = graph.add(new ReadNode(address, LocationIdentity.any(), StampFactory.forKind(wordKind), BarrierType.NONE));

                    loweredCallTarget = graph.add(new HotSpotIndirectCallTargetNode(metaspaceMethod, compiledEntry, parameters.toArray(new ValueNode[parameters.size()]), callTarget.returnStamp(), signature, callTarget.targetMethod(), HotSpotCallingConventionType.JavaCall, callTarget.invokeKind()));

                    graph.addBeforeFixed(invoke.asNode(), metaspaceMethod);
                    graph.addAfterFixed(metaspaceMethod, compiledEntry);
                }
            }

            if (loweredCallTarget == null)
            {
                loweredCallTarget = graph.add(new HotSpotDirectCallTargetNode(parameters.toArray(new ValueNode[parameters.size()]), callTarget.returnStamp(), signature, callTarget.targetMethod(), HotSpotCallingConventionType.JavaCall, callTarget.invokeKind()));
            }
            callTarget.replaceAndDelete(loweredCallTarget);
        }
    }

    @Override
    protected Stamp loadCompressedStamp(ObjectStamp stamp)
    {
        return HotSpotNarrowOopStamp.compressed(stamp, GraalHotSpotVMConfig.oopEncoding);
    }

    @Override
    protected ValueNode newCompressionNode(CompressionOp op, ValueNode value)
    {
        return new HotSpotCompressionNode(op, value, GraalHotSpotVMConfig.oopEncoding);
    }

    @Override
    public ValueNode staticFieldBase(StructuredGraph graph, ResolvedJavaField f)
    {
        HotSpotResolvedJavaField field = (HotSpotResolvedJavaField) f;
        JavaConstant base = constantReflection.asJavaClass(field.getDeclaringClass());
        return ConstantNode.forConstant(base, metaAccess, graph);
    }

    @Override
    protected ValueNode createReadArrayComponentHub(StructuredGraph graph, ValueNode arrayHub, FixedNode anchor)
    {
        /*
         * Anchor the read of the element klass to the cfg, because it is only valid when arrayClass
         * is an object class, which might not be the case in other parts of the compiled method.
         */
        AddressNode address = createOffsetAddress(graph, arrayHub, GraalHotSpotVMConfig.arrayClassElementOffset);
        return graph.unique(new FloatingReadNode(address, HotSpotReplacementsUtil.OBJ_ARRAY_KLASS_ELEMENT_KLASS_LOCATION, null, KlassPointerStamp.klassNonNull(), AbstractBeginNode.prevBegin(anchor)));
    }

    @Override
    protected void lowerUnsafeLoadNode(RawLoadNode load, LoweringTool tool)
    {
        StructuredGraph graph = load.graph();
        if (!(load instanceof GuardedUnsafeLoadNode) && !graph.getGuardsStage().allowsFloatingGuards() && addReadBarrier(load))
        {
            unsafeLoadSnippets.lower(load, tool);
        }
        else
        {
            super.lowerUnsafeLoadNode(load, tool);
        }
    }

    private void lowerLoadMethodNode(LoadMethodNode loadMethodNode)
    {
        StructuredGraph graph = loadMethodNode.graph();
        HotSpotResolvedJavaMethod method = (HotSpotResolvedJavaMethod) loadMethodNode.getMethod();
        ReadNode metaspaceMethod = createReadVirtualMethod(graph, loadMethodNode.getHub(), method, loadMethodNode.getReceiverType());
        graph.replaceFixed(loadMethodNode, metaspaceMethod);
    }

    private static void lowerGetClassNode(GetClassNode getClass, LoweringTool tool, StructuredGraph graph)
    {
        StampProvider stampProvider = tool.getStampProvider();
        LoadHubNode hub = graph.unique(new LoadHubNode(stampProvider, getClass.getObject()));
        HubGetClassNode hubGetClass = graph.unique(new HubGetClassNode(tool.getMetaAccess(), hub));
        getClass.replaceAtUsagesAndDelete(hubGetClass);
        hub.lower(tool);
        hubGetClass.lower(tool);
    }

    private void lowerStoreHubNode(StoreHubNode storeHub, StructuredGraph graph)
    {
        WriteNode hub = createWriteHub(graph, storeHub.getObject(), storeHub.getValue());
        graph.replaceFixed(storeHub, hub);
    }

    @Override
    public BarrierType fieldInitializationBarrier(JavaKind entryKind)
    {
        return (entryKind == JavaKind.Object && !GraalHotSpotVMConfig.useDeferredInitBarriers) ? BarrierType.IMPRECISE : BarrierType.NONE;
    }

    @Override
    public BarrierType arrayInitializationBarrier(JavaKind entryKind)
    {
        return (entryKind == JavaKind.Object && !GraalHotSpotVMConfig.useDeferredInitBarriers) ? BarrierType.PRECISE : BarrierType.NONE;
    }

    private void lowerOSRStartNode(OSRStartNode osrStart)
    {
        StructuredGraph graph = osrStart.graph();
        if (graph.getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS)
        {
            StartNode newStart = graph.add(new StartNode());
            ParameterNode buffer = graph.addWithoutUnique(new ParameterNode(0, StampPair.createSingle(StampFactory.forKind(runtime.getTarget().wordJavaKind))));
            ForeignCallNode migrationEnd = graph.add(new ForeignCallNode(foreignCalls, HotSpotForeignCallsProviderImpl.OSR_MIGRATION_END, buffer));
            migrationEnd.setStateAfter(osrStart.stateAfter());
            newStart.setNext(migrationEnd);
            FixedNode next = osrStart.next();
            osrStart.setNext(null);
            migrationEnd.setNext(next);
            graph.setStart(newStart);

            final int wordSize = target.wordSize;

            // taken from c2 locals_addr = osr_buf + (max_locals-1)*wordSize)
            int localsOffset = (graph.method().getMaxLocals() - 1) * wordSize;
            for (OSRLocalNode osrLocal : graph.getNodes(OSRLocalNode.TYPE))
            {
                int size = osrLocal.getStackKind().getSlotCount();
                int offset = localsOffset - (osrLocal.index() + size - 1) * wordSize;
                AddressNode address = createOffsetAddress(graph, buffer, offset);
                ReadNode load = graph.add(new ReadNode(address, LocationIdentity.any(), osrLocal.stamp(NodeView.DEFAULT), BarrierType.NONE));
                osrLocal.replaceAndDelete(load);
                graph.addBeforeFixed(migrationEnd, load);
            }

            // taken from c2 monitors_addr = osr_buf + (max_locals+mcnt*2-1)*wordSize)
            final int lockCount = osrStart.stateAfter().locksSize();
            final int locksOffset = (graph.method().getMaxLocals() + lockCount * 2 - 1) * wordSize;

            // first initialize the lock slots for all enters with the displaced marks read from the buffer
            for (OSRMonitorEnterNode osrMonitorEnter : graph.getNodes(OSRMonitorEnterNode.TYPE))
            {
                MonitorIdNode monitorID = osrMonitorEnter.getMonitorId();
                OSRLockNode lock = (OSRLockNode) osrMonitorEnter.object();
                final int index = lock.index();

                final int offsetDisplacedHeader = locksOffset - ((index * 2) + 1) * wordSize;
                final int offsetLockObject = locksOffset - index * 2 * wordSize;

                // load the displaced mark from the osr buffer
                AddressNode addressDisplacedHeader = createOffsetAddress(graph, buffer, offsetDisplacedHeader);
                ReadNode loadDisplacedHeader = graph.add(new ReadNode(addressDisplacedHeader, LocationIdentity.any(), lock.stamp(NodeView.DEFAULT), BarrierType.NONE));
                graph.addBeforeFixed(migrationEnd, loadDisplacedHeader);

                // we need to initialize the stack slot for the lock
                BeginLockScopeNode beginLockScope = graph.add(new BeginLockScopeNode(lock.getStackKind(), monitorID.getLockDepth()));
                graph.addBeforeFixed(migrationEnd, beginLockScope);

                // write the displaced mark to the correct stack slot
                AddressNode addressDisplacedMark = createOffsetAddress(graph, beginLockScope, GraalHotSpotVMConfig.lockDisplacedMarkOffset);
                WriteNode writeStackSlot = graph.add(new WriteNode(addressDisplacedMark, HotSpotReplacementsUtil.DISPLACED_MARK_WORD_LOCATION, loadDisplacedHeader, BarrierType.NONE));
                graph.addBeforeFixed(migrationEnd, writeStackSlot);

                // load the lock object from the osr buffer
                AddressNode addressLockObject = createOffsetAddress(graph, buffer, offsetLockObject);
                ReadNode loadObject = graph.add(new ReadNode(addressLockObject, LocationIdentity.any(), lock.stamp(NodeView.DEFAULT), BarrierType.NONE));
                lock.replaceAndDelete(loadObject);
                graph.addBeforeFixed(migrationEnd, loadObject);
            }

            osrStart.replaceAtUsagesAndDelete(newStart);
        }
    }

    static final class Exceptions
    {
        protected static final ArrayIndexOutOfBoundsException cachedArrayIndexOutOfBoundsException;
        protected static final NullPointerException cachedNullPointerException;

        static
        {
            cachedArrayIndexOutOfBoundsException = new ArrayIndexOutOfBoundsException();
            cachedArrayIndexOutOfBoundsException.setStackTrace(new StackTraceElement[0]);
            cachedNullPointerException = new NullPointerException();
            cachedNullPointerException.setStackTrace(new StackTraceElement[0]);
        }
    }

    public static final class RuntimeCalls
    {
        public static final ForeignCallDescriptor CREATE_ARRAY_STORE_EXCEPTION = new ForeignCallDescriptor("createArrayStoreException", ArrayStoreException.class, Object.class);
        public static final ForeignCallDescriptor CREATE_CLASS_CAST_EXCEPTION = new ForeignCallDescriptor("createClassCastException", ClassCastException.class, Object.class, KlassPointer.class);
        public static final ForeignCallDescriptor CREATE_NULL_POINTER_EXCEPTION = new ForeignCallDescriptor("createNullPointerException", NullPointerException.class);
        public static final ForeignCallDescriptor CREATE_OUT_OF_BOUNDS_EXCEPTION = new ForeignCallDescriptor("createOutOfBoundsException", ArrayIndexOutOfBoundsException.class, int.class);
    }

    private boolean throwCachedException(BytecodeExceptionNode node)
    {
        Throwable exception;
        if (node.getExceptionClass() == NullPointerException.class)
        {
            exception = Exceptions.cachedNullPointerException;
        }
        else if (node.getExceptionClass() == ArrayIndexOutOfBoundsException.class)
        {
            exception = Exceptions.cachedArrayIndexOutOfBoundsException;
        }
        else
        {
            return false;
        }

        StructuredGraph graph = node.graph();
        FloatingNode exceptionNode = ConstantNode.forConstant(constantReflection.forObject(exception), metaAccess, graph);
        graph.replaceFixedWithFloating(node, exceptionNode);
        return true;
    }

    private void lowerBytecodeExceptionNode(BytecodeExceptionNode node)
    {
        if (GraalOptions.OmitHotExceptionStacktrace.getValue(node.getOptions()))
        {
            if (throwCachedException(node))
            {
                return;
            }
        }

        ForeignCallDescriptor descriptor;
        if (node.getExceptionClass() == NullPointerException.class)
        {
            descriptor = RuntimeCalls.CREATE_NULL_POINTER_EXCEPTION;
        }
        else if (node.getExceptionClass() == ArrayIndexOutOfBoundsException.class)
        {
            descriptor = RuntimeCalls.CREATE_OUT_OF_BOUNDS_EXCEPTION;
        }
        else if (node.getExceptionClass() == ArrayStoreException.class)
        {
            descriptor = RuntimeCalls.CREATE_ARRAY_STORE_EXCEPTION;
        }
        else if (node.getExceptionClass() == ClassCastException.class)
        {
            descriptor = RuntimeCalls.CREATE_CLASS_CAST_EXCEPTION;
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }

        StructuredGraph graph = node.graph();
        ForeignCallNode foreignCallNode = graph.add(new ForeignCallNode(foreignCalls, descriptor, node.stamp(NodeView.DEFAULT), node.getArguments()));
        graph.replaceFixedWithFixed(node, foreignCallNode);
    }

    private boolean addReadBarrier(RawLoadNode load)
    {
        if (GraalHotSpotVMConfig.useG1GC && load.graph().getGuardsStage() == StructuredGraph.GuardsStage.FIXED_DEOPTS && load.object().getStackKind() == JavaKind.Object && load.accessKind() == JavaKind.Object && !StampTool.isPointerAlwaysNull(load.object()))
        {
            ResolvedJavaType type = StampTool.typeOrNull(load.object());
            if (type != null && !type.isArray())
            {
                return true;
            }
        }
        return false;
    }

    private ReadNode createReadVirtualMethod(StructuredGraph graph, ValueNode hub, HotSpotResolvedJavaMethod method, ResolvedJavaType receiverType)
    {
        return createReadVirtualMethod(graph, hub, method.vtableEntryOffset(receiverType));
    }

    private ReadNode createReadVirtualMethod(StructuredGraph graph, ValueNode hub, int vtableEntryOffset)
    {
        // We use LocationNode.ANY_LOCATION for the reads that access the vtable
        // entry as HotSpot does not guarantee that this is a final value.
        Stamp methodStamp = MethodPointerStamp.methodNonNull();
        AddressNode address = createOffsetAddress(graph, hub, vtableEntryOffset);
        return graph.add(new ReadNode(address, LocationIdentity.any(), methodStamp, BarrierType.NONE));
    }

    @Override
    protected ValueNode createReadHub(StructuredGraph graph, ValueNode object, LoweringTool tool)
    {
        if (tool.getLoweringStage() != LoweringTool.StandardLoweringStage.LOW_TIER)
        {
            return graph.unique(new LoadHubNode(tool.getStampProvider(), object));
        }

        KlassPointerStamp hubStamp = KlassPointerStamp.klassNonNull();
        if (GraalHotSpotVMConfig.useCompressedClassPointers)
        {
            hubStamp = hubStamp.compressed(GraalHotSpotVMConfig.klassEncoding);
        }

        AddressNode address = createOffsetAddress(graph, object, GraalHotSpotVMConfig.hubOffset);
        LocationIdentity hubLocation = GraalHotSpotVMConfig.useCompressedClassPointers ? HotSpotReplacementsUtil.COMPRESSED_HUB_LOCATION : HotSpotReplacementsUtil.HUB_LOCATION;
        FloatingReadNode memoryRead = graph.unique(new FloatingReadNode(address, hubLocation, null, hubStamp, null, BarrierType.NONE));
        if (GraalHotSpotVMConfig.useCompressedClassPointers)
        {
            return HotSpotCompressionNode.uncompress(memoryRead, GraalHotSpotVMConfig.klassEncoding);
        }
        else
        {
            return memoryRead;
        }
    }

    private WriteNode createWriteHub(StructuredGraph graph, ValueNode object, ValueNode value)
    {
        ValueNode writeValue = value;
        if (GraalHotSpotVMConfig.useCompressedClassPointers)
        {
            writeValue = HotSpotCompressionNode.compress(value, GraalHotSpotVMConfig.klassEncoding);
        }

        AddressNode address = createOffsetAddress(graph, object, GraalHotSpotVMConfig.hubOffset);
        return graph.add(new WriteNode(address, HotSpotReplacementsUtil.HUB_WRITE_LOCATION, writeValue, BarrierType.NONE));
    }

    @Override
    protected BarrierType fieldLoadBarrierType(ResolvedJavaField f)
    {
        HotSpotResolvedJavaField loadField = (HotSpotResolvedJavaField) f;
        BarrierType barrierType = BarrierType.NONE;
        if (GraalHotSpotVMConfig.useG1GC && loadField.getJavaKind() == JavaKind.Object && metaAccess.lookupJavaType(Reference.class).equals(loadField.getDeclaringClass()) && loadField.getName().equals("referent"))
        {
            barrierType = BarrierType.PRECISE;
        }
        return barrierType;
    }

    @Override
    public int fieldOffset(ResolvedJavaField f)
    {
        HotSpotResolvedJavaField field = (HotSpotResolvedJavaField) f;
        return field.offset();
    }

    @Override
    public int arrayScalingFactor(JavaKind kind)
    {
        if (GraalHotSpotVMConfig.useCompressedOops && kind == JavaKind.Object)
        {
            return super.arrayScalingFactor(JavaKind.Int);
        }
        else
        {
            return super.arrayScalingFactor(kind);
        }
    }

    @Override
    public int arrayBaseOffset(JavaKind kind)
    {
        return HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(kind);
    }

    @Override
    protected final JavaKind getStorageKind(ResolvedJavaField field)
    {
        return field.getJavaKind();
    }
}
