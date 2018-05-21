package graalvm.compiler.hotspot.nodes.aot;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.hotspot.word.KlassPointer;
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
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

/**
 * A call to the VM via a regular stub.
 */
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
