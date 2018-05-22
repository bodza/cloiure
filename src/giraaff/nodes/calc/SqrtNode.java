package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Sqrt;
import giraaff.graph.NodeClass;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Square root.
 */
public final class SqrtNode extends UnaryArithmeticNode<Sqrt> implements ArithmeticLIRLowerable
{
    public static final NodeClass<SqrtNode> TYPE = NodeClass.create(SqrtNode.class);

    protected SqrtNode(ValueNode x)
    {
        super(TYPE, ArithmeticOpTable::getSqrt, x);
    }

    public static ValueNode create(ValueNode x, NodeView view)
    {
        if (x.isConstant())
        {
            ArithmeticOpTable.UnaryOp<Sqrt> op = ArithmeticOpTable.forStamp(x.stamp(view)).getSqrt();
            return ConstantNode.forPrimitive(op.foldStamp(x.stamp(view)), op.foldConstant(x.asConstant()));
        }
        return new SqrtNode(x);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitMathSqrt(nodeValueMap.operand(getValue())));
    }
}
