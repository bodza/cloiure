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
    public static final NodeClass<BlackholeNode> TYPE = NodeClass.create(BlackholeNode.class);

    @Input ValueNode value;

    // @cons
    public BlackholeNode(ValueNode value)
    {
        super(TYPE, StampFactory.forVoid());
        this.value = value;
    }

    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.getLIRGeneratorTool().emitBlackhole(gen.operand(value));
    }
}
