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
// @class ResolveMethodAndLoadCountersStubCall
public final class ResolveMethodAndLoadCountersStubCall extends DeoptimizingStubCall implements Canonicalizable, LIRLowerable
{
    // @def
    public static final NodeClass<ResolveMethodAndLoadCountersStubCall> TYPE = NodeClass.create(ResolveMethodAndLoadCountersStubCall.class);

    @OptionalInput
    // @field
    protected ValueNode method;
    @Input
    // @field
    protected ValueNode klassHint;
    @Input
    // @field
    protected ValueNode methodDescription;
    // @field
    protected Constant methodConstant;

    // @cons
    public ResolveMethodAndLoadCountersStubCall(ValueNode __method, ValueNode __klassHint, ValueNode __methodDescription)
    {
        super(TYPE, MethodCountersPointerStamp.methodCountersNonNull());
        this.klassHint = __klassHint;
        this.method = __method;
        this.methodDescription = __methodDescription;
    }

    @NodeIntrinsic
    public static native MethodCountersPointer resolveMethodAndLoadCounters(MethodPointer method, KlassPointer klassHint, Object methodDescription);

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (method != null)
        {
            methodConstant = GraphUtil.foldIfConstantAndRemove(this, method);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        Value __methodDescriptionValue = __gen.operand(methodDescription);
        Value __klassHintValue = __gen.operand(klassHint);
        LIRFrameState __fs = __gen.state(this);

        __gen.setResult(this, ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitResolveMethodAndLoadCounters(methodConstant, __klassHintValue, __methodDescriptionValue, __fs));
    }
}
