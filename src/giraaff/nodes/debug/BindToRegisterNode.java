package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.lir.StandardOp;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public final class BindToRegisterNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<BindToRegisterNode> TYPE = NodeClass.create(BindToRegisterNode.class);
    @Input ValueNode value;

    public BindToRegisterNode(ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().append(new StandardOp.BindToRegisterOp(gen.getLIRGeneratorTool().asAllocatable(gen.operand(value))));
    }
}
