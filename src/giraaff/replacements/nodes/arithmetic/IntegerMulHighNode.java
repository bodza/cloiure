package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.MulHigh;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public final class IntegerMulHighNode extends BinaryArithmeticNode<MulHigh> implements Canonicalizable.BinaryCommutative<ValueNode>
{
    public static final NodeClass<IntegerMulHighNode> TYPE = NodeClass.create(IntegerMulHighNode.class);

    public IntegerMulHighNode(ValueNode x, ValueNode y)
    {
        super(TYPE, ArithmeticOpTable::getMulHigh, x, y);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        Value a = nodeValueMap.operand(getX());
        Value b = nodeValueMap.operand(getY());
        nodeValueMap.setResult(this, gen.emitMulHigh(a, b));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        ValueNode ret = super.canonical(tool, forX, forY);
        if (ret != this)
        {
            return ret;
        }

        if (forX.isConstant() && !forY.isConstant())
        {
            // we try to swap and canonicalize
            ValueNode improvement = canonical(tool, forY, forX);
            if (improvement != this)
            {
                return improvement;
            }
            // if this fails we only swap
            return new IntegerMulHighNode(forY, forX);
        }
        return canonical(this, forY);
    }

    private static ValueNode canonical(IntegerMulHighNode self, ValueNode forY)
    {
        if (forY.isConstant())
        {
            Constant c = forY.asConstant();
            if (c instanceof PrimitiveConstant && ((PrimitiveConstant) c).getJavaKind().isNumericInteger())
            {
                long i = ((PrimitiveConstant) c).asLong();
                if (i == 0 || i == 1)
                {
                    return ConstantNode.forIntegerStamp(self.stamp(NodeView.DEFAULT), 0);
                }
            }
        }
        return self;
    }
}