package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.code.CodeUtil;
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
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.HotSpotRuntime;
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

// @NodeInfo.allowedUsageTypes "Memory"
// @class ArrayCopyCallNode
public final class ArrayCopyCallNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single, MemoryAccess, Canonicalizable
{
    // @def
    public static final NodeClass<ArrayCopyCallNode> TYPE = NodeClass.create(ArrayCopyCallNode.class);

    @Input
    // @field
    protected ValueNode src;
    @Input
    // @field
    protected ValueNode srcPos;
    @Input
    // @field
    protected ValueNode dest;
    @Input
    // @field
    protected ValueNode destPos;
    @Input
    // @field
    protected ValueNode length;

    @OptionalInput(InputType.Memory)
    // @field
    MemoryNode lastLocationAccess;

    // @field
    protected final JavaKind elementKind;
    // @field
    protected final LocationIdentity locationIdentity;

    /**
     * Aligned means that the offset of the copy is heap word aligned.
     */
    // @field
    protected boolean aligned;
    // @field
    protected boolean disjoint;
    // @field
    protected boolean uninitialized;

    // @field
    protected final HotSpotGraalRuntime runtime;

    // @cons
    public ArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind, boolean __aligned, boolean __disjoint, boolean __uninitialized)
    {
        this(__runtime, __src, __srcPos, __dest, __destPos, __length, __elementKind, null, __aligned, __disjoint, __uninitialized);
    }

    // @cons
    public ArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind, boolean __disjoint)
    {
        this(__runtime, __src, __srcPos, __dest, __destPos, __length, __elementKind, null, false, __disjoint, false);
    }

    // @cons
    protected ArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind, LocationIdentity __locationIdentity, boolean __aligned, boolean __disjoint, boolean __uninitialized)
    {
        super(TYPE, StampFactory.forVoid());
        this.src = __src;
        this.srcPos = __srcPos;
        this.dest = __dest;
        this.destPos = __destPos;
        this.length = __length;
        this.elementKind = __elementKind;
        this.locationIdentity = (__locationIdentity != null ? __locationIdentity : NamedLocationIdentity.getArrayLocation(__elementKind));
        this.aligned = __aligned;
        this.disjoint = __disjoint;
        this.uninitialized = __uninitialized;
        this.runtime = __runtime;
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

    private ValueNode computeBase(ValueNode __base, ValueNode __pos)
    {
        FixedWithNextNode __basePtr = graph().add(new GetObjectAddressNode(__base));
        graph().addBeforeFixed(this, __basePtr);
        Stamp __wordStamp = StampFactory.forKind(runtime.getTarget().wordJavaKind);
        ValueNode __wordPos = IntegerConvertNode.convert(__pos, __wordStamp, graph(), NodeView.DEFAULT);
        int __shift = CodeUtil.log2(HotSpotRuntime.getArrayIndexScale(elementKind));
        ValueNode __scaledIndex = graph().unique(new LeftShiftNode(__wordPos, ConstantNode.forInt(__shift, graph())));
        ValueNode __offset = graph().unique(new AddNode(__scaledIndex, ConstantNode.forIntegerStamp(__wordStamp, HotSpotRuntime.getArrayBaseOffset(elementKind), graph())));
        return graph().unique(new OffsetAddressNode(__basePtr, __offset));
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            updateAlignedDisjoint();
            ForeignCallDescriptor __desc = HotSpotHostForeignCallsProvider.lookupArraycopyDescriptor(elementKind, isAligned(), isDisjoint(), isUninitialized(), locationIdentity.equals(LocationIdentity.any()));
            StructuredGraph __graph = graph();
            ValueNode __srcAddr = computeBase(getSource(), getSourcePosition());
            ValueNode __destAddr = computeBase(getDestination(), getDestinationPosition());
            ValueNode __len = getLength();
            if (__len.stamp(NodeView.DEFAULT).getStackKind() != JavaKind.Long)
            {
                __len = IntegerConvertNode.convert(__len, StampFactory.forKind(JavaKind.Long), graph(), NodeView.DEFAULT);
            }
            ForeignCallNode __call = __graph.add(new ForeignCallNode(runtime.getBackend().getForeignCalls(), __desc, __srcAddr, __destAddr, __len));
            __call.setStateAfter(stateAfter());
            __graph.replaceFixedWithFixed(this, __call);
        }
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __lla)
    {
        updateUsagesInterface(lastLocationAccess, __lla);
        lastLocationAccess = __lla;
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

    public static void arraycopyObjectKillsAny(Object __src, int __srcPos, Object __dest, int __destPos, int __length)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, JavaKind.Object, LocationIdentity.any(), false, false, false);
    }

    public static void arraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantNodeParameter JavaKind __elementKind)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, __elementKind, false, false, false);
    }

    public static void disjointArraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantNodeParameter JavaKind __elementKind)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, __elementKind, false, true, false);
    }

    public static void disjointUninitializedArraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @ConstantNodeParameter JavaKind __elementKind)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, __elementKind, false, true, true);
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

    boolean isHeapWordAligned(JavaConstant __value, JavaKind __kind)
    {
        return (HotSpotRuntime.getArrayBaseOffset(__kind) + (long) __value.asInt() * HotSpotRuntime.getArrayIndexScale(__kind)) % HotSpotRuntime.heapWordSize == 0;
    }

    public void updateAlignedDisjoint()
    {
        JavaKind __componentKind = elementKind;
        if (srcPos == destPos)
        {
            // can treat as disjoint
            disjoint = true;
        }
        PrimitiveConstant __constantSrc = (PrimitiveConstant) srcPos.stamp(NodeView.DEFAULT).asConstant();
        PrimitiveConstant __constantDst = (PrimitiveConstant) destPos.stamp(NodeView.DEFAULT).asConstant();
        if (__constantSrc != null && __constantDst != null)
        {
            if (!aligned)
            {
                aligned = isHeapWordAligned(__constantSrc, __componentKind) && isHeapWordAligned(__constantDst, __componentKind);
            }
            if (__constantSrc.asInt() >= __constantDst.asInt())
            {
                // low to high copy so treat as disjoint
                disjoint = true;
            }
        }
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
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
