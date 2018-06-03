package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Div;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class FloatDivNode
public final class FloatDivNode extends BinaryArithmeticNode<Div>
{
    // @def
    public static final NodeClass<FloatDivNode> TYPE = NodeClass.create(FloatDivNode.class);

    // @cons
    public FloatDivNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons
    protected FloatDivNode(NodeClass<? extends FloatDivNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, ArithmeticOpTable::getDiv, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        BinaryOp<Div> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getDiv();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), __y.stamp(__view));
        ConstantNode __tryConstantFold = tryConstantFold(__op, __x, __y, __stamp, __view);
        if (__tryConstantFold != null)
        {
            return __tryConstantFold;
        }
        return canonical(null, __op, __x, __y);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        return canonical(this, getOp(__forX, __forY), __forX, __forY);
    }

    private static ValueNode canonical(FloatDivNode __self, BinaryOp<Div> __op, ValueNode __forX, ValueNode __forY)
    {
        if (__forY.isConstant())
        {
            Constant __c = __forY.asConstant();
            if (__op.isNeutral(__c))
            {
                return __forX;
            }
        }
        return __self != null ? __self : new FloatDivNode(__forX, __forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitDiv(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY()), null));
    }
}
