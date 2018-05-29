package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Mul;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class MulNode
public class MulNode extends BinaryArithmeticNode<Mul> implements NarrowableArithmeticNode, BinaryCommutative<ValueNode>
{
    public static final NodeClass<MulNode> TYPE = NodeClass.create(MulNode.class);

    // @cons
    public MulNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

    // @cons
    protected MulNode(NodeClass<? extends MulNode> c, ValueNode x, ValueNode y)
    {
        super(c, ArithmeticOpTable::getMul, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        BinaryOp<Mul> op = ArithmeticOpTable.forStamp(x.stamp(view)).getMul();
        Stamp stamp = op.foldStamp(x.stamp(view), y.stamp(view));
        ConstantNode tryConstantFold = tryConstantFold(op, x, y, stamp, view);
        if (tryConstantFold != null)
        {
            return tryConstantFold;
        }
        return canonical(null, op, stamp, x, y, view);
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
            return new MulNode(forY, forX);
        }
        BinaryOp<Mul> op = getOp(forX, forY);
        NodeView view = NodeView.from(tool);
        return canonical(this, op, stamp(view), forX, forY, view);
    }

    private static ValueNode canonical(MulNode self, BinaryOp<Mul> op, Stamp stamp, ValueNode forX, ValueNode forY, NodeView view)
    {
        if (forY.isConstant())
        {
            Constant c = forY.asConstant();
            if (op.isNeutral(c))
            {
                return forX;
            }

            if (c instanceof PrimitiveConstant && ((PrimitiveConstant) c).getJavaKind().isNumericInteger())
            {
                long i = ((PrimitiveConstant) c).asLong();
                ValueNode result = canonical(stamp, forX, i, view);
                if (result != null)
                {
                    return result;
                }
            }

            if (op.isAssociative())
            {
                // canonicalize expressions like "(a * 1) * 2"
                return reassociate(self != null ? self : (MulNode) new MulNode(forX, forY).maybeCommuteInputs(), ValueNode.isConstantPredicate(), forX, forY, view);
            }
        }
        return self != null ? self : new MulNode(forX, forY).maybeCommuteInputs();
    }

    public static ValueNode canonical(Stamp stamp, ValueNode forX, long i, NodeView view)
    {
        if (i == 0)
        {
            return ConstantNode.forIntegerStamp(stamp, 0);
        }
        else if (i == 1)
        {
            return forX;
        }
        else if (i == -1)
        {
            return NegateNode.create(forX, view);
        }
        else if (i > 0)
        {
            if (CodeUtil.isPowerOf2(i))
            {
                return new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(i)));
            }
            else if (CodeUtil.isPowerOf2(i - 1))
            {
                return AddNode.create(new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(i - 1))), forX, view);
            }
            else if (CodeUtil.isPowerOf2(i + 1))
            {
                return SubNode.create(new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(i + 1))), forX, view);
            }
            else
            {
                int bitCount = Long.bitCount(i);
                long highestBitValue = Long.highestOneBit(i);
                if (bitCount == 2)
                {
                    // e.g. 0b1000_0010
                    long lowerBitValue = i - highestBitValue;
                    ValueNode left = new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(highestBitValue)));
                    ValueNode right = lowerBitValue == 1 ? forX : new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(lowerBitValue)));
                    return AddNode.create(left, right, view);
                }
                else
                {
                    // e.g. 0b1111_1101
                    int shiftToRoundUpToPowerOf2 = CodeUtil.log2(highestBitValue) + 1;
                    long subValue = (1 << shiftToRoundUpToPowerOf2) - i;
                    if (CodeUtil.isPowerOf2(subValue) && shiftToRoundUpToPowerOf2 < ((IntegerStamp) stamp).getBits())
                    {
                        ValueNode left = new LeftShiftNode(forX, ConstantNode.forInt(shiftToRoundUpToPowerOf2));
                        ValueNode right = new LeftShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(subValue)));
                        return SubNode.create(left, right, view);
                    }
                }
            }
        }
        else if (i < 0)
        {
            if (CodeUtil.isPowerOf2(-i))
            {
                return NegateNode.create(LeftShiftNode.create(forX, ConstantNode.forInt(CodeUtil.log2(-i)), view), view);
            }
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        Value op1 = nodeValueMap.operand(getX());
        Value op2 = nodeValueMap.operand(getY());
        if (shouldSwapInputs(nodeValueMap))
        {
            Value tmp = op1;
            op1 = op2;
            op2 = tmp;
        }
        nodeValueMap.setResult(this, gen.emitMul(op1, op2, false));
    }
}
