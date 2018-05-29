package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.core.common.PermanentBailoutException;
import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.type.AbstractObjectStamp;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.BinaryOpLogicNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNegationNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.options.OptionValues;

// @class CompareNode
public abstract class CompareNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>
{
    public static final NodeClass<CompareNode> TYPE = NodeClass.create(CompareNode.class);

    protected final CanonicalCondition condition;
    protected final boolean unorderedIsTrue;

    /**
     * Constructs a new Compare instruction.
     *
     * @param x the instruction producing the first input to the instruction
     * @param y the instruction that produces the second input to this instruction
     */
    // @cons
    protected CompareNode(NodeClass<? extends CompareNode> c, CanonicalCondition condition, boolean unorderedIsTrue, ValueNode x, ValueNode y)
    {
        super(c, x, y);
        this.condition = condition;
        this.unorderedIsTrue = unorderedIsTrue;
    }

    /**
     * Gets the condition (comparison operation) for this instruction.
     *
     * @return the condition
     */
    public final CanonicalCondition condition()
    {
        return condition;
    }

    /**
     * Checks whether unordered inputs mean true or false (only applies to float operations).
     *
     * @return {@code true} if unordered inputs produce true
     */
    public final boolean unorderedIsTrue()
    {
        return this.unorderedIsTrue;
    }

    public static LogicNode tryConstantFold(CanonicalCondition condition, ValueNode forX, ValueNode forY, ConstantReflectionProvider constantReflection, boolean unorderedIsTrue)
    {
        if (forX.isConstant() && forY.isConstant() && (constantReflection != null || forX.asConstant() instanceof PrimitiveConstant))
        {
            return LogicConstantNode.forBoolean(condition.foldCondition(forX.asConstant(), forY.asConstant(), constantReflection, unorderedIsTrue));
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static LogicNode tryConstantFoldPrimitive(CanonicalCondition condition, ValueNode forX, ValueNode forY, boolean unorderedIsTrue, NodeView view)
    {
        if (forX.asConstant() instanceof PrimitiveConstant && forY.asConstant() instanceof PrimitiveConstant)
        {
            return LogicConstantNode.forBoolean(condition.foldCondition((PrimitiveConstant) forX.asConstant(), (PrimitiveConstant) forY.asConstant(), unorderedIsTrue));
        }
        return null;
    }

    /**
     * Does this operation represent an identity check such that for x == y, x is exactly the same
     * thing as y. This is generally true except for some floating point comparisons.
     *
     * @return true for identity comparisons
     */
    public boolean isIdentityComparison()
    {
        return condition == CanonicalCondition.EQ;
    }

    // @class CompareNode.CompareOp
    public abstract static class CompareOp
    {
        public LogicNode canonical(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, boolean unorderedIsTrue, ValueNode forX, ValueNode forY, NodeView view)
        {
            LogicNode constantCondition = tryConstantFold(condition, forX, forY, constantReflection, unorderedIsTrue);
            if (constantCondition != null)
            {
                return constantCondition;
            }
            LogicNode result;
            if (forX.isConstant())
            {
                if ((result = canonicalizeSymmetricConstant(constantReflection, metaAccess, options, smallestCompareWidth, condition, forX.asConstant(), forY, true, unorderedIsTrue, view)) != null)
                {
                    return result;
                }
            }
            else if (forY.isConstant())
            {
                if ((result = canonicalizeSymmetricConstant(constantReflection, metaAccess, options, smallestCompareWidth, condition, forY.asConstant(), forX, false, unorderedIsTrue, view)) != null)
                {
                    return result;
                }
            }
            else if (forX instanceof ConvertNode && forY instanceof ConvertNode)
            {
                ConvertNode convertX = (ConvertNode) forX;
                ConvertNode convertY = (ConvertNode) forY;
                if (convertX.preservesOrder(condition) && convertY.preservesOrder(condition) && convertX.getValue().stamp(view).isCompatible(convertY.getValue().stamp(view)))
                {
                    boolean supported = true;
                    if (convertX.getValue().stamp(view) instanceof IntegerStamp)
                    {
                        IntegerStamp intStamp = (IntegerStamp) convertX.getValue().stamp(view);
                        supported = smallestCompareWidth != null && intStamp.getBits() >= smallestCompareWidth;
                    }

                    if (supported)
                    {
                        boolean multiUsage = (convertX.asNode().hasMoreThanOneUsage() || convertY.asNode().hasMoreThanOneUsage());
                        if ((forX instanceof ZeroExtendNode || forX instanceof SignExtendNode) && multiUsage)
                        {
                            // Do not perform for zero or sign extend if there are multiple usages of the value.
                            return null;
                        }
                        return duplicateModified(convertX.getValue(), convertY.getValue(), unorderedIsTrue, view);
                    }
                }
            }
            return null;
        }

        protected LogicNode canonicalizeSymmetricConstant(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, Constant constant, ValueNode nonConstant, boolean mirrored, boolean unorderedIsTrue, NodeView view)
        {
            if (nonConstant instanceof ConditionalNode)
            {
                Condition realCondition = condition.asCondition();
                if (mirrored)
                {
                    realCondition = realCondition.mirror();
                }
                return optimizeConditional(constant, (ConditionalNode) nonConstant, constantReflection, realCondition, unorderedIsTrue);
            }
            else if (nonConstant instanceof NormalizeCompareNode)
            {
                return optimizeNormalizeCompare(constantReflection, metaAccess, options, smallestCompareWidth, constant, (NormalizeCompareNode) nonConstant, mirrored, view);
            }
            else if (nonConstant instanceof ConvertNode)
            {
                ConvertNode convert = (ConvertNode) nonConstant;
                boolean multiUsage = (convert.asNode().hasMoreThanOneUsage() && convert.getValue().hasExactlyOneUsage());
                if ((convert instanceof ZeroExtendNode || convert instanceof SignExtendNode) && multiUsage)
                {
                    // Do not perform for zero or sign extend if it could introduce new live values.
                    return null;
                }

                boolean supported = true;
                if (convert.getValue().stamp(view) instanceof IntegerStamp)
                {
                    IntegerStamp intStamp = (IntegerStamp) convert.getValue().stamp(view);
                    supported = smallestCompareWidth != null && intStamp.getBits() > smallestCompareWidth;
                }

                if (supported)
                {
                    ConstantNode newConstant = canonicalConvertConstant(constantReflection, metaAccess, options, condition, convert, constant, view);
                    if (newConstant != null)
                    {
                        if (mirrored)
                        {
                            return duplicateModified(newConstant, convert.getValue(), unorderedIsTrue, view);
                        }
                        else
                        {
                            return duplicateModified(convert.getValue(), newConstant, unorderedIsTrue, view);
                        }
                    }
                }
            }

            return null;
        }

        private static ConstantNode canonicalConvertConstant(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, CanonicalCondition condition, ConvertNode convert, Constant constant, NodeView view)
        {
            if (convert.preservesOrder(condition, constant, constantReflection))
            {
                Constant reverseConverted = convert.reverse(constant, constantReflection);
                if (reverseConverted != null && convert.convert(reverseConverted, constantReflection).equals(constant))
                {
                    return ConstantNode.forConstant(convert.getValue().stamp(view), reverseConverted, metaAccess);
                }
            }
            return null;
        }

        @SuppressWarnings("unused")
        protected LogicNode optimizeNormalizeCompare(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, Constant constant, NormalizeCompareNode normalizeNode, boolean mirrored, NodeView view)
        {
            throw new PermanentBailoutException("NormalizeCompareNode connected to %s (%s %s %s)", this, constant, normalizeNode, mirrored);
        }

        private static LogicNode optimizeConditional(Constant constant, ConditionalNode conditionalNode, ConstantReflectionProvider constantReflection, Condition cond, boolean unorderedIsTrue)
        {
            Constant trueConstant = conditionalNode.trueValue().asConstant();
            Constant falseConstant = conditionalNode.falseValue().asConstant();

            if (falseConstant != null && trueConstant != null && constantReflection != null)
            {
                boolean trueResult = cond.foldCondition(trueConstant, constant, constantReflection, unorderedIsTrue);
                boolean falseResult = cond.foldCondition(falseConstant, constant, constantReflection, unorderedIsTrue);

                if (trueResult == falseResult)
                {
                    return LogicConstantNode.forBoolean(trueResult);
                }
                else
                {
                    if (trueResult)
                    {
                        return conditionalNode.condition();
                    }
                    else
                    {
                        return LogicNegationNode.create(conditionalNode.condition());
                    }
                }
            }

            return null;
        }

        protected abstract LogicNode duplicateModified(ValueNode newW, ValueNode newY, boolean unorderedIsTrue, NodeView view);
    }

    public static LogicNode createCompareNode(StructuredGraph graph, CanonicalCondition condition, ValueNode x, ValueNode y, ConstantReflectionProvider constantReflection, NodeView view)
    {
        LogicNode result = createCompareNode(condition, x, y, constantReflection, view);
        return (result.graph() == null ? graph.addOrUniqueWithInputs(result) : result);
    }

    public static LogicNode createCompareNode(CanonicalCondition condition, ValueNode x, ValueNode y, ConstantReflectionProvider constantReflection, NodeView view)
    {
        LogicNode comparison;
        if (condition == CanonicalCondition.EQ)
        {
            if (x.stamp(view) instanceof AbstractObjectStamp)
            {
                comparison = ObjectEqualsNode.create(x, y, constantReflection, view);
            }
            else if (x.stamp(view) instanceof AbstractPointerStamp)
            {
                comparison = PointerEqualsNode.create(x, y, view);
            }
            else
            {
                comparison = IntegerEqualsNode.create(x, y, view);
            }
        }
        else if (condition == CanonicalCondition.LT)
        {
            comparison = IntegerLessThanNode.create(x, y, view);
        }
        else
        {
            comparison = IntegerBelowNode.create(x, y, view);
        }

        return comparison;
    }

    public static LogicNode createCompareNode(StructuredGraph graph, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode result = createCompareNode(constantReflection, metaAccess, options, smallestCompareWidth, condition, x, y, view);
        return (result.graph() == null ? graph.addOrUniqueWithInputs(result) : result);
    }

    public static LogicNode createCompareNode(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode comparison;
        if (condition == CanonicalCondition.EQ)
        {
            if (x.stamp(view) instanceof AbstractObjectStamp)
            {
                comparison = ObjectEqualsNode.create(constantReflection, metaAccess, options, x, y, view);
            }
            else if (x.stamp(view) instanceof AbstractPointerStamp)
            {
                comparison = PointerEqualsNode.create(x, y, view);
            }
            else
            {
                comparison = IntegerEqualsNode.create(constantReflection, metaAccess, options, smallestCompareWidth, x, y, view);
            }
        }
        else if (condition == CanonicalCondition.LT)
        {
            comparison = IntegerLessThanNode.create(constantReflection, metaAccess, options, smallestCompareWidth, x, y, view);
        }
        else
        {
            comparison = IntegerBelowNode.create(constantReflection, metaAccess, options, smallestCompareWidth, x, y, view);
        }

        return comparison;
    }
}
