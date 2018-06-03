package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.And;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

// @class AndNode
public final class AndNode extends BinaryArithmeticNode<And> implements NarrowableArithmeticNode, BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<AndNode> TYPE = NodeClass.create(AndNode.class);

    // @cons
    public AndNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, ArithmeticOpTable::getAnd, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        BinaryOp<And> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getAnd();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), __y.stamp(__view));
        ConstantNode __tryConstantFold = tryConstantFold(__op, __x, __y, __stamp, __view);
        if (__tryConstantFold != null)
        {
            return __tryConstantFold;
        }
        return canonical(null, __op, __stamp, __x, __y, __view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        NodeView __view = NodeView.from(__tool);
        return canonical(this, getOp(__forX, __forY), stamp(__view), __forX, __forY, __view);
    }

    private static ValueNode canonical(AndNode __self, BinaryOp<And> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY))
        {
            return __forX;
        }
        if (__forX.isConstant() && !__forY.isConstant())
        {
            return new AndNode(__forY, __forX);
        }
        if (__forY.isConstant())
        {
            Constant __c = __forY.asConstant();
            if (__op.isNeutral(__c))
            {
                return __forX;
            }

            if (__c instanceof PrimitiveConstant && ((PrimitiveConstant) __c).getJavaKind().isNumericInteger())
            {
                long __rawY = ((PrimitiveConstant) __c).asLong();
                long __mask = CodeUtil.mask(PrimitiveStamp.getBits(__stamp));
                if ((__rawY & __mask) == 0)
                {
                    return ConstantNode.forIntegerStamp(__stamp, 0);
                }
                if (__forX instanceof SignExtendNode)
                {
                    SignExtendNode __ext = (SignExtendNode) __forX;
                    if (__rawY == ((1L << __ext.getInputBits()) - 1))
                    {
                        return new ZeroExtendNode(__ext.getValue(), __ext.getResultBits());
                    }
                }
                IntegerStamp __xStamp = (IntegerStamp) __forX.stamp(__view);
                if (((__xStamp.upMask() | __xStamp.downMask()) & ~__rawY) == 0)
                {
                    // No bits are set which are outside the mask, so the mask will have no effect.
                    return __forX;
                }
            }

            return reassociate(__self != null ? __self : (AndNode) new AndNode(__forX, __forY).maybeCommuteInputs(), ValueNode.isConstantPredicate(), __forX, __forY, __view);
        }
        if (__forX instanceof NotNode && __forY instanceof NotNode)
        {
            return new NotNode(OrNode.create(((NotNode) __forX).getValue(), ((NotNode) __forY).getValue(), __view));
        }
        return __self != null ? __self : new AndNode(__forX, __forY).maybeCommuteInputs();
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitAnd(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY())));
    }
}
