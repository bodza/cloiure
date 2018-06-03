package giraaff.nodes.calc;

import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNegationNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// The {@code ConditionalNode} class represents a comparison that yields one of two (eagerly
// evaluated) values.
///
// @class ConditionalNode
public final class ConditionalNode extends FloatingNode implements Canonicalizable, LIRLowerable
{
    // @def
    public static final NodeClass<ConditionalNode> TYPE = NodeClass.create(ConditionalNode.class);

    @Input(InputType.Condition)
    // @field
    LogicNode ___condition;
    @Input(InputType.Value)
    // @field
    ValueNode ___trueValue;
    @Input(InputType.Value)
    // @field
    ValueNode ___falseValue;

    public LogicNode condition()
    {
        return this.___condition;
    }

    // @cons
    public ConditionalNode(LogicNode __condition)
    {
        this(__condition, ConstantNode.forInt(1, __condition.graph()), ConstantNode.forInt(0, __condition.graph()));
    }

    // @cons
    public ConditionalNode(LogicNode __condition, ValueNode __trueValue, ValueNode __falseValue)
    {
        super(TYPE, __trueValue.stamp(NodeView.DEFAULT).meet(__falseValue.stamp(NodeView.DEFAULT)));
        this.___condition = __condition;
        this.___trueValue = __trueValue;
        this.___falseValue = __falseValue;
    }

    public static ValueNode create(LogicNode __condition, NodeView __view)
    {
        return create(__condition, ConstantNode.forInt(1, __condition.graph()), ConstantNode.forInt(0, __condition.graph()), __view);
    }

    public static ValueNode create(LogicNode __condition, ValueNode __trueValue, ValueNode __falseValue, NodeView __view)
    {
        ValueNode __synonym = findSynonym(__condition, __trueValue, __falseValue, __view);
        if (__synonym != null)
        {
            return __synonym;
        }
        ValueNode __result = canonicalizeConditional(__condition, __trueValue, __falseValue, __trueValue.stamp(__view).meet(__falseValue.stamp(__view)), __view);
        if (__result != null)
        {
            return __result;
        }
        return new ConditionalNode(__condition, __trueValue, __falseValue);
    }

    @Override
    public boolean inferStamp()
    {
        Stamp __valueStamp = this.___trueValue.stamp(NodeView.DEFAULT).meet(this.___falseValue.stamp(NodeView.DEFAULT));
        if (this.___condition instanceof IntegerLessThanNode)
        {
            IntegerLessThanNode __lessThan = (IntegerLessThanNode) this.___condition;
            if (__lessThan.getX() == this.___trueValue && __lessThan.getY() == this.___falseValue)
            {
                // this encodes a min operation
                JavaConstant __constant = __lessThan.getX().asJavaConstant();
                if (__constant == null)
                {
                    __constant = __lessThan.getY().asJavaConstant();
                }
                if (__constant != null)
                {
                    IntegerStamp __bounds = StampFactory.forInteger(__constant.getJavaKind(), __constant.getJavaKind().getMinValue(), __constant.asLong());
                    __valueStamp = __valueStamp.join(__bounds);
                }
            }
            else if (__lessThan.getX() == this.___falseValue && __lessThan.getY() == this.___trueValue)
            {
                // this encodes a max operation
                JavaConstant __constant = __lessThan.getX().asJavaConstant();
                if (__constant == null)
                {
                    __constant = __lessThan.getY().asJavaConstant();
                }
                if (__constant != null)
                {
                    IntegerStamp __bounds = StampFactory.forInteger(__constant.getJavaKind(), __constant.asLong(), __constant.getJavaKind().getMaxValue());
                    __valueStamp = __valueStamp.join(__bounds);
                }
            }
        }
        return updateStamp(__valueStamp);
    }

    public ValueNode trueValue()
    {
        return this.___trueValue;
    }

    public ValueNode falseValue()
    {
        return this.___falseValue;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __synonym = findSynonym(this.___condition, trueValue(), falseValue(), __view);
        if (__synonym != null)
        {
            return __synonym;
        }

        ValueNode __result = canonicalizeConditional(this.___condition, trueValue(), falseValue(), this.___stamp, __view);
        if (__result != null)
        {
            return __result;
        }

        return this;
    }

    public static ValueNode canonicalizeConditional(LogicNode __condition, ValueNode __trueValue, ValueNode __falseValue, Stamp __stamp, NodeView __view)
    {
        if (__trueValue == __falseValue)
        {
            return __trueValue;
        }

        if (__condition instanceof CompareNode && ((CompareNode) __condition).isIdentityComparison())
        {
            // optimize the pattern (x == y) ? x : y
            CompareNode __compare = (CompareNode) __condition;
            if ((__compare.getX() == __trueValue && __compare.getY() == __falseValue) || (__compare.getX() == __falseValue && __compare.getY() == __trueValue))
            {
                return __falseValue;
            }
        }

        if (__trueValue.stamp(__view) instanceof IntegerStamp)
        {
            // check if the conditional is redundant
            if (__condition instanceof IntegerLessThanNode)
            {
                IntegerLessThanNode __lessThan = (IntegerLessThanNode) __condition;
                IntegerStamp __falseValueStamp = (IntegerStamp) __falseValue.stamp(__view);
                IntegerStamp __trueValueStamp = (IntegerStamp) __trueValue.stamp(__view);
                if (__lessThan.getX() == __trueValue && __lessThan.getY() == __falseValue)
                {
                    // return "x" for "x < y ? x : y" in case that we know "x <= y"
                    if (__trueValueStamp.upperBound() <= __falseValueStamp.lowerBound())
                    {
                        return __trueValue;
                    }
                }
                else if (__lessThan.getX() == __falseValue && __lessThan.getY() == __trueValue)
                {
                    // return "y" for "x < y ? y : x" in case that we know "x <= y"
                    if (__falseValueStamp.upperBound() <= __trueValueStamp.lowerBound())
                    {
                        return __trueValue;
                    }
                }
            }

            // this optimizes the case where a value from the range 0 - 1 is mapped to the
            // range 0 - 1
            if (__trueValue.isConstant() && __falseValue.isConstant())
            {
                long __constTrueValue = __trueValue.asJavaConstant().asLong();
                long __constFalseValue = __falseValue.asJavaConstant().asLong();
                if (__condition instanceof IntegerEqualsNode)
                {
                    IntegerEqualsNode __equals = (IntegerEqualsNode) __condition;
                    if (__equals.getY().isConstant() && __equals.getX().stamp(__view) instanceof IntegerStamp)
                    {
                        IntegerStamp __equalsXStamp = (IntegerStamp) __equals.getX().stamp(__view);
                        if (__equalsXStamp.upMask() == 1)
                        {
                            long __equalsY = __equals.getY().asJavaConstant().asLong();
                            if (__equalsY == 0)
                            {
                                if (__constTrueValue == 0 && __constFalseValue == 1)
                                {
                                    // return x when: x == 0 ? 0 : 1;
                                    return IntegerConvertNode.convertUnsigned(__equals.getX(), __stamp, __view);
                                }
                                else if (__constTrueValue == 1 && __constFalseValue == 0)
                                {
                                    // negate a boolean value via xor
                                    return IntegerConvertNode.convertUnsigned(XorNode.create(__equals.getX(), ConstantNode.forIntegerStamp(__equals.getX().stamp(__view), 1), __view), __stamp, __view);
                                }
                            }
                            else if (__equalsY == 1)
                            {
                                if (__constTrueValue == 1 && __constFalseValue == 0)
                                {
                                    // return x when: x == 1 ? 1 : 0;
                                    return IntegerConvertNode.convertUnsigned(__equals.getX(), __stamp, __view);
                                }
                                else if (__constTrueValue == 0 && __constFalseValue == 1)
                                {
                                    // negate a boolean value via xor
                                    return IntegerConvertNode.convertUnsigned(XorNode.create(__equals.getX(), ConstantNode.forIntegerStamp(__equals.getX().stamp(__view), 1), __view), __stamp, __view);
                                }
                            }
                        }
                    }
                }
                else if (__condition instanceof IntegerTestNode)
                {
                    // replace IntegerTestNode with AndNode for the following patterns:
                    // (value & 1) == 0 ? 0 : 1
                    // (value & 1) == 1 ? 1 : 0
                    IntegerTestNode __integerTestNode = (IntegerTestNode) __condition;
                    if (__integerTestNode.getY().isConstant())
                    {
                        long __testY = __integerTestNode.getY().asJavaConstant().asLong();
                        if (__testY == 1 && __constTrueValue == 0 && __constFalseValue == 1)
                        {
                            return IntegerConvertNode.convertUnsigned(AndNode.create(__integerTestNode.getX(), __integerTestNode.getY(), __view), __stamp, __view);
                        }
                    }
                }
            }

            if (__condition instanceof IntegerLessThanNode)
            {
                // Convert a conditional add ((x < 0) ? (x + y) : x) into (x + (y & (x >> (bits - 1)))) to avoid the test.
                IntegerLessThanNode __lt = (IntegerLessThanNode) __condition;
                if (__lt.getY().isConstant() && __lt.getY().asConstant().isDefaultForKind())
                {
                    if (__falseValue == __lt.getX())
                    {
                        if (__trueValue instanceof AddNode)
                        {
                            AddNode __add = (AddNode) __trueValue;
                            if (__add.getX() == __falseValue)
                            {
                                int __bits = ((IntegerStamp) __trueValue.stamp(NodeView.DEFAULT)).getBits();
                                ValueNode __shift = new RightShiftNode(__lt.getX(), ConstantNode.forIntegerBits(32, __bits - 1));
                                ValueNode __and = new AndNode(__shift, __add.getY());
                                return new AddNode(__add.getX(), __and);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static ValueNode findSynonym(ValueNode __condition, ValueNode __trueValue, ValueNode __falseValue, NodeView __view)
    {
        if (__condition instanceof LogicNegationNode)
        {
            LogicNegationNode __negated = (LogicNegationNode) __condition;
            return ConditionalNode.create(__negated.getValue(), __falseValue, __trueValue, __view);
        }
        if (__condition instanceof LogicConstantNode)
        {
            LogicConstantNode __c = (LogicConstantNode) __condition;
            if (__c.getValue())
            {
                return __trueValue;
            }
            else
            {
                return __falseValue;
            }
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.emitConditional(this);
    }

    // @cons
    public ConditionalNode(StructuredGraph __graph, CanonicalCondition __condition, ValueNode __x, ValueNode __y)
    {
        this(CompareNode.createCompareNode(__graph, __condition, __x, __y, null, NodeView.DEFAULT));
    }
}
