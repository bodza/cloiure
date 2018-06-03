package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.lir.StandardOp;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class BindToRegisterNode
public final class BindToRegisterNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<BindToRegisterNode> TYPE = NodeClass.create(BindToRegisterNode.class);

    @Input
    // @field
    ValueNode ___value;

    // @cons
    public BindToRegisterNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.___value = __value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().append(new StandardOp.BindToRegisterOp(__gen.getLIRGeneratorTool().asAllocatable(__gen.operand(this.___value))));
    }
}
