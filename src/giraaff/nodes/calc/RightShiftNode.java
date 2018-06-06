package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class RightShiftNode
public final class RightShiftNode extends ShiftNode<ArithmeticOpTable.ShiftOp.Shr>
{
    // @def
    public static final NodeClass<RightShiftNode> TYPE = NodeClass.create(RightShiftNode.class);

    // @cons RightShiftNode
    public RightShiftNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, ArithmeticOpTable::getShr, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shr> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getShr();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), (IntegerStamp) __y.stamp(__view));
        ValueNode __value = ShiftNode.canonical(__op, __stamp, __x, __y, __view);
        if (__value != null)
        {
            return __value;
        }

        return canonical(null, __op, __stamp, __x, __y, __view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        return canonical(this, getArithmeticOp(), stamp(__view), __forX, __forY, __view);
    }

    private static ValueNode canonical(RightShiftNode __rightShiftNode, ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shr> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        RightShiftNode __self = __rightShiftNode;
        if (__forX.stamp(__view) instanceof IntegerStamp && ((IntegerStamp) __forX.stamp(__view)).isPositive())
        {
            return new UnsignedRightShiftNode(__forX, __forY);
        }

        if (__forY.isConstant())
        {
            int __amount = __forY.asJavaConstant().asInt();
            int __originalAmout = __amount;
            int __mask = __op.getShiftAmountMask(__stamp);
            __amount &= __mask;
            if (__amount == 0)
            {
                return __forX;
            }
            if (__forX instanceof ShiftNode)
            {
                ShiftNode<?> __other = (ShiftNode<?>) __forX;
                if (__other.getY().isConstant())
                {
                    int __otherAmount = __other.getY().asJavaConstant().asInt() & __mask;
                    if (__other instanceof RightShiftNode)
                    {
                        int __total = __amount + __otherAmount;
                        if (__total != (__total & __mask))
                        {
                            IntegerStamp __istamp = (IntegerStamp) __other.getX().stamp(__view);

                            if (__istamp.isPositive())
                            {
                                return ConstantNode.forIntegerKind(__stamp.getStackKind(), 0);
                            }
                            if (__istamp.isStrictlyNegative())
                            {
                                return ConstantNode.forIntegerKind(__stamp.getStackKind(), -1L);
                            }

                            // if we cannot replace both shifts with a constant, replace them by a full shift for this kind
                            return new RightShiftNode(__other.getX(), ConstantNode.forInt(__mask));
                        }
                        return new RightShiftNode(__other.getX(), ConstantNode.forInt(__total));
                    }
                }
            }
            if (__originalAmout != __amount)
            {
                return new RightShiftNode(__forX, ConstantNode.forInt(__amount));
            }
        }
        if (__self == null)
        {
            __self = new RightShiftNode(__forX, __forY);
        }
        return __self;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitShr(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY())));
    }

    @Override
    public boolean isNarrowable(int __resultBits)
    {
        if (super.isNarrowable(__resultBits))
        {
            // For signed right shifts, the narrow can be done before the shift if the cut off bits
            // are all equal to the sign bit of the input. That's equivalent to the condition that
            // the input is in the signed range of the narrow type.
            IntegerStamp __inputStamp = (IntegerStamp) getX().stamp(NodeView.DEFAULT);
            return CodeUtil.minValue(__resultBits) <= __inputStamp.lowerBound() && __inputStamp.upperBound() <= CodeUtil.maxValue(__resultBits);
        }
        else
        {
            return false;
        }
    }
}
