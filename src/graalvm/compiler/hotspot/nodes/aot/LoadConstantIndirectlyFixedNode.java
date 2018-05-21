package graalvm.compiler.hotspot.nodes.aot;

import graalvm.compiler.core.common.PermanentBailoutException;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.hotspot.HotSpotLIRGenerator;
import graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.hotspot.word.MethodPointer;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.util.GraphUtil;

import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

public class LoadConstantIndirectlyFixedNode extends FixedWithNextNode implements Canonicalizable, LIRLowerable
{
    public static final NodeClass<LoadConstantIndirectlyFixedNode> TYPE = NodeClass.create(LoadConstantIndirectlyFixedNode.class);

    @OptionalInput protected ValueNode value;
    protected Constant constant;
    protected HotSpotConstantLoadAction action;

    public LoadConstantIndirectlyFixedNode(ValueNode value)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
        this.constant = null;
        this.action = HotSpotConstantLoadAction.RESOLVE;
    }

    public LoadConstantIndirectlyFixedNode(ValueNode value, HotSpotConstantLoadAction action)
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
    public static native MethodPointer loadMethod(MethodPointer klassPointer);

    @NodeIntrinsic
    public static native Object loadObject(Object object);
}
