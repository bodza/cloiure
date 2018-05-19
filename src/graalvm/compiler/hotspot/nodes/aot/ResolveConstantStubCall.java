package graalvm.compiler.hotspot.nodes.aot;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_16;

import graalvm.compiler.core.common.PermanentBailoutException;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction;
import graalvm.compiler.hotspot.nodes.DeoptimizingStubCall;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.util.GraphUtil;

import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

/**
 * A call to the VM via a regular stub.
 */
@NodeInfo(cycles = CYCLES_UNKNOWN, size = SIZE_16)
public class ResolveConstantStubCall extends DeoptimizingStubCall implements Canonicalizable, LIRLowerable
{
    public static final NodeClass<ResolveConstantStubCall> TYPE = NodeClass.create(ResolveConstantStubCall.class);

    @OptionalInput protected ValueNode value;
    @Input protected ValueNode string;
    protected Constant constant;
    protected HotSpotConstantLoadAction action;

    public ResolveConstantStubCall(ValueNode value, ValueNode string)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.string = string;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    public ResolveConstantStubCall(ValueNode value, ValueNode string, HotSpotConstantLoadAction action)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.string = string;
        this.action = action;
    }

    @NodeIntrinsic
    public static native Object resolveObject(Object value, Object symbol);

    @NodeIntrinsic
    public static native KlassPointer resolveKlass(KlassPointer value, Object symbol);

    @NodeIntrinsic
    public static native KlassPointer resolveKlass(KlassPointer value, Object symbol, @ConstantNodeParameter HotSpotConstantLoadAction action);

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
        Value result;
        LIRFrameState fs = gen.state(this);
        if (constant instanceof HotSpotObjectConstant)
        {
            result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitObjectConstantRetrieval(constant, stringValue, fs);
        }
        else if (constant instanceof HotSpotMetaspaceConstant)
        {
            if (action == HotSpotConstantLoadAction.RESOLVE)
            {
                result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitMetaspaceConstantRetrieval(constant, stringValue, fs);
            }
            else
            {
                result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitKlassInitializationAndRetrieval(constant, stringValue, fs);
            }
        }
        else
        {
            throw new PermanentBailoutException("Unsupported constant type: " + constant);
        }
        gen.setResult(this, result);
    }
}
