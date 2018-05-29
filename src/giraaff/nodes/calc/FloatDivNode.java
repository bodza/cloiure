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
    public static final NodeClass<FloatDivNode> TYPE = NodeClass.create(FloatDivNode.class);

    // @cons
    public FloatDivNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

    // @cons
    protected FloatDivNode(NodeClass<? extends FloatDivNode> c, ValueNode x, ValueNode y)
    {
        super(c, ArithmeticOpTable::getDiv, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        BinaryOp<Div> op = ArithmeticOpTable.forStamp(x.stamp(view)).getDiv();
        Stamp stamp = op.foldStamp(x.stamp(view), y.stamp(view));
        ConstantNode tryConstantFold = tryConstantFold(op, x, y, stamp, view);
        if (tryConstantFold != null)
        {
            return tryConstantFold;
        }
        return canonical(null, op, x, y);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        ValueNode ret = super.canonical(tool, forX, forY);
        if (ret != this)
        {
            return ret;
        }

        return canonical(this, getOp(forX, forY), forX, forY);
    }

    private static ValueNode canonical(FloatDivNode self, BinaryOp<Div> op, ValueNode forX, ValueNode forY)
    {
        if (forY.isConstant())
        {
            Constant c = forY.asConstant();
            if (op.isNeutral(c))
            {
                return forX;
            }
        }
        return self != null ? self : new FloatDivNode(forX, forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitDiv(nodeValueMap.operand(getX()), nodeValueMap.operand(getY()), null));
    }
}
