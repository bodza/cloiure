package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class IntegerMulHighNode
public final class IntegerMulHighNode extends BinaryArithmeticNode<ArithmeticOpTable.BinaryOp.MulHigh> implements Canonicalizable.BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<IntegerMulHighNode> TYPE = NodeClass.create(IntegerMulHighNode.class);

    // @cons IntegerMulHighNode
    public IntegerMulHighNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, ArithmeticOpTable::getMulHigh, __x, __y);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        Value __a = __nodeValueMap.operand(getX());
        Value __b = __nodeValueMap.operand(getY());
        __nodeValueMap.setResult(this, __gen.emitMulHigh(__a, __b));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        if (__forX.isConstant() && !__forY.isConstant())
        {
            // we try to swap and canonicalize
            ValueNode __improvement = canonical(__tool, __forY, __forX);
            if (__improvement != this)
            {
                return __improvement;
            }
            // if this fails we only swap
            return new IntegerMulHighNode(__forY, __forX);
        }
        return canonical(this, __forY);
    }

    private static ValueNode canonical(IntegerMulHighNode __self, ValueNode __forY)
    {
        if (__forY.isConstant())
        {
            Constant __c = __forY.asConstant();
            if (__c instanceof PrimitiveConstant && ((PrimitiveConstant) __c).getJavaKind().isNumericInteger())
            {
                long __i = ((PrimitiveConstant) __c).asLong();
                if (__i == 0 || __i == 1)
                {
                    return ConstantNode.forIntegerStamp(__self.stamp(NodeView.DEFAULT), 0);
                }
            }
        }
        return __self;
    }
}
