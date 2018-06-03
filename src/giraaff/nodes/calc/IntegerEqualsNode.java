package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNegationNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

// @class IntegerEqualsNode
public final class IntegerEqualsNode extends CompareNode implements BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<IntegerEqualsNode> TYPE = NodeClass.create(IntegerEqualsNode.class);

    // @def
    private static final IntegerEqualsOp OP = new IntegerEqualsOp();

    // @cons
    public IntegerEqualsNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, CanonicalCondition.EQ, false, __x, __y);
    }

    public static LogicNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __result = CompareNode.tryConstantFoldPrimitive(CanonicalCondition.EQ, __x, __y, false, __view);
        if (__result != null)
        {
            return __result;
        }
        if (__x instanceof ConditionalNode)
        {
            ConditionalNode __conditionalNode = (ConditionalNode) __x;
            if (__conditionalNode.trueValue() == __y)
            {
                return __conditionalNode.condition();
            }
            if (__conditionalNode.falseValue() == __y)
            {
                return LogicNegationNode.create(__conditionalNode.condition());
            }
        }
        else if (__y instanceof ConditionalNode)
        {
            ConditionalNode __conditionalNode = (ConditionalNode) __y;
            if (__conditionalNode.trueValue() == __x)
            {
                return __conditionalNode.condition();
            }
            if (__conditionalNode.falseValue() == __x)
            {
                return LogicNegationNode.create(__conditionalNode.condition());
            }
        }
        return new IntegerEqualsNode(__x, __y).maybeCommuteInputs();
    }

    public static LogicNode create(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __value = OP.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, CanonicalCondition.EQ, false, __x, __y, __view);
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
        ValueNode __value = OP.canonical(__tool.getConstantReflection(), __tool.getMetaAccess(), __tool.smallestCompareWidth(), CanonicalCondition.EQ, false, __forX, __forY, __view);
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    // @class IntegerEqualsNode.IntegerEqualsOp
    public static final class IntegerEqualsOp extends CompareOp
    {
        @Override
        protected LogicNode optimizeNormalizeCompare(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, Constant __constant, NormalizeCompareNode __normalizeNode, boolean __mirrored, NodeView __view)
        {
            PrimitiveConstant __primitive = (PrimitiveConstant) __constant;
            ValueNode __a = __normalizeNode.getX();
            ValueNode __b = __normalizeNode.getY();
            long __cst = __primitive.asLong();

            if (__cst == 0)
            {
                if (__normalizeNode.getX().getStackKind() == JavaKind.Double || __normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatEqualsNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __a, __b, __view);
                }
                else
                {
                    return IntegerEqualsNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __a, __b, __view);
                }
            }
            else if (__cst == 1)
            {
                if (__normalizeNode.getX().getStackKind() == JavaKind.Double || __normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatLessThanNode.create(__b, __a, !__normalizeNode.___isUnorderedLess, __view);
                }
                else
                {
                    return IntegerLessThanNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __b, __a, __view);
                }
            }
            else if (__cst == -1)
            {
                if (__normalizeNode.getX().getStackKind() == JavaKind.Double || __normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatLessThanNode.create(__a, __b, __normalizeNode.___isUnorderedLess, __view);
                }
                else
                {
                    return IntegerLessThanNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __a, __b, __view);
                }
            }
            else
            {
                return LogicConstantNode.contradiction();
            }
        }

        @Override
        protected CompareNode duplicateModified(ValueNode __newX, ValueNode __newY, boolean __unorderedIsTrue, NodeView __view)
        {
            if (__newX.stamp(__view) instanceof FloatStamp && __newY.stamp(__view) instanceof FloatStamp)
            {
                return new FloatEqualsNode(__newX, __newY);
            }
            else if (__newX.stamp(__view) instanceof IntegerStamp && __newY.stamp(__view) instanceof IntegerStamp)
            {
                return new IntegerEqualsNode(__newX, __newY);
            }
            else if (__newX.stamp(__view) instanceof AbstractPointerStamp && __newY.stamp(__view) instanceof AbstractPointerStamp)
            {
                return new IntegerEqualsNode(__newX, __newY);
            }
            throw GraalError.shouldNotReachHere();
        }

        @Override
        public LogicNode canonical(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, boolean __unorderedIsTrue, ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY))
            {
                return LogicConstantNode.tautology();
            }
            else if (__forX.stamp(__view).alwaysDistinct(__forY.stamp(__view)))
            {
                return LogicConstantNode.contradiction();
            }

            if (__forX instanceof AddNode && __forY instanceof AddNode)
            {
                AddNode __addX = (AddNode) __forX;
                AddNode __addY = (AddNode) __forY;
                ValueNode __v1 = null;
                ValueNode __v2 = null;
                if (__addX.getX() == __addY.getX())
                {
                    __v1 = __addX.getY();
                    __v2 = __addY.getY();
                }
                else if (__addX.getX() == __addY.getY())
                {
                    __v1 = __addX.getY();
                    __v2 = __addY.getX();
                }
                else if (__addX.getY() == __addY.getX())
                {
                    __v1 = __addX.getX();
                    __v2 = __addY.getY();
                }
                else if (__addX.getY() == __addY.getY())
                {
                    __v1 = __addX.getX();
                    __v2 = __addY.getX();
                }
                if (__v1 != null)
                {
                    return create(__v1, __v2, __view);
                }
            }

            return super.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __unorderedIsTrue, __forX, __forY, __view);
        }

        @Override
        protected LogicNode canonicalizeSymmetricConstant(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, Constant __constant, ValueNode __nonConstant, boolean __mirrored, boolean __unorderedIsTrue, NodeView __view)
        {
            if (__constant instanceof PrimitiveConstant)
            {
                PrimitiveConstant __primitiveConstant = (PrimitiveConstant) __constant;
                IntegerStamp __nonConstantStamp = ((IntegerStamp) __nonConstant.stamp(__view));
                if ((__primitiveConstant.asLong() == 1 && __nonConstantStamp.upperBound() == 1 && __nonConstantStamp.lowerBound() == 0) || (__primitiveConstant.asLong() == -1 && __nonConstantStamp.upperBound() == 0 && __nonConstantStamp.lowerBound() == -1))
                {
                    // nonConstant can only be 0 or 1 (respective -1), test against 0 instead of 1
                    // (respective -1) for a more canonical graph and also to allow for faster
                    // execution
                    // on specific platforms.
                    return LogicNegationNode.create(IntegerEqualsNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __nonConstant, ConstantNode.forIntegerKind(__nonConstant.getStackKind(), 0), __view));
                }
                else if (__primitiveConstant.asLong() == 0)
                {
                    if (__nonConstant instanceof AndNode)
                    {
                        AndNode __andNode = (AndNode) __nonConstant;
                        return new IntegerTestNode(__andNode.getX(), __andNode.getY());
                    }
                    else if (__nonConstant instanceof SubNode)
                    {
                        SubNode __subNode = (SubNode) __nonConstant;
                        return IntegerEqualsNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __subNode.getX(), __subNode.getY(), __view);
                    }
                    else if (__nonConstant instanceof ShiftNode && __nonConstant.stamp(__view) instanceof IntegerStamp)
                    {
                        if (__nonConstant instanceof LeftShiftNode)
                        {
                            LeftShiftNode __shift = (LeftShiftNode) __nonConstant;
                            if (__shift.getY().isConstant())
                            {
                                int __mask = __shift.getShiftAmountMask();
                                int __amount = __shift.getY().asJavaConstant().asInt() & __mask;
                                if (__shift.getX().getStackKind() == JavaKind.Int)
                                {
                                    return new IntegerTestNode(__shift.getX(), ConstantNode.forInt(-1 >>> __amount));
                                }
                                else
                                {
                                    return new IntegerTestNode(__shift.getX(), ConstantNode.forLong(-1L >>> __amount));
                                }
                            }
                        }
                        else if (__nonConstant instanceof RightShiftNode)
                        {
                            RightShiftNode __shift = (RightShiftNode) __nonConstant;
                            if (__shift.getY().isConstant() && ((IntegerStamp) __shift.getX().stamp(__view)).isPositive())
                            {
                                int __mask = __shift.getShiftAmountMask();
                                int __amount = __shift.getY().asJavaConstant().asInt() & __mask;
                                if (__shift.getX().getStackKind() == JavaKind.Int)
                                {
                                    return new IntegerTestNode(__shift.getX(), ConstantNode.forInt(-1 << __amount));
                                }
                                else
                                {
                                    return new IntegerTestNode(__shift.getX(), ConstantNode.forLong(-1L << __amount));
                                }
                            }
                        }
                        else if (__nonConstant instanceof UnsignedRightShiftNode)
                        {
                            UnsignedRightShiftNode __shift = (UnsignedRightShiftNode) __nonConstant;
                            if (__shift.getY().isConstant())
                            {
                                int __mask = __shift.getShiftAmountMask();
                                int __amount = __shift.getY().asJavaConstant().asInt() & __mask;
                                if (__shift.getX().getStackKind() == JavaKind.Int)
                                {
                                    return new IntegerTestNode(__shift.getX(), ConstantNode.forInt(-1 << __amount));
                                }
                                else
                                {
                                    return new IntegerTestNode(__shift.getX(), ConstantNode.forLong(-1L << __amount));
                                }
                            }
                        }
                    }
                }
                if (__nonConstant instanceof AddNode)
                {
                    AddNode __addNode = (AddNode) __nonConstant;
                    if (__addNode.getY().isJavaConstant())
                    {
                        return new IntegerEqualsNode(__addNode.getX(), ConstantNode.forIntegerStamp(__nonConstantStamp, __primitiveConstant.asLong() - __addNode.getY().asJavaConstant().asLong()));
                    }
                }
                if (__nonConstant instanceof AndNode)
                {
                    // a & c == c is the same as a & c != 0, if c is a single bit
                    AndNode __andNode = (AndNode) __nonConstant;
                    if (Long.bitCount(((PrimitiveConstant) __constant).asLong()) == 1 && __andNode.getY().isConstant() && __andNode.getY().asJavaConstant().equals(__constant))
                    {
                        return new LogicNegationNode(new IntegerTestNode(__andNode.getX(), __andNode.getY()));
                    }
                }

                if (__nonConstant instanceof XorNode && __nonConstant.stamp(__view) instanceof IntegerStamp)
                {
                    XorNode __xorNode = (XorNode) __nonConstant;
                    if (__xorNode.getY().isJavaConstant() && __xorNode.getY().asJavaConstant().asLong() == 1 && ((IntegerStamp) __xorNode.getX().stamp(__view)).upMask() == 1)
                    {
                        // x ^ 1 == 0 is the same as x == 1 if x in [0, 1]
                        // x ^ 1 == 1 is the same as x == 0 if x in [0, 1]
                        return new IntegerEqualsNode(__xorNode.getX(), ConstantNode.forIntegerStamp(__xorNode.getX().stamp(__view), __primitiveConstant.asLong() ^ 1));
                    }
                }
            }
            return super.canonicalizeSymmetricConstant(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __constant, __nonConstant, __mirrored, __unorderedIsTrue, __view);
        }
    }

    @Override
    public Stamp getSucceedingStampForX(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        if (!__negated)
        {
            return __xStamp.join(__yStamp);
        }
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        if (!__negated)
        {
            return __xStamp.join(__yStamp);
        }
        return null;
    }

    @Override
    public TriState tryFold(Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        if (__xStampGeneric instanceof IntegerStamp && __yStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp __xStamp = (IntegerStamp) __xStampGeneric;
            IntegerStamp __yStamp = (IntegerStamp) __yStampGeneric;
            if (__xStamp.alwaysDistinct(__yStamp))
            {
                return TriState.FALSE;
            }
            else if (__xStamp.neverDistinct(__yStamp))
            {
                return TriState.TRUE;
            }
        }
        return TriState.UNKNOWN;
    }
}
