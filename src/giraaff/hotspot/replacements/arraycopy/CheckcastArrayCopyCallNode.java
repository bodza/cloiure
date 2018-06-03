package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotHostForeignCallsProvider;
import giraaff.hotspot.nodes.GetObjectAddressNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.calc.LeftShiftNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.memory.address.OffsetAddressNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.word.Word;

// @NodeInfo.allowedUsageTypes "Memory, Value"
// @class CheckcastArrayCopyCallNode
public final class CheckcastArrayCopyCallNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<CheckcastArrayCopyCallNode> TYPE = NodeClass.create(CheckcastArrayCopyCallNode.class);

    @Input
    // @field
    ValueNode ___src;
    @Input
    // @field
    ValueNode ___srcPos;
    @Input
    // @field
    ValueNode ___dest;
    @Input
    // @field
    ValueNode ___destPos;
    @Input
    // @field
    ValueNode ___length;
    @Input
    // @field
    ValueNode ___destElemKlass;
    @Input
    // @field
    ValueNode ___superCheckOffset;

    // @field
    protected final boolean ___uninit;

    // @field
    protected final HotSpotGraalRuntime ___runtime;

    // @cons
    protected CheckcastArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, ValueNode __superCheckOffset, ValueNode __destElemKlass, boolean __uninit)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.___src = __src;
        this.___srcPos = __srcPos;
        this.___dest = __dest;
        this.___destPos = __destPos;
        this.___length = __length;
        this.___superCheckOffset = __superCheckOffset;
        this.___destElemKlass = __destElemKlass;
        this.___uninit = __uninit;
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

    public boolean isUninit()
    {
        return this.___uninit;
    }

    private ValueNode computeBase(ValueNode __base, ValueNode __pos)
    {
        FixedWithNextNode __basePtr = graph().add(new GetObjectAddressNode(__base));
        graph().addBeforeFixed(this, __basePtr);

        int __shift = CodeUtil.log2(HotSpotRuntime.getArrayIndexScale(JavaKind.Object));
        ValueNode __extendedPos = IntegerConvertNode.convert(__pos, StampFactory.forKind(this.___runtime.getTarget().wordJavaKind), graph(), NodeView.DEFAULT);
        ValueNode __scaledIndex = graph().unique(new LeftShiftNode(__extendedPos, ConstantNode.forInt(__shift, graph())));
        ValueNode __offset = graph().unique(new AddNode(__scaledIndex, ConstantNode.forIntegerBits(PrimitiveStamp.getBits(__scaledIndex.stamp(NodeView.DEFAULT)), HotSpotRuntime.getArrayBaseOffset(JavaKind.Object), graph())));
        return graph().unique(new OffsetAddressNode(__basePtr, __offset));
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            ForeignCallDescriptor __desc = HotSpotHostForeignCallsProvider.lookupCheckcastArraycopyDescriptor(isUninit());
            StructuredGraph __graph = graph();
            ValueNode __srcAddr = computeBase(getSource(), getSourcePosition());
            ValueNode __destAddr = computeBase(getDestination(), getDestinationPosition());
            ValueNode __len = getLength();
            if (__len.stamp(NodeView.DEFAULT).getStackKind() != this.___runtime.getTarget().wordJavaKind)
            {
                __len = IntegerConvertNode.convert(__len, StampFactory.forKind(this.___runtime.getTarget().wordJavaKind), graph(), NodeView.DEFAULT);
            }
            ForeignCallNode __call = __graph.add(new ForeignCallNode(this.___runtime.getBackend().getForeignCalls(), __desc, __srcAddr, __destAddr, __len, this.___superCheckOffset, this.___destElemKlass));
            __call.setStateAfter(stateAfter());
            __graph.replaceFixedWithFixed(this, __call);
        }
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        // Because of restrictions that the memory graph of snippets matches the original node, pretend that we kill any.
        return LocationIdentity.any();
    }

    @NodeIntrinsic
    public static native int checkcastArraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length, Word __superCheckOffset, Object __destElemKlass, @ConstantNodeParameter boolean __uninit);
}
