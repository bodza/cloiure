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
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

public class LoadConstantIndirectlyNode extends FloatingNode implements Canonicalizable, LIRLowerable
{
    public static final NodeClass<LoadConstantIndirectlyNode> TYPE = NodeClass.create(LoadConstantIndirectlyNode.class);

    @OptionalInput protected ValueNode value;
    protected Constant constant;
    protected HotSpotConstantLoadAction action;

    public LoadConstantIndirectlyNode(ValueNode value)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.constant = null;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    public LoadConstantIndirectlyNode(ValueNode value, HotSpotConstantLoadAction action)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.constant = null;
        this.action = action;
    }

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
        Value result;
        if (constant instanceof HotSpotObjectConstant)
        {
            result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitLoadObjectAddress(constant);
        }
        else if (constant instanceof HotSpotMetaspaceConstant)
        {
            result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitLoadMetaspaceAddress(constant, action);
        }
        else
        {
            throw new PermanentBailoutException("Unsupported constant type: " + constant);
        }
        gen.setResult(this, result);
    }

    @NodeIntrinsic
    public static native KlassPointer loadKlass(KlassPointer klassPointer, @ConstantNodeParameter HotSpotConstantLoadAction action);

    @NodeIntrinsic
    public static native KlassPointer loadKlass(KlassPointer klassPointer);

    @NodeIntrinsic
    public static native Object loadObject(Object object);
}
