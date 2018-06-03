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
    protected ValueNode value;
    // @field
    protected Constant constant;
    // @field
    protected HotSpotConstantLoadAction action;

    // @cons
    public LoadConstantIndirectlyNode(ValueNode __value)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
        this.constant = null;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    // @cons
    public LoadConstantIndirectlyNode(ValueNode __value, HotSpotConstantLoadAction __action)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT));
        this.value = __value;
        this.constant = null;
        this.action = __action;
    }

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
        Value __result;
        if (constant instanceof HotSpotObjectConstant)
        {
            __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitLoadObjectAddress(constant);
        }
        else if (constant instanceof HotSpotMetaspaceConstant)
        {
            __result = ((HotSpotLIRGenerator) __gen.getLIRGeneratorTool()).emitLoadMetaspaceAddress(constant, action);
        }
        else
        {
            throw new BailoutException("unsupported constant type: " + constant);
        }
        __gen.setResult(this, __result);
    }

    @NodeIntrinsic
    public static native KlassPointer loadKlass(KlassPointer klassPointer, @ConstantNodeParameter HotSpotConstantLoadAction action);

    @NodeIntrinsic
    public static native KlassPointer loadKlass(KlassPointer klassPointer);

    @NodeIntrinsic
    public static native Object loadObject(Object object);
}
