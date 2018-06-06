package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class RemNode
public final class RemNode extends BinaryArithmeticNode<ArithmeticOpTable.BinaryOp.Rem> implements Lowerable
{
    // @def
    public static final NodeClass<RemNode> TYPE = NodeClass.create(RemNode.class);

    // @cons RemNode
    protected RemNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons RemNode
    protected RemNode(NodeClass<? extends RemNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, ArithmeticOpTable::getRem, __x, __y);
    }

    public static ValueNode create(ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Rem> __op = ArithmeticOpTable.forStamp(__forX.stamp(__view)).getRem();
        Stamp __stamp = __op.foldStamp(__forX.stamp(__view), __forY.stamp(__view));
        ConstantNode __tryConstantFold = tryConstantFold(__op, __forX, __forY, __stamp, __view);
        if (__tryConstantFold != null)
        {
            return __tryConstantFold;
        }
        return new RemNode(__forX, __forY);
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitRem(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY()), null));
    }
}
