package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.PermanentBailoutException;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.hotspot.HotSpotLIRGenerator;
import giraaff.hotspot.meta.HotSpotConstantLoadAction;
import giraaff.hotspot.nodes.DeoptimizingStubCall;
import giraaff.hotspot.word.KlassPointer;
import giraaff.lir.LIRFrameState;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

/**
 * A call to the VM via a regular stub.
 */
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
        LIRFrameState fs = gen.state(this);
        Value result;
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
