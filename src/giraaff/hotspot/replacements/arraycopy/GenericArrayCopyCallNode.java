package giraaff.hotspot.replacements.arraycopy;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.nodes.GetObjectAddressNode;
import giraaff.nodeinfo.InputType;
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
public final class GenericArrayCopyCallNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single
{
    public static final NodeClass<GenericArrayCopyCallNode> TYPE = NodeClass.create(GenericArrayCopyCallNode.class);
    @Input ValueNode src;
    @Input ValueNode srcPos;
    @Input ValueNode dest;
    @Input ValueNode destPos;
    @Input ValueNode length;

    protected final HotSpotGraalRuntimeProvider runtime;

    protected GenericArrayCopyCallNode(@InjectedNodeParameter HotSpotGraalRuntimeProvider runtime, ValueNode src, ValueNode srcPos, ValueNode dest, ValueNode destPos, ValueNode length)
    {
        super(TYPE, StampFactory.forKind(JavaKind.Int));
        this.src = src;
        this.srcPos = srcPos;
        this.dest = dest;
        this.destPos = destPos;
        this.length = length;
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

    @Override
    public void lower(LoweringTool tool)
    {
        if (graph().getGuardsStage().areFrameStatesAtDeopts())
        {
            StructuredGraph graph = graph();
            ValueNode srcAddr = objectAddress(getSource());
            ValueNode destAddr = objectAddress(getDestination());
            ForeignCallNode call = graph.add(new ForeignCallNode(runtime.getHostBackend().getForeignCalls(), HotSpotBackend.GENERIC_ARRAYCOPY, srcAddr, srcPos, destAddr, destPos, length));
            call.setStateAfter(stateAfter());
            graph.replaceFixedWithFixed(this, call);
        }
    }

    private ValueNode objectAddress(ValueNode obj)
    {
        GetObjectAddressNode result = graph().add(new GetObjectAddressNode(obj));
        graph().addBeforeFixed(this, result);
        return result;
    }

    private ValueNode wordValue(ValueNode value)
    {
        if (value.stamp(NodeView.DEFAULT).getStackKind() != runtime.getTarget().wordJavaKind)
        {
            return IntegerConvertNode.convert(value, StampFactory.forKind(runtime.getTarget().wordJavaKind), graph(), NodeView.DEFAULT);
        }
        return value;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @NodeIntrinsic
    public static native int genericArraycopy(Object src, int srcPos, Object dest, int destPos, int length);
}
