package giraaff.hotspot.nodes.aot;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

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
// @class ResolveConstantStubCall
public final class ResolveConstantStubCall extends DeoptimizingStubCall implements Canonicalizable, LIRLowerable
{
    // @def
    public static final NodeClass<ResolveConstantStubCall> TYPE = NodeClass.create(ResolveConstantStubCall.class);

    @OptionalInput
    // @field
    protected ValueNode value;
    @Input
    // @field
    protected ValueNode string;
    // @field
    protected Constant constant;
    // @field
    protected HotSpotConstantLoadAction action;

    // @cons
    public ResolveConstantStubCall(ValueNode __value, ValueNode __string)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
        this.string = __string;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    // @cons
    public ResolveConstantStubCall(ValueNode __value, ValueNode __string, HotSpotConstantLoadAction __action)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
        this.string = __string;
        this.action = __action;
    }

    @NodeIntrinsic
    public static native Object resolveObject(Object value, Object symbol);

    @NodeIntrinsic
    public static native KlassPointer resolveKlass(KlassPointer value, Object symbol);

    @NodeIntrinsic
    public static native KlassPointer resolveKlass(KlassPointer value, Object symbol, @ConstantNodeParameter HotSpotConstantLoadAction action);

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
        Value __stringValue = __gen.operand(string);
        LIRFrameState __fs = __gen.state(this);
        Value __result;
        if (constant instanceof HotSpotObjectConstant)
        {
            __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitObjectConstantRetrieval(constant, __stringValue, __fs);
        }
        else if (constant instanceof HotSpotMetaspaceConstant)
        {
            if (action == HotSpotConstantLoadAction.RESOLVE)
            {
                __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitMetaspaceConstantRetrieval(constant, __stringValue, __fs);
            }
            else
            {
                __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitKlassInitializationAndRetrieval(constant, __stringValue, __fs);
            }
        }
        else
        {
            throw new BailoutException("unsupported constant type: " + constant);
        }
        __gen.setResult(this, __result);
    }
}
