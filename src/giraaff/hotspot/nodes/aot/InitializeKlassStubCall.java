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
import giraaff.lir.LIRFrameState;
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
// NodeInfo.allowedUsageTypes = Memory
public class InitializeKlassStubCall extends AbstractMemoryCheckpoint implements LIRLowerable, Canonicalizable, DeoptimizingNode.DeoptBefore, MemoryCheckpoint.Single
{
    public static final NodeClass<InitializeKlassStubCall> TYPE = NodeClass.create(InitializeKlassStubCall.class);

    @OptionalInput protected ValueNode value;
    @Input protected ValueNode string;
    @OptionalInput(InputType.State) protected FrameState stateBefore;
    protected Constant constant;

    protected InitializeKlassStubCall(ValueNode value, ValueNode string)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.string = string;
    }

    @NodeIntrinsic
    public static native KlassPointer initializeKlass(KlassPointer value, Object string);

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
        Value stringValue = gen.operand(string);
        LIRFrameState fs = gen.state(this);
        Value result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitKlassInitializationAndRetrieval(constant, stringValue, fs);
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
}
