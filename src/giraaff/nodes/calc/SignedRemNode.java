package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class SignedRemNode
public final class SignedRemNode extends IntegerDivRemNode implements LIRLowerable
{
    // @def
    public static final NodeClass<SignedRemNode> TYPE = NodeClass.create(SignedRemNode.class);

    // @cons SignedRemNode
    protected SignedRemNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons SignedRemNode
    protected SignedRemNode(NodeClass<? extends SignedRemNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, IntegerStamp.OPS.getRem().foldStamp(__x.stamp(NodeView.DEFAULT), __y.stamp(NodeView.DEFAULT)), IntegerDivRemNode.DivRemOp.REM, IntegerDivRemNode.Signedness.SIGNED, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        Stamp __stamp = IntegerStamp.OPS.getRem().foldStamp(__x.stamp(__view), __y.stamp(__view));
        return canonical(null, __x, __y, __stamp, __view);
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(IntegerStamp.OPS.getRem().foldStamp(getX().stamp(NodeView.DEFAULT), getY().stamp(NodeView.DEFAULT)));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        return canonical(this, __forX, __forY, stamp(__view), __view);
    }

    private static ValueNode canonical(SignedRemNode __self, ValueNode __forX, ValueNode __forY, Stamp __stamp, NodeView __view)
    {
        if (__forX.isConstant() && __forY.isConstant())
        {
            long __y = __forY.asJavaConstant().asLong();
            if (__y == 0)
            {
                return __self != null ? __self : new SignedRemNode(__forX, __forY); // this will trap, can
                                                                            // not canonicalize
            }
            return ConstantNode.forIntegerStamp(__stamp, __forX.asJavaConstant().asLong() % __y);
        }
        else if (__forY.isConstant() && __forX.stamp(__view) instanceof IntegerStamp && __forY.stamp(__view) instanceof IntegerStamp)
        {
            long __constY = __forY.asJavaConstant().asLong();
            IntegerStamp __xStamp = (IntegerStamp) __forX.stamp(__view);
            IntegerStamp __yStamp = (IntegerStamp) __forY.stamp(__view);
            if (__constY < 0 && __constY != CodeUtil.minValue(__yStamp.getBits()))
            {
                Stamp __newStamp = IntegerStamp.OPS.getRem().foldStamp(__forX.stamp(__view), __forY.stamp(__view));
                return canonical(null, __forX, ConstantNode.forIntegerStamp(__yStamp, -__constY), __newStamp, __view);
            }

            if (__constY == 1)
            {
                return ConstantNode.forIntegerStamp(__stamp, 0);
            }
            else if (CodeUtil.isPowerOf2(__constY))
            {
                if (__xStamp.isPositive())
                {
                    // x & (y - 1)
                    return new AndNode(__forX, ConstantNode.forIntegerStamp(__stamp, __constY - 1));
                }
                else if (__xStamp.isNegative())
                {
                    // -((-x) & (y - 1))
                    return new NegateNode(new AndNode(new NegateNode(__forX), ConstantNode.forIntegerStamp(__stamp, __constY - 1)));
                }
                else
                {
                    // x - ((x / y) << log2(y))
                    return SubNode.create(__forX, LeftShiftNode.create(SignedDivNode.canonical(__forX, __constY, __view), ConstantNode.forInt(CodeUtil.log2(__constY)), __view), __view);
                }
            }
        }
        return __self != null ? __self : new SignedRemNode(__forX, __forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, __gen.getLIRGeneratorTool().getArithmetic().emitRem(__gen.operand(getX()), __gen.operand(getY()), __gen.state(this)));
    }
}
