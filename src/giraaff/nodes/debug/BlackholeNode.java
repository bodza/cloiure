package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class BlackholeNode
public final class BlackholeNode extends FixedWithNextNode implements LIRLowerable
{
    // @def
    public static final NodeClass<BlackholeNode> TYPE = NodeClass.create(BlackholeNode.class);

    @Input
    // @field
    ValueNode value;

    // @cons
    public BlackholeNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = __value;
    }

    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitBlackhole(__gen.operand(value));
    }
}
