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

// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class ArrayCopyCallNode
public final class ArrayCopyCallNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single, MemoryAccess, Canonicalizable
{
    // @def
    public static final NodeClass<ArrayCopyCallNode> TYPE = NodeClass.create(ArrayCopyCallNode.class);

    @Node.Input
    // @field
    protected ValueNode ___src;
    @Node.Input
    // @field
    protected ValueNode ___srcPos;
    @Node.Input
    // @field
    protected ValueNode ___dest;
    @Node.Input
    // @field
    protected ValueNode ___destPos;
    @Node.Input
    // @field
    protected ValueNode ___length;

    @Node.OptionalInput(InputType.Memory)
    // @field
    MemoryNode ___lastLocationAccess;

    // @field
    protected final JavaKind ___elementKind;
    // @field
    protected final LocationIdentity ___locationIdentity;

    ///
    // Aligned means that the offset of the copy is heap word aligned.
    ///
    // @field
    protected boolean ___aligned;
    // @field
    protected boolean ___disjoint;
    // @field
    protected boolean ___uninitialized;

    // @field
    protected final HotSpotGraalRuntime ___runtime;

    // @cons ArrayCopyCallNode
    public ArrayCopyCallNode(@Node.InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind, boolean __aligned, boolean __disjoint, boolean __uninitialized)
    {
        this(__runtime, __src, __srcPos, __dest, __destPos, __length, __elementKind, null, __aligned, __disjoint, __uninitialized);
    }

    // @cons ArrayCopyCallNode
    public ArrayCopyCallNode(@Node.InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind, boolean __disjoint)
    {
        this(__runtime, __src, __srcPos, __dest, __destPos, __length, __elementKind, null, false, __disjoint, false);
    }

    // @cons ArrayCopyCallNode
    protected ArrayCopyCallNode(@Node.InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, JavaKind __elementKind, LocationIdentity __locationIdentity, boolean __aligned, boolean __disjoint, boolean __uninitialized)
    {
        super(TYPE, StampFactory.forVoid());
        this.___src = __src;
        this.___srcPos = __srcPos;
        this.___dest = __dest;
        this.___destPos = __destPos;
        this.___length = __length;
        this.___elementKind = __elementKind;
        this.___locationIdentity = (__locationIdentity != null ? __locationIdentity : NamedLocationIdentity.getArrayLocation(__elementKind));
        this.___aligned = __aligned;
        this.___disjoint = __disjoint;
        this.___uninitialized = __uninitialized;
        this.___runtime = __runtime;
    }

    public ValueNode getSource()
    {
        return this.___src;
    }

    public ValueNode getSourcePosition()
    {
        return this.___srcPos;
    }

    public ValueNode getDestination()
    {
        return this.___dest;
    }

    public ValueNode getDestinationPosition()
    {
        return this.___destPos;
    }

    public ValueNode getLength()
    {
        return this.___length;
    }

    public JavaKind getElementKind()
    {
        return this.___elementKind;
    }

    private ValueNode computeBase(ValueNode __base, ValueNode __pos)
    {
        FixedWithNextNode __basePtr = graph().add(new GetObjectAddressNode(__base));
        graph().addBeforeFixed(this, __basePtr);
        Stamp __wordStamp = StampFactory.forKind(this.___runtime.getTarget().wordJavaKind);
        ValueNode __wordPos = IntegerConvertNode.convert(__pos, __wordStamp, graph(), NodeView.DEFAULT);
        int __shift = CodeUtil.log2(HotSpotRuntime.getArrayIndexScale(this.___elementKind));
        ValueNode __scaledIndex = graph().unique(new LeftShiftNode(__wordPos, ConstantNode.forInt(__shift, graph())));
        ValueNode __offset = graph().unique(new AddNode(__scaledIndex, ConstantNode.forIntegerStamp(__wordStamp, HotSpotRuntime.getArrayBaseOffset(this.___elementKind), graph())));
        return graph().unique(new OffsetAddressNode(__basePtr, __offset));
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            updateAlignedDisjoint();
            ForeignCallDescriptor __desc = HotSpotHostForeignCallsProvider.lookupArraycopyDescriptor(this.___elementKind, isAligned(), isDisjoint(), isUninitialized(), this.___locationIdentity.equals(LocationIdentity.any()));
            StructuredGraph __graph = graph();
            ValueNode __srcAddr = computeBase(getSource(), getSourcePosition());
            ValueNode __destAddr = computeBase(getDestination(), getDestinationPosition());
            ValueNode __len = getLength();
            if (__len.stamp(NodeView.DEFAULT).getStackKind() != JavaKind.Long)
            {
                __len = IntegerConvertNode.convert(__len, StampFactory.forKind(JavaKind.Long), graph(), NodeView.DEFAULT);
            }
            ForeignCallNode __call = __graph.add(new ForeignCallNode(this.___runtime.getBackend().getForeignCalls(), __desc, __srcAddr, __destAddr, __len));
            __call.setStateAfter(stateAfter());
            __graph.replaceFixedWithFixed(this, __call);
        }
    }

    @Override
    public MemoryNode getLastLocationAccess()
    {
        return this.___lastLocationAccess;
    }

    @Override
    public void setLastLocationAccess(MemoryNode __lla)
    {
        updateUsagesInterface(this.___lastLocationAccess, __lla);
        this.___lastLocationAccess = __lla;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return this.___locationIdentity;
    }

    @Node.NodeIntrinsic
    private static native void arraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @Node.ConstantNodeParameter JavaKind __elementKind, @Node.ConstantNodeParameter boolean __aligned, @Node.ConstantNodeParameter boolean __disjoint, @Node.ConstantNodeParameter boolean __uninitialized);

    @Node.NodeIntrinsic
    private static native void arraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @Node.ConstantNodeParameter JavaKind __elementKind, @Node.ConstantNodeParameter LocationIdentity __locationIdentity, @Node.ConstantNodeParameter boolean __aligned, @Node.ConstantNodeParameter boolean __disjoint, @Node.ConstantNodeParameter boolean __uninitialized);

    public static void arraycopyObjectKillsAny(Object __src, int __srcPos, Object __dest, int __destPos, int __length)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, JavaKind.Object, LocationIdentity.any(), false, false, false);
    }

    public static void arraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @Node.ConstantNodeParameter JavaKind __elementKind)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, __elementKind, false, false, false);
    }

    public static void disjointArraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @Node.ConstantNodeParameter JavaKind __elementKind)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, __elementKind, false, true, false);
    }

    public static void disjointUninitializedArraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, @Node.ConstantNodeParameter JavaKind __elementKind)
    {
        arraycopy(__src, __srcPos, __dest, __destPos, __length, __elementKind, false, true, true);
    }

    public boolean isAligned()
    {
        return this.___aligned;
    }

    public boolean isDisjoint()
    {
        return this.___disjoint;
    }

    public boolean isUninitialized()
    {
        return this.___uninitialized;
    }

    boolean isHeapWordAligned(JavaConstant __value, JavaKind __kind)
    {
        return (HotSpotRuntime.getArrayBaseOffset(__kind) + (long) __value.asInt() * HotSpotRuntime.getArrayIndexScale(__kind)) % HotSpotRuntime.heapWordSize == 0;
    }

    public void updateAlignedDisjoint()
    {
        JavaKind __componentKind = this.___elementKind;
        if (this.___srcPos == this.___destPos)
        {
            // can treat as disjoint
            this.___disjoint = true;
        }
        PrimitiveConstant __constantSrc = (PrimitiveConstant) this.___srcPos.stamp(NodeView.DEFAULT).asConstant();
        PrimitiveConstant __constantDst = (PrimitiveConstant) this.___destPos.stamp(NodeView.DEFAULT).asConstant();
        if (__constantSrc != null && __constantDst != null)
        {
            if (!this.___aligned)
            {
                this.___aligned = isHeapWordAligned(__constantSrc, __componentKind) && isHeapWordAligned(__constantDst, __componentKind);
            }
            if (__constantSrc.asInt() >= __constantDst.asInt())
            {
                // low to high copy so treat as disjoint
                this.___disjoint = true;
            }
        }
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (getLength().isConstant() && getLength().asConstant().isDefaultForKind())
        {
            if (this.___lastLocationAccess != null)
            {
                replaceAtUsages(InputType.Memory, this.___lastLocationAccess.asNode());
            }
            return null;
        }
        return this;
    }
}
