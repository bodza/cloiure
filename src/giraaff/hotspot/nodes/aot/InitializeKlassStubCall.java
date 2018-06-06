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

///
// A call to the VM via a regular stub.
///
// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class InitializeKlassStubCall
public final class InitializeKlassStubCall extends AbstractMemoryCheckpoint implements LIRLowerable, Canonicalizable, DeoptimizingNode.DeoptBefore, MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<InitializeKlassStubCall> TYPE = NodeClass.create(InitializeKlassStubCall.class);

    @Node.OptionalInput
    // @field
    protected ValueNode ___value;
    @Node.Input
    // @field
    protected ValueNode ___string;
    @Node.OptionalInput(InputType.StateI)
    // @field
    protected FrameState ___stateBefore;
    // @field
    protected Constant ___constant;

    // @cons InitializeKlassStubCall
    protected InitializeKlassStubCall(ValueNode __value, ValueNode __string)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.___value = __value;
        this.___string = __string;
    }

    @Node.NodeIntrinsic
    public static native KlassPointer initializeKlass(KlassPointer __value, Object __string);

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
        __gen.setResult(this, ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitKlassInitializationAndRetrieval(this.___constant, __gen.operand(this.___string), __gen.state(this)));
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
}
