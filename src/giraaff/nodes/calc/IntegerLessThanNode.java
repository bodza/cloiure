package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.core.common.NumUtil;
import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNegationNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.options.OptionValues;
import giraaff.util.GraalError;

public final class IntegerLessThanNode extends IntegerLowerThanNode
{
    public static final NodeClass<IntegerLessThanNode> TYPE = NodeClass.create(IntegerLessThanNode.class);

    private static final LessThanOp OP = new LessThanOp();

    public IntegerLessThanNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y, OP);
    }

    public static LogicNode create(ValueNode x, ValueNode y, NodeView view)
    {
        return OP.create(x, y, view);
    }

    public static LogicNode create(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode value = OP.canonical(constantReflection, metaAccess, options, smallestCompareWidth, OP.getCondition(), false, x, y, view);
        if (value != null)
        {
            return value;
        }
        return create(x, y, view);
    }

    @Override
    public Node canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode value = OP.canonical(tool.getConstantReflection(), tool.getMetaAccess(), tool.getOptions(), tool.smallestCompareWidth(), OP.getCondition(), false, forX, forY, view);
        if (value != null)
        {
            return value;
        }
        return this;
    }

    public static boolean subtractMayUnderflow(long x, long y, long minValue)
    {
        long r = x - y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        return (((x ^ y) & (x ^ r)) < 0) || r <= minValue;
    }

    public static boolean subtractMayOverflow(long x, long y, long maxValue)
    {
        long r = x - y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        return (((x ^ y) & (x ^ r)) < 0) || r > maxValue;
    }

    public static class LessThanOp extends LowerOp
    {
        @Override
        protected CompareNode duplicateModified(ValueNode newX, ValueNode newY, boolean unorderedIsTrue, NodeView view)
        {
            if (newX.stamp(view) instanceof FloatStamp && newY.stamp(view) instanceof FloatStamp)
            {
                return new FloatLessThanNode(newX, newY, unorderedIsTrue); // TODO Is the last arg supposed to be true?
            }
            else if (newX.stamp(view) instanceof IntegerStamp && newY.stamp(view) instanceof IntegerStamp)
            {
                return new IntegerLessThanNode(newX, newY);
            }
            throw GraalError.shouldNotReachHere();
        }

        @Override
        protected LogicNode optimizeNormalizeCompare(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, Constant constant, NormalizeCompareNode normalizeNode, boolean mirrored, NodeView view)
        {
            PrimitiveConstant primitive = (PrimitiveConstant) constant;
            /*
             * a NC b < c  (not mirrored)
             * cases for c:
             *  0         -> a < b
             *  [MIN, -1] -> false
             *  1         -> a <= b
             *  [2, MAX]  -> true
             * unordered-is-less means unordered-is-true.
             *
             * c < a NC b  (mirrored)
             * cases for c:
             *  0         -> a > b
             *  [1, MAX]  -> false
             *  -1        -> a >= b
             *  [MIN, -2] -> true
             * unordered-is-less means unordered-is-false.
             *
             * We can handle mirroring by swapping a & b and negating the constant.
             */
            ValueNode a = mirrored ? normalizeNode.getY() : normalizeNode.getX();
            ValueNode b = mirrored ? normalizeNode.getX() : normalizeNode.getY();
            long cst = mirrored ? -primitive.asLong() : primitive.asLong();

            if (cst == 0)
            {
                if (normalizeNode.getX().getStackKind() == JavaKind.Double || normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatLessThanNode.create(constantReflection, metaAccess, options, smallestCompareWidth, a, b, mirrored ^ normalizeNode.isUnorderedLess, view);
                }
                else
                {
                    return IntegerLessThanNode.create(constantReflection, metaAccess, options, smallestCompareWidth, a, b, view);
                }
            }
            else if (cst == 1)
            {
                // a <= b <=> !(a > b)
                LogicNode compare;
                if (normalizeNode.getX().getStackKind() == JavaKind.Double || normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    // since we negate, we have to reverse the unordered result
                    compare = FloatLessThanNode.create(constantReflection, metaAccess, options, smallestCompareWidth, b, a, mirrored == normalizeNode.isUnorderedLess, view);
                }
                else
                {
                    compare = IntegerLessThanNode.create(constantReflection, metaAccess, options, smallestCompareWidth, b, a, view);
                }
                return LogicNegationNode.create(compare);
            }
            else if (cst <= -1)
            {
                return LogicConstantNode.contradiction();
            }
            else
            {
                return LogicConstantNode.tautology();
            }
        }

        @Override
        protected LogicNode findSynonym(ValueNode forX, ValueNode forY, NodeView view)
        {
            LogicNode result = super.findSynonym(forX, forY, view);
            if (result != null)
            {
                return result;
            }
            if (forX.stamp(view) instanceof IntegerStamp && forY.stamp(view) instanceof IntegerStamp)
            {
                if (IntegerStamp.sameSign((IntegerStamp) forX.stamp(view), (IntegerStamp) forY.stamp(view)))
                {
                    return new IntegerBelowNode(forX, forY);
                }
            }
            if (forY.isConstant() && forX instanceof SubNode)
            {
                SubNode sub = (SubNode) forX;
                ValueNode xx = null;
                ValueNode yy = null;
                boolean negate = false;
                if (forY.asConstant().isDefaultForKind())
                {
                    // (x - y) < 0 when x - y is known not to underflow <=> x < y
                    xx = sub.getX();
                    yy = sub.getY();
                }
                else if (forY.isJavaConstant() && forY.asJavaConstant().asLong() == 1)
                {
                    // (x - y) < 1 when x - y is known not to underflow <=> !(y < x)
                    xx = sub.getY();
                    yy = sub.getX();
                    negate = true;
                }
                if (xx != null)
                {
                    IntegerStamp xStamp = (IntegerStamp) sub.getX().stamp(view);
                    IntegerStamp yStamp = (IntegerStamp) sub.getY().stamp(view);
                    long minValue = CodeUtil.minValue(xStamp.getBits());
                    long maxValue = CodeUtil.maxValue(xStamp.getBits());

                    if (!subtractMayUnderflow(xStamp.lowerBound(), yStamp.upperBound(), minValue) && !subtractMayOverflow(xStamp.upperBound(), yStamp.lowerBound(), maxValue))
                    {
                        LogicNode logic = new IntegerLessThanNode(xx, yy);
                        if (negate)
                        {
                            logic = LogicNegationNode.create(logic);
                        }
                        return logic;
                    }
                }
            }

            if (forX.stamp(view) instanceof IntegerStamp)
            {
                int bits = ((IntegerStamp) forX.stamp(view)).getBits();
                long min = OP.minValue(bits);
                long xResidue = 0;
                ValueNode left = null;
                JavaConstant leftCst = null;
                if (forX instanceof AddNode)
                {
                    AddNode xAdd = (AddNode) forX;
                    if (xAdd.getY().isJavaConstant())
                    {
                        long xCst = xAdd.getY().asJavaConstant().asLong();
                        xResidue = xCst - min;
                        left = xAdd.getX();
                    }
                }
                else if (forX.isJavaConstant())
                {
                    leftCst = forX.asJavaConstant();
                }
                if (left != null || leftCst != null)
                {
                    long yResidue = 0;
                    ValueNode right = null;
                    JavaConstant rightCst = null;
                    if (forY instanceof AddNode)
                    {
                        AddNode yAdd = (AddNode) forY;
                        if (yAdd.getY().isJavaConstant())
                        {
                            long yCst = yAdd.getY().asJavaConstant().asLong();
                            yResidue = yCst - min;
                            right = yAdd.getX();
                        }
                    }
                    else if (forY.isJavaConstant())
                    {
                        rightCst = forY.asJavaConstant();
                    }
                    if (right != null || rightCst != null)
                    {
                        if ((xResidue == 0 && left != null) || (yResidue == 0 && right != null))
                        {
                            if (left == null)
                            {
                                left = ConstantNode.forIntegerBits(bits, leftCst.asLong() - min);
                            }
                            else if (xResidue != 0)
                            {
                                left = AddNode.create(left, ConstantNode.forIntegerBits(bits, xResidue), view);
                            }
                            if (right == null)
                            {
                                right = ConstantNode.forIntegerBits(bits, rightCst.asLong() - min);
                            }
                            else if (yResidue != 0)
                            {
                                right = AddNode.create(right, ConstantNode.forIntegerBits(bits, yResidue), view);
                            }
                            return new IntegerBelowNode(left, right);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected CanonicalCondition getCondition()
        {
            return CanonicalCondition.LT;
        }

        @Override
        protected IntegerLowerThanNode createNode(ValueNode x, ValueNode y)
        {
            return new IntegerLessThanNode(x, y);
        }

        @Override
        protected long upperBound(IntegerStamp stamp)
        {
            return stamp.upperBound();
        }

        @Override
        protected long lowerBound(IntegerStamp stamp)
        {
            return stamp.lowerBound();
        }

        @Override
        protected int compare(long a, long b)
        {
            return Long.compare(a, b);
        }

        @Override
        protected long min(long a, long b)
        {
            return Math.min(a, b);
        }

        @Override
        protected long max(long a, long b)
        {
            return Math.max(a, b);
        }

        @Override
        protected long cast(long a, int bits)
        {
            return CodeUtil.signExtend(a, bits);
        }

        @Override
        protected long minValue(int bits)
        {
            return NumUtil.minValue(bits);
        }

        @Override
        protected long maxValue(int bits)
        {
            return NumUtil.maxValue(bits);
        }

        @Override
        protected IntegerStamp forInteger(int bits, long min, long max)
        {
            return StampFactory.forInteger(bits, cast(min, bits), cast(max, bits));
        }
    }
}
