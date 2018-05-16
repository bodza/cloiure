package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.core.common.type.AbstractPointerStamp;
import graalvm.compiler.core.common.type.FloatStamp;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable.BinaryCommutative;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.LogicNegationNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.TriState;

@NodeInfo(shortName = "==")
public final class IntegerEqualsNode extends CompareNode implements BinaryCommutative<ValueNode>
{
    public static final NodeClass<IntegerEqualsNode> TYPE = NodeClass.create(IntegerEqualsNode.class);
    private static final IntegerEqualsOp OP = new IntegerEqualsOp();

    public IntegerEqualsNode(ValueNode x, ValueNode y)
    {
        super(TYPE, CanonicalCondition.EQ, false, x, y);
        assert !x.getStackKind().isNumericFloat() && x.getStackKind() != JavaKind.Object;
        assert !y.getStackKind().isNumericFloat() && y.getStackKind() != JavaKind.Object;
    }

    public static LogicNode create(ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode result = CompareNode.tryConstantFoldPrimitive(CanonicalCondition.EQ, x, y, false, view);
        if (result != null)
        {
            return result;
        }
        if (x instanceof ConditionalNode)
        {
            ConditionalNode conditionalNode = (ConditionalNode) x;
            if (conditionalNode.trueValue() == y)
            {
                return conditionalNode.condition();
            }
            if (conditionalNode.falseValue() == y)
            {
                return LogicNegationNode.create(conditionalNode.condition());
            }
        }
        else if (y instanceof ConditionalNode)
        {
            ConditionalNode conditionalNode = (ConditionalNode) y;
            if (conditionalNode.trueValue() == x)
            {
                return conditionalNode.condition();
            }
            if (conditionalNode.falseValue() == x)
            {
                return LogicNegationNode.create(conditionalNode.condition());
            }
        }
        return new IntegerEqualsNode(x, y).maybeCommuteInputs();
    }

    public static LogicNode create(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode value = OP.canonical(constantReflection, metaAccess, options, smallestCompareWidth, CanonicalCondition.EQ, false, x, y, view);
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
        ValueNode value = OP.canonical(tool.getConstantReflection(), tool.getMetaAccess(), tool.getOptions(), tool.smallestCompareWidth(), CanonicalCondition.EQ, false, forX, forY, view);
        if (value != null)
        {
            return value;
        }
        return this;
    }

    public static class IntegerEqualsOp extends CompareOp
    {
        @Override
        protected LogicNode optimizeNormalizeCompare(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, Constant constant, NormalizeCompareNode normalizeNode, boolean mirrored, NodeView view)
        {
            PrimitiveConstant primitive = (PrimitiveConstant) constant;
            ValueNode a = normalizeNode.getX();
            ValueNode b = normalizeNode.getY();
            long cst = primitive.asLong();

            if (cst == 0)
            {
                if (normalizeNode.getX().getStackKind() == JavaKind.Double || normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatEqualsNode.create(constantReflection, metaAccess, options, smallestCompareWidth, a, b, view);
                }
                else
                {
                    return IntegerEqualsNode.create(constantReflection, metaAccess, options, smallestCompareWidth, a, b, view);
                }
            }
            else if (cst == 1)
            {
                if (normalizeNode.getX().getStackKind() == JavaKind.Double || normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatLessThanNode.create(b, a, !normalizeNode.isUnorderedLess, view);
                }
                else
                {
                    return IntegerLessThanNode.create(constantReflection, metaAccess, options, smallestCompareWidth, b, a, view);
                }
            }
            else if (cst == -1)
            {
                if (normalizeNode.getX().getStackKind() == JavaKind.Double || normalizeNode.getX().getStackKind() == JavaKind.Float)
                {
                    return FloatLessThanNode.create(a, b, normalizeNode.isUnorderedLess, view);
                }
                else
                {
                    return IntegerLessThanNode.create(constantReflection, metaAccess, options, smallestCompareWidth, a, b, view);
                }
            }
            else
            {
                return LogicConstantNode.contradiction();
            }
        }

        @Override
        protected CompareNode duplicateModified(ValueNode newX, ValueNode newY, boolean unorderedIsTrue, NodeView view)
        {
            if (newX.stamp(view) instanceof FloatStamp && newY.stamp(view) instanceof FloatStamp)
            {
                return new FloatEqualsNode(newX, newY);
            }
            else if (newX.stamp(view) instanceof IntegerStamp && newY.stamp(view) instanceof IntegerStamp)
            {
                return new IntegerEqualsNode(newX, newY);
            }
            else if (newX.stamp(view) instanceof AbstractPointerStamp && newY.stamp(view) instanceof AbstractPointerStamp)
            {
                return new IntegerEqualsNode(newX, newY);
            }
            throw GraalError.shouldNotReachHere();
        }

        @Override
        public LogicNode canonical(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, boolean unorderedIsTrue, ValueNode forX, ValueNode forY, NodeView view)
        {
            if (GraphUtil.unproxify(forX) == GraphUtil.unproxify(forY))
            {
                return LogicConstantNode.tautology();
            }
            else if (forX.stamp(view).alwaysDistinct(forY.stamp(view)))
            {
                return LogicConstantNode.contradiction();
            }

            if (forX instanceof AddNode && forY instanceof AddNode)
            {
                AddNode addX = (AddNode) forX;
                AddNode addY = (AddNode) forY;
                ValueNode v1 = null;
                ValueNode v2 = null;
                if (addX.getX() == addY.getX())
                {
                    v1 = addX.getY();
                    v2 = addY.getY();
                }
                else if (addX.getX() == addY.getY())
                {
                    v1 = addX.getY();
                    v2 = addY.getX();
                }
                else if (addX.getY() == addY.getX())
                {
                    v1 = addX.getX();
                    v2 = addY.getY();
                }
                else if (addX.getY() == addY.getY())
                {
                    v1 = addX.getX();
                    v2 = addY.getX();
                }
                if (v1 != null)
                {
                    assert v2 != null;
                    return create(v1, v2, view);
                }
            }

            return super.canonical(constantReflection, metaAccess, options, smallestCompareWidth, condition, unorderedIsTrue, forX, forY, view);
        }

        @Override
        protected LogicNode canonicalizeSymmetricConstant(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, Constant constant, ValueNode nonConstant, boolean mirrored, boolean unorderedIsTrue, NodeView view)
        {
            if (constant instanceof PrimitiveConstant)
            {
                PrimitiveConstant primitiveConstant = (PrimitiveConstant) constant;
                IntegerStamp nonConstantStamp = ((IntegerStamp) nonConstant.stamp(view));
                if ((primitiveConstant.asLong() == 1 && nonConstantStamp.upperBound() == 1 && nonConstantStamp.lowerBound() == 0) || (primitiveConstant.asLong() == -1 && nonConstantStamp.upperBound() == 0 && nonConstantStamp.lowerBound() == -1))
                {
                    // nonConstant can only be 0 or 1 (respective -1), test against 0 instead of 1
                    // (respective -1) for a more canonical graph and also to allow for faster
                    // execution
                    // on specific platforms.
                    return LogicNegationNode.create(IntegerEqualsNode.create(constantReflection, metaAccess, options, smallestCompareWidth, nonConstant, ConstantNode.forIntegerKind(nonConstant.getStackKind(), 0), view));
                }
                else if (primitiveConstant.asLong() == 0)
                {
                    if (nonConstant instanceof AndNode)
                    {
                        AndNode andNode = (AndNode) nonConstant;
                        return new IntegerTestNode(andNode.getX(), andNode.getY());
                    }
                    else if (nonConstant instanceof SubNode)
                    {
                        SubNode subNode = (SubNode) nonConstant;
                        return IntegerEqualsNode.create(constantReflection, metaAccess, options, smallestCompareWidth, subNode.getX(), subNode.getY(), view);
                    }
                    else if (nonConstant instanceof ShiftNode && nonConstant.stamp(view) instanceof IntegerStamp)
                    {
                        if (nonConstant instanceof LeftShiftNode)
                        {
                            LeftShiftNode shift = (LeftShiftNode) nonConstant;
                            if (shift.getY().isConstant())
                            {
                                int mask = shift.getShiftAmountMask();
                                int amount = shift.getY().asJavaConstant().asInt() & mask;
                                if (shift.getX().getStackKind() == JavaKind.Int)
                                {
                                    return new IntegerTestNode(shift.getX(), ConstantNode.forInt(-1 >>> amount));
                                }
                                else
                                {
                                    assert shift.getX().getStackKind() == JavaKind.Long;
                                    return new IntegerTestNode(shift.getX(), ConstantNode.forLong(-1L >>> amount));
                                }
                            }
                        }
                        else if (nonConstant instanceof RightShiftNode)
                        {
                            RightShiftNode shift = (RightShiftNode) nonConstant;
                            if (shift.getY().isConstant() && ((IntegerStamp) shift.getX().stamp(view)).isPositive())
                            {
                                int mask = shift.getShiftAmountMask();
                                int amount = shift.getY().asJavaConstant().asInt() & mask;
                                if (shift.getX().getStackKind() == JavaKind.Int)
                                {
                                    return new IntegerTestNode(shift.getX(), ConstantNode.forInt(-1 << amount));
                                }
                                else
                                {
                                    assert shift.getX().getStackKind() == JavaKind.Long;
                                    return new IntegerTestNode(shift.getX(), ConstantNode.forLong(-1L << amount));
                                }
                            }
                        }
                        else if (nonConstant instanceof UnsignedRightShiftNode)
                        {
                            UnsignedRightShiftNode shift = (UnsignedRightShiftNode) nonConstant;
                            if (shift.getY().isConstant())
                            {
                                int mask = shift.getShiftAmountMask();
                                int amount = shift.getY().asJavaConstant().asInt() & mask;
                                if (shift.getX().getStackKind() == JavaKind.Int)
                                {
                                    return new IntegerTestNode(shift.getX(), ConstantNode.forInt(-1 << amount));
                                }
                                else
                                {
                                    assert shift.getX().getStackKind() == JavaKind.Long;
                                    return new IntegerTestNode(shift.getX(), ConstantNode.forLong(-1L << amount));
                                }
                            }
                        }
                    }
                }
                if (nonConstant instanceof AddNode)
                {
                    AddNode addNode = (AddNode) nonConstant;
                    if (addNode.getY().isJavaConstant())
                    {
                        return new IntegerEqualsNode(addNode.getX(), ConstantNode.forIntegerStamp(nonConstantStamp, primitiveConstant.asLong() - addNode.getY().asJavaConstant().asLong()));
                    }
                }
                if (nonConstant instanceof AndNode)
                {
                    /*
                     * a & c == c is the same as a & c != 0, if c is a single bit.
                     */
                    AndNode andNode = (AndNode) nonConstant;
                    if (Long.bitCount(((PrimitiveConstant) constant).asLong()) == 1 && andNode.getY().isConstant() && andNode.getY().asJavaConstant().equals(constant))
                    {
                        return new LogicNegationNode(new IntegerTestNode(andNode.getX(), andNode.getY()));
                    }
                }

                if (nonConstant instanceof XorNode && nonConstant.stamp(view) instanceof IntegerStamp)
                {
                    XorNode xorNode = (XorNode) nonConstant;
                    if (xorNode.getY().isJavaConstant() && xorNode.getY().asJavaConstant().asLong() == 1 && ((IntegerStamp) xorNode.getX().stamp(view)).upMask() == 1)
                    {
                        // x ^ 1 == 0 is the same as x == 1 if x in [0, 1]
                        // x ^ 1 == 1 is the same as x == 0 if x in [0, 1]
                        return new IntegerEqualsNode(xorNode.getX(), ConstantNode.forIntegerStamp(xorNode.getX().stamp(view), primitiveConstant.asLong() ^ 1));
                    }
                }
            }
            return super.canonicalizeSymmetricConstant(constantReflection, metaAccess, options, smallestCompareWidth, condition, constant, nonConstant, mirrored, unorderedIsTrue, view);
        }
    }

    @Override
    public Stamp getSucceedingStampForX(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        if (!negated)
        {
            return xStamp.join(yStamp);
        }
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        if (!negated)
        {
            return xStamp.join(yStamp);
        }
        return null;
    }

    @Override
    public TriState tryFold(Stamp xStampGeneric, Stamp yStampGeneric)
    {
        if (xStampGeneric instanceof IntegerStamp && yStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp xStamp = (IntegerStamp) xStampGeneric;
            IntegerStamp yStamp = (IntegerStamp) yStampGeneric;
            if (xStamp.alwaysDistinct(yStamp))
            {
                return TriState.FALSE;
            }
            else if (xStamp.neverDistinct(yStamp))
            {
                return TriState.TRUE;
            }
        }
        return TriState.UNKNOWN;
    }
}
