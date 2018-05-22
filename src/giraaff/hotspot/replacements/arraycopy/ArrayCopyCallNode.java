package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.hotspot.HotSpotJVMCIRuntimeProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PrimitiveConstant;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.hotspot.nodes.GetObjectAddressNode;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryAccess;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.MemoryNode;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

public final class ArrayCopyCallNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single, MemoryAccess, Canonicalizable
{
    public static final NodeClass<ArrayCopyCallNode> TYPE = NodeClass.create(ArrayCopyCallNode.class);
    @Input protected ValueNode src;
    @Input protected ValueNode srcPos;
    @Input protected ValueNode dest;
    @Input protected ValueNode destPos;
    @Input protected ValueNode length;

    @OptionalInput(InputType.Memory) MemoryNode lastLocationAccess;

    protected final JavaKind elementKind;
    protected final LocationIdentity locationIdentity;

    /**
     * Aligned means that the offset of the copy is heap word aligned.
     */
    protected boolean aligned;
    protected boolean disjoint;
    protected boolean uninitialized;

    protected final HotSpotGraalRuntimeProvider runtime;

    public ArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntimeProvider runtime, ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length, JavaKind elementKind, boolean aligned, boolean disjoint, boolean uninitialized)
    {
        this(runtime, src, srcPos, dest, destPos, length, elementKind, null, aligned, disjoint, uninitialized);
    }

    public ArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntimeProvider runtime, ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length, JavaKind elementKind, boolean disjoint)
    {
        this(runtime, src, srcPos, dest, destPos, length, elementKind, null, false, disjoint, false);
    }

    protected ArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntimeProvider runtime, ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length, JavaKind elementKind, LocationIdentity locationIdentity, boolean aligned, boolean disjoint, boolean uninitialized)
    {
        super(TYPE, StampFactory.forVoid());
        this.src = src;
        this.srcPos = srcPos;
        this.dest = dest;
        this.destPos = destPos;
        this.length = length;
        this.elementKind = elementKind;
        this.locationIdentity = (locationIdentity != null ? locationIdentity : NamedLocationIdentity.getArrayLocation(elementKind));
        this.aligned = aligned;
        this.disjoint = disjoint;
        this.uninitialized = uninitialized;
        this.runtime = runtime;
    }

    public ValueNode getSource()
    {
        return src;
    }

    public ValueNode getSourcePosition()
    {
        return srcPos;
    }

    public ValueNode getDestination()
    {
        return dest;
    }

    public ValueNode getDestinationPosition()
    {
        return destPos;
    }

    public ValueNode getLength()
    {
        return length;
    }

    public JavaKind getElementKind()
    {
        return elementKind;
    }

    private ValueNode computeBase(ValueNode base, ValueNode pos)
    {
        FixedWithNextNode basePtr = graph().add(new GetObjectAddressNode(base));
        graph().addBeforeFixed(this, basePtr);
        Stamp wordStamp = StampFactory.forKind(runtime.getTarget().wordJavaKind);
        ValueNode wordPos = IntegerConvertNode.convert(pos, wordStamp, graph(), NodeView.DEFAULT);
        int shift = CodeUtil.log2(HotSpotJVMCIRuntimeProvider.getArrayIndexScale(elementKind));
        ValueNode scaledIndex = graph().unique(new LeftShiftNode(wordPos, ConstantNode.forInt(shift, graph())));
        ValueNode offset = graph().unique(new AddNode(scaledIndex, ConstantNode.forIntegerStamp(wordStamp, HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(elementKind), graph())));
        return graph().unique(new OffsetAddressNode(basePtr, offset));
    }

    @Override
    public void lower(LoweringTool tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            updateAlignedDisjoint();
            ForeignCallDescriptor desc = HotSpotHostForeignCallsProvider.lookupArraycopyDescriptor(elementKind, isAligned(), isDisjoint(), isUninitialized(), locationIdentity.equals(LocationIdentity.any()));
            StructuredGraph graph = graph();
            ValueNode srcAddr = computeBase(getSource(), getSourcePosition());
            ValueNode destAddr = computeBase(getDestination(), getDestinationPosition());
            ValueNode len = getLength();
            if (len.stamp(NodeView.DEFAULT).getStackKind() != JavaKind.Long)
            {
                len = IntegerConvertNode.convert(len, StampFactory.forKind(JavaKind.Long), graph(), NodeView.DEFAULT);
            }
            ForeignCallNode call = graph.add(new ForeignCallNode(runtime.getHostBackend().getForeignCalls(), desc, srcAddr, destAddr, len));
            call.setStateAfter(stateAfter());
            graph.replaceFixedWithFixed(this, call);
        }
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode lla)
    {
        updateUsagesInterface(lastLocationAccess, lla);
        lastLocationAccess = lla;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return locationIdentity;
    }

    @NodeIntrinsic
    private static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length, @ConstantNodeParameter JavaKind elementKind, @ConstantNodeParameter boolean aligned, @ConstantNodeParameter boolean disjoint, @ConstantNodeParameter boolean uninitialized);

    @NodeIntrinsic
    private static native void arraycopy(Object src, int srcPos, Object dest, int destPos, int length, @ConstantNodeParameter JavaKind elementKind, @ConstantNodeParameter LocationIdentity locationIdentity, @ConstantNodeParameter boolean aligned, @ConstantNodeParameter boolean disjoint, @ConstantNodeParameter boolean uninitialized);

    public static void arraycopyObjectKillsAny(Object src, int srcPos, Object dest, int destPos, int length)
    {
        arraycopy(src, srcPos, dest, destPos, length, JavaKind.Object, LocationIdentity.any(), false, false, false);
    }

    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length, @ConstantNodeParameter JavaKind elementKind)
    {
        arraycopy(src, srcPos, dest, destPos, length, elementKind, false, false, false);
    }

    public static void disjointArraycopy(Object src, int srcPos, Object dest, int destPos, int length, @ConstantNodeParameter JavaKind elementKind)
    {
        arraycopy(src, srcPos, dest, destPos, length, elementKind, false, true, false);
    }

    public static void disjointUninitializedArraycopy(Object src, int srcPos, Object dest, int destPos, int length, @ConstantNodeParameter JavaKind elementKind)
    {
        arraycopy(src, srcPos, dest, destPos, length, elementKind, false, true, true);
    }

    public boolean isAligned()
    {
        return aligned;
    }

    public boolean isDisjoint()
    {
        return disjoint;
    }

    public boolean isUninitialized()
    {
        return uninitialized;
    }

    boolean isHeapWordAligned(JavaConstant value, JavaKind kind)
    {
        return (HotSpotJVMCIRuntimeProvider.getArrayBaseOffset(kind) + (long) value.asInt() * HotSpotJVMCIRuntimeProvider.getArrayIndexScale(kind)) % runtime.getVMConfig().heapWordSize == 0;
    }

    public void updateAlignedDisjoint()
    {
        JavaKind componentKind = elementKind;
        if (srcPos == destPos)
        {
            // Can treat as disjoint
            disjoint = true;
        }
        PrimitiveConstant constantSrc = (PrimitiveConstant) srcPos.stamp(NodeView.DEFAULT).asConstant();
        PrimitiveConstant constantDst = (PrimitiveConstant) destPos.stamp(NodeView.DEFAULT).asConstant();
        if (constantSrc != null && constantDst != null)
        {
            if (!aligned)
            {
                aligned = isHeapWordAligned(constantSrc, componentKind) && isHeapWordAligned(constantDst, componentKind);
            }
            if (constantSrc.asInt() >= constantDst.asInt())
            {
                // low to high copy so treat as disjoint
                disjoint = true;
            }
        }
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (getLength().isConstant() && getLength().asConstant().isDefaultForKind())
        {
            if (lastLocationAccess != null)
            {
                replaceAtUsages(InputType.Memory, lastLocationAccess.asNode());
            }
            return null;
        }
        return this;
    }
}
