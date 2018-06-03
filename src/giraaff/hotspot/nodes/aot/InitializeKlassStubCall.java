package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.hotspot.word.KlassPointer;
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

/**
 * A call to the VM via a regular stub.
 */
// @NodeInfo.allowedUsageTypes "Memory"
// @class InitializeKlassStubCall
public final class InitializeKlassStubCall extends AbstractMemoryCheckpoint implements LIRLowerable, Canonicalizable, DeoptimizingNode.DeoptBefore, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<InitializeKlassStubCall> TYPE = NodeClass.create(InitializeKlassStubCall.class);

    @OptionalInput
    // @field
    protected ValueNode value;
    @Input
    // @field
    protected ValueNode string;
    @OptionalInput(InputType.State)
    // @field
    protected FrameState stateBefore;
    // @field
    protected Constant constant;

    // @cons
    protected InitializeKlassStubCall(ValueNode __value, ValueNode __string)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
        this.string = __string;
    }

    @NodeIntrinsic
    public static native KlassPointer initializeKlass(KlassPointer value, Object string);

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
        __gen.setResult(this, ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitKlassInitializationAndRetrieval(constant, __gen.operand(string), __gen.state(this)));
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
}
