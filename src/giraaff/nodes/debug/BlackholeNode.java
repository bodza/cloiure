package giraaff.nodes.debug;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
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

    @Node.Input
    // @field
    ValueNode ___value;

    // @cons BlackholeNode
    public BlackholeNode(ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid());
        this.___value = __value;
    }

    public ValueNode getValue()
    {
        return this.___value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.getLIRGeneratorTool().emitBlackhole(__gen.operand(this.___value));
    }
}
