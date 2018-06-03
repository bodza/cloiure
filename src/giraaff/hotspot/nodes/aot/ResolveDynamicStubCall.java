package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

/**
 * A call to the VM via a regular stub.
 */
// @NodeInfo.allowedUsageTypes "Memory"
// @class ResolveDynamicStubCall
public final class ResolveDynamicStubCall extends AbstractMemoryCheckpoint implements LIRLowerable, Canonicalizable, DeoptimizingNode.DeoptBefore, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<ResolveDynamicStubCall> TYPE = NodeClass.create(ResolveDynamicStubCall.class);

    @OptionalInput
    // @field
    protected ValueNode value;
    @OptionalInput(InputType.State)
    // @field
    protected FrameState stateBefore;
    // @field
    protected Constant constant;

    // @cons
    public ResolveDynamicStubCall(ValueNode __value)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
    }

    @NodeIntrinsic
    public static native Object resolveInvoke(Object value);

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (value != null)
        {
            constant = GraphUtil.foldIfConstantAndRemove(this, value);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitResolveDynamicInvoke(constant, __gen.state(this)));
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    @Override
    public FrameState stateBefore()
    {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __f)
    {
        updateUsages(stateBefore, __f);
        stateBefore = __f;
    }

    @Override
    public void markDeleted()
    {
        throw GraalError.shouldNotReachHere("ResolveDynamicStubCall node deleted");
    }
}
