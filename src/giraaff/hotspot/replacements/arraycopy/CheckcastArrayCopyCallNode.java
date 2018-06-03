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
    ValueNode src;
    @Input
    // @field
    ValueNode srcPos;
    @Input
    // @field
    ValueNode dest;
    @Input
    // @field
    ValueNode destPos;
    @Input
    // @field
    ValueNode length;
    @Input
    // @field
    ValueNode destElemKlass;
    @Input
    // @field
    ValueNode superCheckOffset;

    // @field
    protected final boolean uninit;

    // @field
    protected final HotSpotGraalRuntime runtime;

    // @cons
    protected CheckcastArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length, ValueNode __superCheckOffset, ValueNode __destElemKlass, boolean __uninit)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.src = __src;
        this.srcPos = __srcPos;
        this.dest = __dest;
        this.destPos = __destPos;
        this.length = __length;
        this.superCheckOffset = __superCheckOffset;
        this.destElemKlass = __destElemKlass;
        this.uninit = __uninit;
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

    public boolean isUninit()
    {
        return uninit;
    }

    private ValueNode computeBase(ValueNode __base, ValueNode __pos)
    {
        FixedWithNextNode __basePtr = graph().add(new GetObjectAddressNode(__base));
        graph().addBeforeFixed(this, __basePtr);

        int __shift = CodeUtil.log2(HotSpotRuntime.getArrayIndexScale(JavaKind.Object));
        ValueNode __extendedPos = IntegerConvertNode.convert(__pos, StampFactory.forKind(runtime.getTarget().wordJavaKind), graph(), NodeView.DEFAULT);
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
            if (__len.stamp(NodeView.DEFAULT).getStackKind() != runtime.getTarget().wordJavaKind)
            {
                __len = IntegerConvertNode.convert(__len, StampFactory.forKind(runtime.getTarget().wordJavaKind), graph(), NodeView.DEFAULT);
            }
            ForeignCallNode __call = __graph.add(new ForeignCallNode(runtime.getBackend().getForeignCalls(), __desc, __srcAddr, __destAddr, __len, superCheckOffset, destElemKlass));
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
    public static native int checkcastArraycopy(Object src, int srcPos, Object dest, int destPos, int length, Word superCheckOffset, Object destElemKlass, @ConstantNodeParameter boolean uninit);
}
