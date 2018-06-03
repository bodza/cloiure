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
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

// @class LoadConstantIndirectlyNode
public final class LoadConstantIndirectlyNode extends FloatingNode implements Canonicalizable, LIRLowerable
{
    // @def
    public static final NodeClass<LoadConstantIndirectlyNode> TYPE = NodeClass.create(LoadConstantIndirectlyNode.class);

    @OptionalInput
    // @field
    protected ValueNode ___value;
    // @field
    protected Constant ___constant;
    // @field
    protected HotSpotConstantLoadAction ___action;

    // @cons
    public LoadConstantIndirectlyNode(ValueNode __value)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.___value = __value;
        this.___constant = null;
        this.___action = HotSpotConstantLoadAction.RESOLVE;
    }

    // @cons
    public LoadConstantIndirectlyNode(ValueNode __value, HotSpotConstantLoadAction __action)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.___value = __value;
        this.___constant = null;
        this.___action = __action;
    }

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
        Value __result;
        if (this.___constant instanceof HotSpotObjectConstant)
        {
            __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitLoadObjectAddress(this.___constant);
        }
        else if (this.___constant instanceof HotSpotMetaspaceConstant)
        {
            __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitLoadMetaspaceAddress(this.___constant, this.___action);
        }
        else
        {
            throw new BailoutException("unsupported constant type: " + this.___constant);
        }
        __gen.setResult(this, __result);
    }

    @NodeIntrinsic
    public static native KlassPointer loadKlass(KlassPointer __klassPointer, @ConstantNodeParameter HotSpotConstantLoadAction __action);

    @NodeIntrinsic
    public static native KlassPointer loadKlass(KlassPointer __klassPointer);

    @NodeIntrinsic
    public static native Object loadObject(Object __object);
}
