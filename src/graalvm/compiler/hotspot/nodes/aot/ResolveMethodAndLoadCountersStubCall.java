package graalvm.compiler.hotspot.nodes.aot;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_16;

import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.hotspot.nodes.DeoptimizingStubCall;
import graalvm.compiler.hotspot.nodes.type.MethodCountersPointerStamp;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.hotspot.word.MethodCountersPointer;
import graalvm.compiler.hotspot.word.MethodPointer;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.util.GraphUtil;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

/**
 * A call to the VM via a regular stub.
 */
@NodeInfo(cycles = CYCLES_UNKNOWN, size = SIZE_16)
public class ResolveMethodAndLoadCountersStubCall extends DeoptimizingStubCall implements Canonicalizable, LIRLowerable {
    public static final NodeClass<ResolveMethodAndLoadCountersStubCall> TYPE = NodeClass.create(ResolveMethodAndLoadCountersStubCall.class);

    @OptionalInput protected ValueNode method;
    @Input protected ValueNode klassHint;
    @Input protected ValueNode methodDescription;
    protected Constant methodConstant;

    public ResolveMethodAndLoadCountersStubCall(ValueNode method, ValueNode klassHint, ValueNode methodDescription) {
        super(TYPE, MethodCountersPointerStamp.methodCountersNonNull());
        this.klassHint = klassHint;
        this.method = method;
        this.methodDescription = methodDescription;
    }

    @NodeIntrinsic
    public static native MethodCountersPointer resolveMethodAndLoadCounters(MethodPointer method, KlassPointer klassHint, Object methodDescription);

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (method != null) {
            methodConstant = GraphUtil.foldIfConstantAndRemove(this, method);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        assert methodConstant != null : "Expected method to fold: " + method;

        Value methodDescriptionValue = gen.operand(methodDescription);
        Value klassHintValue = gen.operand(klassHint);
        LIRFrameState fs = gen.state(this);
        assert fs != null : "The stateAfter is null";

        Value result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitResolveMethodAndLoadCounters(methodConstant, klassHintValue, methodDescriptionValue, fs);

        gen.setResult(this, result);
    }

}
