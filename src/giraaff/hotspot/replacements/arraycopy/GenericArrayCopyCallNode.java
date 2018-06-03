package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.hotspot.nodes.GetObjectAddressNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @NodeInfo.allowedUsageTypes "Memory, Value"
// @class GenericArrayCopyCallNode
public final class GenericArrayCopyCallNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<GenericArrayCopyCallNode> TYPE = NodeClass.create(GenericArrayCopyCallNode.class);

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

    // @field
    protected final HotSpotGraalRuntime runtime;

    // @cons
    protected GenericArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.src = __src;
        this.srcPos = __srcPos;
        this.dest = __dest;
        this.destPos = __destPos;
        this.length = __length;
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

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            StructuredGraph __graph = graph();
            ValueNode __srcAddr = objectAddress(getSource());
            ValueNode __destAddr = objectAddress(getDestination());
            ForeignCallNode __call = __graph.add(new ForeignCallNode(runtime.getBackend().getForeignCalls(), HotSpotBackend.GENERIC_ARRAYCOPY, __srcAddr, srcPos, __destAddr, destPos, length));
            __call.setStateAfter(stateAfter());
            __graph.replaceFixedWithFixed(this, __call);
        }
    }

    private ValueNode objectAddress(ValueNode __obj)
    {
        GetObjectAddressNode __result = graph().add(new GetObjectAddressNode(__obj));
        graph().addBeforeFixed(this, __result);
        return __result;
    }

    private ValueNode wordValue(ValueNode __value)
    {
        if (__value.stamp(NodeView.DEFAULT).getStackKind() != runtime.getTarget().wordJavaKind)
        {
            return IntegerConvertNode.convert(__value, StampFactory.forKind(runtime.getTarget().wordJavaKind), graph(), NodeView.DEFAULT);
        }
        return __value;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @NodeIntrinsic
    public static native int genericArraycopy(Object src, int srcPos, Object dest, int destPos, int length);
}
