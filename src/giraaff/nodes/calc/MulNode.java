package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class MulNode
public class MulNode extends BinaryArithmeticNode<ArithmeticOpTable.BinaryOp.Mul> implements NarrowableArithmeticNode, Canonicalizable.BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<MulNode> TYPE = NodeClass.create(MulNode.class);

    // @cons MulNode
    public MulNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons MulNode
    protected MulNode(NodeClass<? extends MulNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, ArithmeticOpTable::getMul, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getMul();
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

        if (__forX.isConstant() && !__forY.isConstant())
        {
            // we try to swap and canonicalize
            ValueNode __improvement = canonical(__tool, __forY, __forX);
            if (__improvement != this)
            {
                return __improvement;
            }
            // if this fails we only swap
            return new MulNode(__forY, __forX);
        }
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul> __op = getOp(__forX, __forY);
        NodeView __view = NodeView.from(__tool);
        return canonical(this, __op, stamp(__view), __forX, __forY, __view);
    }

    private static ValueNode canonical(MulNode __self, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        if (__forY.isConstant())
        {
            Constant __c = __forY.asConstant();
            if (__op.isNeutral(__c))
            {
                return __forX;
            }

            if (__c instanceof PrimitiveConstant && ((PrimitiveConstant) __c).getJavaKind().isNumericInteger())
            {
                long __i = ((PrimitiveConstant) __c).asLong();
                ValueNode __result = canonical(__stamp, __forX, __i, __view);
                if (__result != null)
                {
                    return __result;
                }
            }

            if (__op.isAssociative())
            {
                // canonicalize expressions like "(a * 1) * 2"
                return reassociate(__self != null ? __self : (MulNode) new MulNode(__forX, __forY).maybeCommuteInputs(), ValueNode.isConstantPredicate(), __forX, __forY, __view);
            }
        }
        return __self != null ? __self : new MulNode(__forX, __forY).maybeCommuteInputs();
    }

    public static ValueNode canonical(Stamp __stamp, ValueNode __forX, long __i, NodeView __view)
    {
        if (__i == 0)
        {
            return ConstantNode.forIntegerStamp(__stamp, 0);
        }
        else if (__i == 1)
        {
            return __forX;
        }
        else if (__i == -1)
        {
            return NegateNode.create(__forX, __view);
        }
        else if (__i > 0)
        {
            if (CodeUtil.isPowerOf2(__i))
            {
                return new LeftShiftNode(__forX, ConstantNode.forInt(CodeUtil.log2(__i)));
            }
            else if (CodeUtil.isPowerOf2(__i - 1))
            {
                return AddNode.create(new LeftShiftNode(__forX, ConstantNode.forInt(CodeUtil.log2(__i - 1))), __forX, __view);
            }
            else if (CodeUtil.isPowerOf2(__i + 1))
            {
                return SubNode.create(new LeftShiftNode(__forX, ConstantNode.forInt(CodeUtil.log2(__i + 1))), __forX, __view);
            }
            else
            {
                int __bitCount = Long.bitCount(__i);
                long __highestBitValue = Long.highestOneBit(__i);
                if (__bitCount == 2)
                {
                    // e.g. 0b1000_0010
                    long __lowerBitValue = __i - __highestBitValue;
                    ValueNode __left = new LeftShiftNode(__forX, ConstantNode.forInt(CodeUtil.log2(__highestBitValue)));
                    ValueNode __right = __lowerBitValue == 1 ? __forX : new LeftShiftNode(__forX, ConstantNode.forInt(CodeUtil.log2(__lowerBitValue)));
                    return AddNode.create(__left, __right, __view);
                }
                else
                {
                    // e.g. 0b1111_1101
                    int __shiftToRoundUpToPowerOf2 = CodeUtil.log2(__highestBitValue) + 1;
                    long __subValue = (1 << __shiftToRoundUpToPowerOf2) - __i;
                    if (CodeUtil.isPowerOf2(__subValue) && __shiftToRoundUpToPowerOf2 < ((IntegerStamp) __stamp).getBits())
                    {
                        ValueNode __left = new LeftShiftNode(__forX, ConstantNode.forInt(__shiftToRoundUpToPowerOf2));
                        ValueNode __right = new LeftShiftNode(__forX, ConstantNode.forInt(CodeUtil.log2(__subValue)));
                        return SubNode.create(__left, __right, __view);
                    }
                }
            }
        }
        else if (__i < 0)
        {
            if (CodeUtil.isPowerOf2(-__i))
            {
                return NegateNode.create(LeftShiftNode.create(__forX, ConstantNode.forInt(CodeUtil.log2(-__i)), __view), __view);
            }
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        Value __op1 = __nodeValueMap.operand(getX());
        Value __op2 = __nodeValueMap.operand(getY());
        if (shouldSwapInputs(__nodeValueMap))
        {
            Value __tmp = __op1;
            __op1 = __op2;
            __op2 = __tmp;
        }
        __nodeValueMap.setResult(this, __gen.emitMul(__op1, __op2, false));
    }
}
