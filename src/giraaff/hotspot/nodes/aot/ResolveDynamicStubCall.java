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

///
// A call to the VM via a regular stub.
///
// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class ResolveDynamicStubCall
public final class ResolveDynamicStubCall extends AbstractMemoryCheckpoint implements LIRLowerable, Canonicalizable, DeoptimizingNode.DeoptBefore, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<ResolveDynamicStubCall> TYPE = NodeClass.create(ResolveDynamicStubCall.class);

    @Node.OptionalInput
    // @field
    protected ValueNode ___value;
    @Node.OptionalInput(InputType.StateI)
    // @field
    protected FrameState ___stateBefore;
    // @field
    protected Constant ___constant;

    // @cons ResolveDynamicStubCall
    public ResolveDynamicStubCall(ValueNode __value)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.___value = __value;
    }

    @Node.NodeIntrinsic
    public static native Object resolveInvoke(Object __value);

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (this.___value != null)
        {
            this.___constant = GraphUtil.foldIfConstantAndRemove(this, this.___value);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitResolveDynamicInvoke(this.___constant, __gen.state(this)));
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
        return this.___stateBefore;
    }

    @Override
    public void setStateBefore(FrameState __f)
    {
        updateUsages(this.___stateBefore, __f);
        this.___stateBefore = __f;
    }

    @Override
    public void markDeleted()
    {
        throw GraalError.shouldNotReachHere("ResolveDynamicStubCall node deleted");
    }
}
