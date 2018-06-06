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

///
// A call to the VM via a regular stub.
///
// @class ResolveConstantStubCall
public final class ResolveConstantStubCall extends DeoptimizingStubCall implements Canonicalizable, LIRLowerable
{
    // @def
    public static final NodeClass<ResolveConstantStubCall> TYPE = NodeClass.create(ResolveConstantStubCall.class);

    @Node.OptionalInput
    // @field
    protected ValueNode ___value;
    @Node.Input
    // @field
    protected ValueNode ___string;
    // @field
    protected Constant ___constant;
    // @field
    protected HotSpotConstantLoadAction ___action;

    // @cons ResolveConstantStubCall
    public ResolveConstantStubCall(ValueNode __value, ValueNode __string)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.___value = __value;
        this.___string = __string;
        this.___action = HotSpotConstantLoadAction.RESOLVE;
    }

    // @cons ResolveConstantStubCall
    public ResolveConstantStubCall(ValueNode __value, ValueNode __string, HotSpotConstantLoadAction __action)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.___value = __value;
        this.___string = __string;
        this.___action = __action;
    }

    @Node.NodeIntrinsic
    public static native Object resolveObject(Object __value, Object __symbol);

    @Node.NodeIntrinsic
    public static native KlassPointer resolveKlass(KlassPointer __value, Object __symbol);

    @Node.NodeIntrinsic
    public static native KlassPointer resolveKlass(KlassPointer __value, Object __symbol, @Node.ConstantNodeParameter HotSpotConstantLoadAction __action);

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
        Value __stringValue = __gen.operand(this.___string);
        LIRFrameState __fs = __gen.state(this);
        Value __result;
        if (this.___constant instanceof HotSpotObjectConstant)
        {
            __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitObjectConstantRetrieval(this.___constant, __stringValue, __fs);
        }
        else if (this.___constant instanceof HotSpotMetaspaceConstant)
        {
            if (this.___action == HotSpotConstantLoadAction.RESOLVE)
            {
                __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitMetaspaceConstantRetrieval(this.___constant, __stringValue, __fs);
            }
            else
            {
                __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitKlassInitializationAndRetrieval(this.___constant, __stringValue, __fs);
            }
        }
        else
        {
            throw new BailoutException("unsupported constant type: " + this.___constant);
        }
        __gen.setResult(this, __result);
    }
}
