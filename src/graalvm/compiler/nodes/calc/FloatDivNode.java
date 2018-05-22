package graalvm.compiler.nodes.calc;

import jdk.vm.ci.meta.Constant;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.BinaryOp;
import graalvm.compiler.core.common.type.ArithmeticOpTable.BinaryOp.Div;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public class FloatDivNode extends BinaryArithmeticNode<Div>
{
    public static final NodeClass<FloatDivNode> TYPE = NodeClass.create(FloatDivNode.class);

    public FloatDivNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

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
