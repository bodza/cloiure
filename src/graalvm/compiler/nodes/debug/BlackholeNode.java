package graalvm.compiler.nodes.debug;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class BlackholeNode extends FixedWithNextNode implements LIRLowerable
{
    public static final NodeClass<BlackholeNode> TYPE = NodeClass.create(BlackholeNode.class);
    @Input ValueNode value;

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
