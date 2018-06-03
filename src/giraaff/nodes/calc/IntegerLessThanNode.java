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
import giraaff.util.GraalError;

// @class IntegerLessThanNode
public final class IntegerLessThanNode extends IntegerLowerThanNode
{
    // @def
    public static final NodeClass<IntegerLessThanNode> TYPE = NodeClass.create(IntegerLessThanNode.class);

    // @def
    private static final LessThanOp OP = new LessThanOp();

    // @cons
    public IntegerLessThanNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, __x, __y, OP);
    }

    public static LogicNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        return OP.create(__x, __y, __view);
    }

    public static LogicNode create(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __value = OP.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, OP.getCondition(), false, __x, __y, __view);
        if (__value != null)
        {
            return __value;
        }
        return create(__x, __y, __view);
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __value = OP.canonical(__tool.getConstantReflection(), __tool.getMetaAccess(), __tool.smallestCompareWidth(), OP.getCondition(), false, __forX, __forY, __view);
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    public static boolean subtractMayUnderflow(long __x, long __y, long __minValue)
    {
        long __r = __x - __y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        return (((__x ^ __y) & (__x ^ __r)) < 0) || __r <= __minValue;
    }

    public static boolean subtractMayOverflow(long __x, long __y, long __maxValue)
    {
        long __r = __x - __y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        return (((__x ^ __y) & (__x ^ __r)) < 0) || __r > __maxValue;
    }

    // @class IntegerLessThanNode.LessThanOp
    public static final class LessThanOp extends LowerOp
    {
        @Override
        protected CompareNode duplicateModified(ValueNode __newX, ValueNode __newY, boolean __unorderedIsTrue, NodeView __view)
        {
            if (__newX.stamp(__view) instanceof FloatStamp && __newY.stamp(__view) instanceof FloatStamp)
            {
                return new FloatLessThanNode(__newX, __newY, __unorderedIsTrue); // TODO Is the last arg supposed to be true?
            }
            else if (__newX.stamp(__view) instanceof IntegerStamp && __newY.stamp(__view) instanceof IntegerStamp)
            {
                return new IntegerLessThanNode(__newX, __newY);
            }
            throw GraalError.shouldNotReachHere();
        }

        @Override
        protected LogicNode optimizeNormalizeCompare(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, Constant __constant, NormalizeCompareNode __normalizeNode, boolean __mirrored, NodeView __view)
        {
            PrimitiveConstant __primitive = (PrimitiveConstant) __constant;
            // a NC b < c  (not mirrored)
            // cases for c:
            //  0         -> a < b
            //  [MIN, -1] -> false
            //  1         -> a <= b
            //  [2, MAX]  -> true
            // unordered-is-less means unordered-is-true.
            //
            // c < a NC b  (mirrored)
            // cases for c:
            //  0         -> a > b
            //  [1, MAX]  -> false
            //  -1        -> a >= b
            //  [MIN, -2] -> true
            // unordered-is-less means unordered-is-false.
            //
            // We can handle mirroring by swapping a & b and negating the constant.
            ValueNode __a = __mirrored ? __normalizeNode.getY() : __normalizeNode.getX();
            ValueNode __b = __mirrored ? __normalizeNode.getX() : __normalizeNode.getY();
            long __cst = __mirrored ? -__primitive.asLong() : __primitive.asLong();

            if (__cst == 0)
            {
                if (__normalizeNode.getX().getStackKind() == JavaKind.Double || __normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatLessThanNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __a, __b, __mirrored ^ __normalizeNode.___isUnorderedLess, __view);
                }
                else
                {
                    return IntegerLessThanNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __a, __b, __view);
                }
            }
            else if (__cst == 1)
            {
                // a <= b <=> !(a > b)
                LogicNode __compare;
                if (__normalizeNode.getX().getStackKind() == JavaKind.Double || __normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    // since we negate, we have to reverse the unordered result
                    __compare = FloatLessThanNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __b, __a, __mirrored == __normalizeNode.___isUnorderedLess, __view);
                }
                else
                {
                    __compare = IntegerLessThanNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __b, __a, __view);
                }
                return LogicNegationNode.create(__compare);
            }
            else if (__cst <= -1)
            {
                return LogicConstantNode.contradiction();
            }
            else
            {
                return LogicConstantNode.tautology();
            }
        }

        @Override
        protected LogicNode findSynonym(ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            LogicNode __result = super.findSynonym(__forX, __forY, __view);
            if (__result != null)
            {
                return __result;
            }
            if (__forX.stamp(__view) instanceof IntegerStamp && __forY.stamp(__view) instanceof IntegerStamp)
            {
                if (IntegerStamp.sameSign((IntegerStamp) __forX.stamp(__view), (IntegerStamp) __forY.stamp(__view)))
                {
                    return new IntegerBelowNode(__forX, __forY);
                }
            }
            if (__forY.isConstant() && __forX instanceof SubNode)
            {
                SubNode __sub = (SubNode) __forX;
                ValueNode __xx = null;
                ValueNode __yy = null;
                boolean __negate = false;
                if (__forY.asConstant().isDefaultForKind())
                {
                    // (x - y) < 0 when x - y is known not to underflow <=> x < y
                    __xx = __sub.getX();
                    __yy = __sub.getY();
                }
                else if (__forY.isJavaConstant() && __forY.asJavaConstant().asLong() == 1)
                {
                    // (x - y) < 1 when x - y is known not to underflow <=> !(y < x)
                    __xx = __sub.getY();
                    __yy = __sub.getX();
                    __negate = true;
                }
                if (__xx != null)
                {
                    IntegerStamp __xStamp = (IntegerStamp) __sub.getX().stamp(__view);
                    IntegerStamp __yStamp = (IntegerStamp) __sub.getY().stamp(__view);
                    long __minValue = CodeUtil.minValue(__xStamp.getBits());
                    long __maxValue = CodeUtil.maxValue(__xStamp.getBits());

                    if (!subtractMayUnderflow(__xStamp.lowerBound(), __yStamp.upperBound(), __minValue) && !subtractMayOverflow(__xStamp.upperBound(), __yStamp.lowerBound(), __maxValue))
                    {
                        LogicNode __logic = new IntegerLessThanNode(__xx, __yy);
                        if (__negate)
                        {
                            __logic = LogicNegationNode.create(__logic);
                        }
                        return __logic;
                    }
                }
            }

            if (__forX.stamp(__view) instanceof IntegerStamp)
            {
                int __bits = ((IntegerStamp) __forX.stamp(__view)).getBits();
                long __min = OP.minValue(__bits);
                long __xResidue = 0;
                ValueNode __left = null;
                JavaConstant __leftCst = null;
                if (__forX instanceof AddNode)
                {
                    AddNode __xAdd = (AddNode) __forX;
                    if (__xAdd.getY().isJavaConstant())
                    {
                        long __xCst = __xAdd.getY().asJavaConstant().asLong();
                        __xResidue = __xCst - __min;
                        __left = __xAdd.getX();
                    }
                }
                else if (__forX.isJavaConstant())
                {
                    __leftCst = __forX.asJavaConstant();
                }
                if (__left != null || __leftCst != null)
                {
                    long __yResidue = 0;
                    ValueNode __right = null;
                    JavaConstant __rightCst = null;
                    if (__forY instanceof AddNode)
                    {
                        AddNode __yAdd = (AddNode) __forY;
                        if (__yAdd.getY().isJavaConstant())
                        {
                            long __yCst = __yAdd.getY().asJavaConstant().asLong();
                            __yResidue = __yCst - __min;
                            __right = __yAdd.getX();
                        }
                    }
                    else if (__forY.isJavaConstant())
                    {
                        __rightCst = __forY.asJavaConstant();
                    }
                    if (__right != null || __rightCst != null)
                    {
                        if ((__xResidue == 0 && __left != null) || (__yResidue == 0 && __right != null))
                        {
                            if (__left == null)
                            {
                                __left = ConstantNode.forIntegerBits(__bits, __leftCst.asLong() - __min);
                            }
                            else if (__xResidue != 0)
                            {
                                __left = AddNode.create(__left, ConstantNode.forIntegerBits(__bits, __xResidue), __view);
                            }
                            if (__right == null)
                            {
                                __right = ConstantNode.forIntegerBits(__bits, __rightCst.asLong() - __min);
                            }
                            else if (__yResidue != 0)
                            {
                                __right = AddNode.create(__right, ConstantNode.forIntegerBits(__bits, __yResidue), __view);
                            }
                            return new IntegerBelowNode(__left, __right);
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
        protected IntegerLowerThanNode createNode(ValueNode __x, ValueNode __y)
        {
            return new IntegerLessThanNode(__x, __y);
        }

        @Override
        protected long upperBound(IntegerStamp __stamp)
        {
            return __stamp.upperBound();
        }

        @Override
        protected long lowerBound(IntegerStamp __stamp)
        {
            return __stamp.lowerBound();
        }

        @Override
        protected int compare(long __a, long __b)
        {
            return Long.compare(__a, __b);
        }

        @Override
        protected long min(long __a, long __b)
        {
            return Math.min(__a, __b);
        }

        @Override
        protected long max(long __a, long __b)
        {
            return Math.max(__a, __b);
        }

        @Override
        protected long cast(long __a, int __bits)
        {
            return CodeUtil.signExtend(__a, __bits);
        }

        @Override
        protected long minValue(int __bits)
        {
            return NumUtil.minValue(__bits);
        }

        @Override
        protected long maxValue(int __bits)
        {
            return NumUtil.maxValue(__bits);
        }

        @Override
        protected IntegerStamp forInteger(int __bits, long __min, long __max)
        {
            return StampFactory.forInteger(__bits, cast(__min, __bits), cast(__max, __bits));
        }
    }
}
