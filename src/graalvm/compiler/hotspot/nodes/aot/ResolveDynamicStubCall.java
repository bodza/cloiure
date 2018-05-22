package graalvm.compiler.hotspot.nodes.aot;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.DeoptimizingNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.util.GraphUtil;

/**
 * A call to the VM via a regular stub.
 */
public class ResolveDynamicStubCall extends AbstractMemoryCheckpoint implements LIRLowerable, Canonicalizable, DeoptimizingNode.DeoptBefore, MemoryCheckpoint.Single
{
    public static final NodeClass<ResolveDynamicStubCall> TYPE = NodeClass.create(ResolveDynamicStubCall.class);

    @OptionalInput protected ValueNode value;
    @OptionalInput(InputType.State) protected FrameState stateBefore;
    protected Constant constant;

    public ResolveDynamicStubCall(ValueNode value)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
    }

    @NodeIntrinsic
    public static native Object resolveInvoke(Object value);

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (value != null)
        {
            constant = GraphUtil.foldIfConstantAndRemove(this, value);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value result;
        LIRFrameState fs = gen.state(this);
        result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitResolveDynamicInvoke(constant, fs);
        gen.setResult(this, result);
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
    public void setStateBefore(FrameState f)
    {
        updateUsages(stateBefore, f);
        stateBefore = f;
    }

    @Override
    public void markDeleted()
    {
        throw GraalError.shouldNotReachHere("ResolveDynamicStubCall node deleted");
    }
}
