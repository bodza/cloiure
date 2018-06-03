package giraaff.nodes.calc;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;

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

// @class CompareNode
public abstract class CompareNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>
{
    // @def
    public static final NodeClass<CompareNode> TYPE = NodeClass.create(CompareNode.class);

    // @field
    protected final CanonicalCondition ___condition;
    // @field
    protected final boolean ___unorderedIsTrue;

    ///
    // Constructs a new Compare instruction.
    //
    // @param x the instruction producing the first input to the instruction
    // @param y the instruction that produces the second input to this instruction
    ///
    // @cons
    protected CompareNode(NodeClass<? extends CompareNode> __c, CanonicalCondition __condition, boolean __unorderedIsTrue, ValueNode __x, ValueNode __y)
    {
        super(__c, __x, __y);
        this.___condition = __condition;
        this.___unorderedIsTrue = __unorderedIsTrue;
    }

    ///
    // Gets the condition (comparison operation) for this instruction.
    //
    // @return the condition
    ///
    public final CanonicalCondition condition()
    {
        return this.___condition;
    }

    ///
    // Checks whether unordered inputs mean true or false (only applies to float operations).
    //
    // @return {@code true} if unordered inputs produce true
    ///
    public final boolean unorderedIsTrue()
    {
        return this.___unorderedIsTrue;
    }

    public static LogicNode tryConstantFold(CanonicalCondition __condition, ValueNode __forX, ValueNode __forY, ConstantReflectionProvider __constantReflection, boolean __unorderedIsTrue)
    {
        if (__forX.isConstant() && __forY.isConstant() && (__constantReflection != null || __forX.asConstant() instanceof PrimitiveConstant))
        {
            return LogicConstantNode.forBoolean(__condition.foldCondition(__forX.asConstant(), __forY.asConstant(), __constantReflection, __unorderedIsTrue));
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static LogicNode tryConstantFoldPrimitive(CanonicalCondition __condition, ValueNode __forX, ValueNode __forY, boolean __unorderedIsTrue, NodeView __view)
    {
        if (__forX.asConstant() instanceof PrimitiveConstant && __forY.asConstant() instanceof PrimitiveConstant)
        {
            return LogicConstantNode.forBoolean(__condition.foldCondition((PrimitiveConstant) __forX.asConstant(), (PrimitiveConstant) __forY.asConstant(), __unorderedIsTrue));
        }
        return null;
    }

    ///
    // Does this operation represent an identity check such that for x == y, x is exactly the same
    // thing as y. This is generally true except for some floating point comparisons.
    //
    // @return true for identity comparisons
    ///
    public boolean isIdentityComparison()
    {
        return this.___condition == CanonicalCondition.EQ;
    }

    // @class CompareNode.CompareOp
    public abstract static class CompareOp
    {
        public LogicNode canonical(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, boolean __unorderedIsTrue, ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            LogicNode __constantCondition = tryConstantFold(__condition, __forX, __forY, __constantReflection, __unorderedIsTrue);
            if (__constantCondition != null)
            {
                return __constantCondition;
            }
            LogicNode __result;
            if (__forX.isConstant())
            {
                if ((__result = canonicalizeSymmetricConstant(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __forX.asConstant(), __forY, true, __unorderedIsTrue, __view)) != null)
                {
                    return __result;
                }
            }
            else if (__forY.isConstant())
            {
                if ((__result = canonicalizeSymmetricConstant(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __forY.asConstant(), __forX, false, __unorderedIsTrue, __view)) != null)
                {
                    return __result;
                }
            }
            else if (__forX instanceof ConvertNode && __forY instanceof ConvertNode)
            {
                ConvertNode __convertX = (ConvertNode) __forX;
                ConvertNode __convertY = (ConvertNode) __forY;
                if (__convertX.preservesOrder(__condition) && __convertY.preservesOrder(__condition) && __convertX.getValue().stamp(__view).isCompatible(__convertY.getValue().stamp(__view)))
                {
                    boolean __supported = true;
                    if (__convertX.getValue().stamp(__view) instanceof IntegerStamp)
                    {
                        IntegerStamp __intStamp = (IntegerStamp) __convertX.getValue().stamp(__view);
                        __supported = __smallestCompareWidth != null && __intStamp.getBits() >= __smallestCompareWidth;
                    }

                    if (__supported)
                    {
                        boolean __multiUsage = (__convertX.asNode().hasMoreThanOneUsage() || __convertY.asNode().hasMoreThanOneUsage());
                        if ((__forX instanceof ZeroExtendNode || __forX instanceof SignExtendNode) && __multiUsage)
                        {
                            // Do not perform for zero or sign extend if there are multiple usages of the value.
                            return null;
                        }
                        return duplicateModified(__convertX.getValue(), __convertY.getValue(), __unorderedIsTrue, __view);
                    }
                }
            }
            return null;
        }

        protected LogicNode canonicalizeSymmetricConstant(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, Constant __constant, ValueNode __nonConstant, boolean __mirrored, boolean __unorderedIsTrue, NodeView __view)
        {
            if (__nonConstant instanceof ConditionalNode)
            {
                Condition __realCondition = __condition.asCondition();
                if (__mirrored)
                {
                    __realCondition = __realCondition.mirror();
                }
                return optimizeConditional(__constant, (ConditionalNode) __nonConstant, __constantReflection, __realCondition, __unorderedIsTrue);
            }
            else if (__nonConstant instanceof NormalizeCompareNode)
            {
                return optimizeNormalizeCompare(__constantReflection, __metaAccess, __smallestCompareWidth, __constant, (NormalizeCompareNode) __nonConstant, __mirrored, __view);
            }
            else if (__nonConstant instanceof ConvertNode)
            {
                ConvertNode __convert = (ConvertNode) __nonConstant;
                boolean __multiUsage = (__convert.asNode().hasMoreThanOneUsage() && __convert.getValue().hasExactlyOneUsage());
                if ((__convert instanceof ZeroExtendNode || __convert instanceof SignExtendNode) && __multiUsage)
                {
                    // Do not perform for zero or sign extend if it could introduce new live values.
                    return null;
                }

                boolean __supported = true;
                if (__convert.getValue().stamp(__view) instanceof IntegerStamp)
                {
                    IntegerStamp __intStamp = (IntegerStamp) __convert.getValue().stamp(__view);
                    __supported = __smallestCompareWidth != null && __intStamp.getBits() > __smallestCompareWidth;
                }

                if (__supported)
                {
                    ConstantNode __newConstant = canonicalConvertConstant(__constantReflection, __metaAccess, __condition, __convert, __constant, __view);
                    if (__newConstant != null)
                    {
                        if (__mirrored)
                        {
                            return duplicateModified(__newConstant, __convert.getValue(), __unorderedIsTrue, __view);
                        }
                        else
                        {
                            return duplicateModified(__convert.getValue(), __newConstant, __unorderedIsTrue, __view);
                        }
                    }
                }
            }

            return null;
        }

        private static ConstantNode canonicalConvertConstant(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, CanonicalCondition __condition, ConvertNode __convert, Constant __constant, NodeView __view)
        {
            if (__convert.preservesOrder(__condition, __constant, __constantReflection))
            {
                Constant __reverseConverted = __convert.reverse(__constant, __constantReflection);
                if (__reverseConverted != null && __convert.convert(__reverseConverted, __constantReflection).equals(__constant))
                {
                    return ConstantNode.forConstant(__convert.getValue().stamp(__view), __reverseConverted, __metaAccess);
                }
            }
            return null;
        }

        @SuppressWarnings("unused")
        protected LogicNode optimizeNormalizeCompare(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, Constant __constant, NormalizeCompareNode __normalizeNode, boolean __mirrored, NodeView __view)
        {
            throw new BailoutException("NormalizeCompareNode connected to %s (%s %s %s)", this, __constant, __normalizeNode, __mirrored);
        }

        private static LogicNode optimizeConditional(Constant __constant, ConditionalNode __conditionalNode, ConstantReflectionProvider __constantReflection, Condition __cond, boolean __unorderedIsTrue)
        {
            Constant __trueConstant = __conditionalNode.trueValue().asConstant();
            Constant __falseConstant = __conditionalNode.falseValue().asConstant();

            if (__falseConstant != null && __trueConstant != null && __constantReflection != null)
            {
                boolean __trueResult = __cond.foldCondition(__trueConstant, __constant, __constantReflection, __unorderedIsTrue);
                boolean __falseResult = __cond.foldCondition(__falseConstant, __constant, __constantReflection, __unorderedIsTrue);

                if (__trueResult == __falseResult)
                {
                    return LogicConstantNode.forBoolean(__trueResult);
                }
                else
                {
                    if (__trueResult)
                    {
                        return __conditionalNode.condition();
                    }
                    else
                    {
                        return LogicNegationNode.create(__conditionalNode.condition());
                    }
                }
            }

            return null;
        }

        protected abstract LogicNode duplicateModified(ValueNode __newW, ValueNode __newY, boolean __unorderedIsTrue, NodeView __view);
    }

    public static LogicNode createCompareNode(StructuredGraph __graph, CanonicalCondition __condition, ValueNode __x, ValueNode __y, ConstantReflectionProvider __constantReflection, NodeView __view)
    {
        LogicNode __result = createCompareNode(__condition, __x, __y, __constantReflection, __view);
        return (__result.graph() == null ? __graph.addOrUniqueWithInputs(__result) : __result);
    }

    public static LogicNode createCompareNode(CanonicalCondition __condition, ValueNode __x, ValueNode __y, ConstantReflectionProvider __constantReflection, NodeView __view)
    {
        LogicNode __comparison;
        if (__condition == CanonicalCondition.EQ)
        {
            if (__x.stamp(__view) instanceof AbstractObjectStamp)
            {
                __comparison = ObjectEqualsNode.create(__x, __y, __constantReflection, __view);
            }
            else if (__x.stamp(__view) instanceof AbstractPointerStamp)
            {
                __comparison = PointerEqualsNode.create(__x, __y, __view);
            }
            else
            {
                __comparison = IntegerEqualsNode.create(__x, __y, __view);
            }
        }
        else if (__condition == CanonicalCondition.LT)
        {
            __comparison = IntegerLessThanNode.create(__x, __y, __view);
        }
        else
        {
            __comparison = IntegerBelowNode.create(__x, __y, __view);
        }

        return __comparison;
    }

    public static LogicNode createCompareNode(StructuredGraph __graph, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __result = createCompareNode(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __x, __y, __view);
        return (__result.graph() == null ? __graph.addOrUniqueWithInputs(__result) : __result);
    }

    public static LogicNode createCompareNode(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __comparison;
        if (__condition == CanonicalCondition.EQ)
        {
            if (__x.stamp(__view) instanceof AbstractObjectStamp)
            {
                __comparison = ObjectEqualsNode.create(__constantReflection, __metaAccess, __x, __y, __view);
            }
            else if (__x.stamp(__view) instanceof AbstractPointerStamp)
            {
                __comparison = PointerEqualsNode.create(__x, __y, __view);
            }
            else
            {
                __comparison = IntegerEqualsNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __x, __y, __view);
            }
        }
        else if (__condition == CanonicalCondition.LT)
        {
            __comparison = IntegerLessThanNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __x, __y, __view);
        }
        else
        {
            __comparison = IntegerBelowNode.create(__constantReflection, __metaAccess, __smallestCompareWidth, __x, __y, __view);
        }

        return __comparison;
    }
}
