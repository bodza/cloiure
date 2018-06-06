package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
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

// @NodeInfo.allowedUsageTypes "InputType.Memory, InputType.Value"
// @class GenericArrayCopyCallNode
public final class GenericArrayCopyCallNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<GenericArrayCopyCallNode> TYPE = NodeClass.create(GenericArrayCopyCallNode.class);

    @Node.Input
    // @field
    ValueNode ___src;
    @Node.Input
    // @field
    ValueNode ___srcPos;
    @Node.Input
    // @field
    ValueNode ___dest;
    @Node.Input
    // @field
    ValueNode ___destPos;
    @Node.Input
    // @field
    ValueNode ___length;

    // @field
    protected final HotSpotGraalRuntime ___runtime;

    // @cons GenericArrayCopyCallNode
    protected GenericArrayCopyCallNode(@Node.InjectedNodeParameter HotSpotGraalRuntime __runtime, ValueNode __src, ValueNode __srcPos, ValueNode __dest, ValueNode __destPos, ValueNode __length)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.___src = __src;
        this.___srcPos = __srcPos;
        this.___dest = __dest;
        this.___destPos = __destPos;
        this.___length = __length;
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

    @Override
    public void lower(LoweringTool __tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            StructuredGraph __graph = graph();
            ValueNode __srcAddr = objectAddress(getSource());
            ValueNode __destAddr = objectAddress(getDestination());
            ForeignCallNode __call = __graph.add(new ForeignCallNode(this.___runtime.getBackend().getForeignCalls(), HotSpotBackend.GENERIC_ARRAYCOPY, __srcAddr, this.___srcPos, __destAddr, this.___destPos, this.___length));
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
        if (__value.stamp(NodeView.DEFAULT).getStackKind() != this.___runtime.getTarget().wordJavaKind)
        {
            return IntegerConvertNode.convert(__value, StampFactory.forKind(this.___runtime.getTarget().wordJavaKind), graph(), NodeView.DEFAULT);
        }
        return __value;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Node.NodeIntrinsic
    public static native int genericArraycopy(Object __src, int __srcPos, Object __dest, int __destPos, int __length);
}
