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

///
// Square root.
///
// @class SqrtNode
public final class SqrtNode extends UnaryArithmeticNode<Sqrt> implements ArithmeticLIRLowerable
{
    // @def
    public static final NodeClass<SqrtNode> TYPE = NodeClass.create(SqrtNode.class);

    // @cons
    protected SqrtNode(ValueNode __x)
    {
        super(TYPE, ArithmeticOpTable::getSqrt, __x);
    }

    public static ValueNode create(ValueNode __x, NodeView __view)
    {
        if (__x.isConstant())
        {
            ArithmeticOpTable.UnaryOp<Sqrt> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getSqrt();
            return ConstantNode.forPrimitive(__op.foldStamp(__x.stamp(__view)), __op.foldConstant(__x.asConstant()));
        }
        return new SqrtNode(__x);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitMathSqrt(__nodeValueMap.operand(getValue())));
    }
}
