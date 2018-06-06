package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class SignedDivNode
public final class SignedDivNode extends IntegerDivRemNode implements LIRLowerable
{
    // @def
    public static final NodeClass<SignedDivNode> TYPE = NodeClass.create(SignedDivNode.class);

    // @cons SignedDivNode
    protected SignedDivNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons SignedDivNode
    protected SignedDivNode(NodeClass<? extends SignedDivNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, IntegerStamp.OPS.getDiv().foldStamp(__x.stamp(NodeView.DEFAULT), __y.stamp(NodeView.DEFAULT)), IntegerDivRemNode.DivRemOp.DIV, IntegerDivRemNode.Signedness.SIGNED, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        return canonical(null, __x, __y, __view);
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(IntegerStamp.OPS.getDiv().foldStamp(getX().stamp(NodeView.DEFAULT), getY().stamp(NodeView.DEFAULT)));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        return canonical(this, __forX, __forY, __view);
    }

    public static ValueNode canonical(SignedDivNode __self, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        Stamp __predictedStamp = IntegerStamp.OPS.getDiv().foldStamp(__forX.stamp(NodeView.DEFAULT), __forY.stamp(NodeView.DEFAULT));
        Stamp __stamp = __self != null ? __self.stamp(__view) : __predictedStamp;
        if (__forX.isConstant() && __forY.isConstant())
        {
            long __y = __forY.asJavaConstant().asLong();
            if (__y == 0)
            {
                return __self != null ? __self : new SignedDivNode(__forX, __forY); // this will trap, can
                                                                            // not canonicalize
            }
            return ConstantNode.forIntegerStamp(__stamp, __forX.asJavaConstant().asLong() / __y);
        }
        else if (__forY.isConstant())
        {
            long __c = __forY.asJavaConstant().asLong();
            ValueNode __v = canonical(__forX, __c, __view);
            if (__v != null)
            {
                return __v;
            }
        }

        // Convert the expression ((a - a % b) / b) into (a / b).
        if (__forX instanceof SubNode)
        {
            SubNode __integerSubNode = (SubNode) __forX;
            if (__integerSubNode.getY() instanceof SignedRemNode)
            {
                SignedRemNode __integerRemNode = (SignedRemNode) __integerSubNode.getY();
                if (__integerSubNode.stamp(__view).isCompatible(__stamp) && __integerRemNode.stamp(__view).isCompatible(__stamp) && __integerSubNode.getX() == __integerRemNode.getX() && __forY == __integerRemNode.getY())
                {
                    SignedDivNode __sd = new SignedDivNode(__integerSubNode.getX(), __forY);
                    __sd.___stateBefore = __self != null ? __self.___stateBefore : null;
                    return __sd;
                }
            }
        }

        if (__self != null && __self.next() instanceof SignedDivNode)
        {
            NodeClass<?> __nodeClass = __self.getNodeClass();
            if (__self.next().getClass() == __self.getClass() && __nodeClass.equalInputs(__self, __self.next()) && __self.valueEquals(__self.next()))
            {
                return __self.next();
            }
        }

        return __self != null ? __self : new SignedDivNode(__forX, __forY);
    }

    public static ValueNode canonical(ValueNode __forX, long __c, NodeView __view)
    {
        if (__c == 1)
        {
            return __forX;
        }
        if (__c == -1)
        {
            return NegateNode.create(__forX, __view);
        }
        long __abs = Math.abs(__c);
        if (CodeUtil.isPowerOf2(__abs) && __forX.stamp(__view) instanceof IntegerStamp)
        {
            ValueNode __dividend = __forX;
            IntegerStamp __stampX = (IntegerStamp) __forX.stamp(__view);
            int __log2 = CodeUtil.log2(__abs);
            // no rounding if dividend is positive or if its low bits are always 0
            if (__stampX.canBeNegative() || (__stampX.upMask() & (__abs - 1)) != 0)
            {
                int __bits = PrimitiveStamp.getBits(__forX.stamp(__view));
                RightShiftNode __sign = new RightShiftNode(__forX, ConstantNode.forInt(__bits - 1));
                UnsignedRightShiftNode __round = new UnsignedRightShiftNode(__sign, ConstantNode.forInt(__bits - __log2));
                __dividend = BinaryArithmeticNode.add(__dividend, __round, __view);
            }
            RightShiftNode __shift = new RightShiftNode(__dividend, ConstantNode.forInt(__log2));
            if (__c < 0)
            {
                return NegateNode.create(__shift, __view);
            }
            return __shift;
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, __gen.getLIRGeneratorTool().getArithmetic().emitDiv(__gen.operand(getX()), __gen.operand(getY()), __gen.state(this)));
    }
}
