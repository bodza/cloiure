package graalvm.compiler.nodes.debug;

import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.StandardOp;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

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
