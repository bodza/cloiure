package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.hotspot.nodes.DeoptimizingStubCall;
import giraaff.hotspot.nodes.type.MethodCountersPointerStamp;
import giraaff.hotspot.word.KlassPointer;
import giraaff.hotspot.word.MethodCountersPointer;
import giraaff.hotspot.word.MethodPointer;
import giraaff.lir.LIRFrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

/**
 * A call to the VM via a regular stub.
 */
public class ResolveMethodAndLoadCountersStubCall extends DeoptimizingStubCall implements Canonicalizable, LIRLowerable
{
    public static final NodeClass<ResolveMethodAndLoadCountersStubCall> TYPE = NodeClass.create(ResolveMethodAndLoadCountersStubCall.class);

    @OptionalInput protected ValueNode method;
    @Input protected ValueNode klassHint;
    @Input protected ValueNode methodDescription;
    protected Constant methodConstant;

    public ResolveMethodAndLoadCountersStubCall(ValueNode method, ValueNode klassHint, ValueNode methodDescription)
    {
        super(TYPE, MethodCountersPointerStamp.methodCountersNonNull());
        this.klassHint = klassHint;
        this.method = method;
        this.methodDescription = methodDescription;
    }

    @NodeIntrinsic
    public static native MethodCountersPointer resolveMethodAndLoadCounters(MethodPointer method, KlassPointer klassHint, Object methodDescription);

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (method != null)
        {
            methodConstant = GraphUtil.foldIfConstantAndRemove(this, method);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        Value methodDescriptionValue = gen.operand(methodDescription);
        Value klassHintValue = gen.operand(klassHint);
        LIRFrameState fs = gen.state(this);

        Value result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitResolveMethodAndLoadCounters(methodConstant, klassHintValue, methodDescriptionValue, fs);

        gen.setResult(this, result);
    }
}
