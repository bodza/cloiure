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

/**
 * The {@code ConditionalNode} class represents a comparison that yields one of two (eagerly
 * evaluated) values.
 */
public final class ConditionalNode extends FloatingNode implements Canonicalizable, LIRLowerable
{
    public static final NodeClass<ConditionalNode> TYPE = NodeClass.create(ConditionalNode.class);
    @Input(InputType.Condition) LogicNode condition;
    @Input(InputType.Value) ValueNode trueValue;
    @Input(InputType.Value) ValueNode falseValue;

    public LogicNode condition()
    {
        return condition;
    }

    public ConditionalNode(LogicNode condition)
    {
        this(condition, ConstantNode.forInt(1, condition.graph()), ConstantNode.forInt(0, condition.graph()));
    }

    public ConditionalNode(LogicNode condition, ValueNode trueValue, ValueNode falseValue)
    {
        super(TYPE, trueValue.stamp(NodeView.DEFAULT).meet(falseValue.stamp(NodeView.DEFAULT)));
        this.condition = condition;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    public static ValueNode create(LogicNode condition, NodeView view)
    {
        return create(condition, ConstantNode.forInt(1, condition.graph()), ConstantNode.forInt(0, condition.graph()), view);
    }

    public static ValueNode create(LogicNode condition, ValueNode trueValue, ValueNode falseValue, NodeView view)
    {
        ValueNode synonym = findSynonym(condition, trueValue, falseValue, view);
        if (synonym != null)
        {
            return synonym;
        }
        ValueNode result = canonicalizeConditional(condition, trueValue, falseValue, trueValue.stamp(view).meet(falseValue.stamp(view)), view);
        if (result != null)
        {
            return result;
        }
        return new ConditionalNode(condition, trueValue, falseValue);
    }

    @Override
    public boolean inferStamp()
    {
        Stamp valueStamp = trueValue.stamp(NodeView.DEFAULT).meet(falseValue.stamp(NodeView.DEFAULT));
        if (condition instanceof IntegerLessThanNode)
        {
            IntegerLessThanNode lessThan = (IntegerLessThanNode) condition;
            if (lessThan.getX() == trueValue && lessThan.getY() == falseValue)
            {
                // this encodes a min operation
                JavaConstant constant = lessThan.getX().asJavaConstant();
                if (constant == null)
                {
                    constant = lessThan.getY().asJavaConstant();
                }
                if (constant != null)
                {
                    IntegerStamp bounds = StampFactory.forInteger(constant.getJavaKind(), constant.getJavaKind().getMinValue(), constant.asLong());
                    valueStamp = valueStamp.join(bounds);
                }
            }
            else if (lessThan.getX() == falseValue && lessThan.getY() == trueValue)
            {
                // this encodes a max operation
                JavaConstant constant = lessThan.getX().asJavaConstant();
                if (constant == null)
                {
                    constant = lessThan.getY().asJavaConstant();
                }
                if (constant != null)
                {
                    IntegerStamp bounds = StampFactory.forInteger(constant.getJavaKind(), constant.asLong(), constant.getJavaKind().getMaxValue());
                    valueStamp = valueStamp.join(bounds);
                }
            }
        }
        return updateStamp(valueStamp);
    }

    public ValueNode trueValue()
    {
        return trueValue;
    }

    public ValueNode falseValue()
    {
        return falseValue;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool)
    {
        NodeView view = NodeView.from(tool);
        ValueNode synonym = findSynonym(condition, trueValue(), falseValue(), view);
        if (synonym != null)
        {
            return synonym;
        }

        ValueNode result = canonicalizeConditional(condition, trueValue(), falseValue(), stamp, view);
        if (result != null)
        {
            return result;
        }

        return this;
    }

    public static ValueNode canonicalizeConditional(LogicNode condition, ValueNode trueValue, ValueNode falseValue, Stamp stamp, NodeView view)
    {
        if (trueValue == falseValue)
        {
            return trueValue;
        }

        if (condition instanceof CompareNode && ((CompareNode) condition).isIdentityComparison())
        {
            // optimize the pattern (x == y) ? x : y
            CompareNode compare = (CompareNode) condition;
            if ((compare.getX() == trueValue && compare.getY() == falseValue) || (compare.getX() == falseValue && compare.getY() == trueValue))
            {
                return falseValue;
            }
        }

        if (trueValue.stamp(view) instanceof IntegerStamp)
        {
            // check if the conditional is redundant
            if (condition instanceof IntegerLessThanNode)
            {
                IntegerLessThanNode lessThan = (IntegerLessThanNode) condition;
                IntegerStamp falseValueStamp = (IntegerStamp) falseValue.stamp(view);
                IntegerStamp trueValueStamp = (IntegerStamp) trueValue.stamp(view);
                if (lessThan.getX() == trueValue && lessThan.getY() == falseValue)
                {
                    // return "x" for "x < y ? x : y" in case that we know "x <= y"
                    if (trueValueStamp.upperBound() <= falseValueStamp.lowerBound())
                    {
                        return trueValue;
                    }
                }
                else if (lessThan.getX() == falseValue && lessThan.getY() == trueValue)
                {
                    // return "y" for "x < y ? y : x" in case that we know "x <= y"
                    if (falseValueStamp.upperBound() <= trueValueStamp.lowerBound())
                    {
                        return trueValue;
                    }
                }
            }

            // this optimizes the case where a value from the range 0 - 1 is mapped to the
            // range 0 - 1
            if (trueValue.isConstant() && falseValue.isConstant())
            {
                long constTrueValue = trueValue.asJavaConstant().asLong();
                long constFalseValue = falseValue.asJavaConstant().asLong();
                if (condition instanceof IntegerEqualsNode)
                {
                    IntegerEqualsNode equals = (IntegerEqualsNode) condition;
                    if (equals.getY().isConstant() && equals.getX().stamp(view) instanceof IntegerStamp)
                    {
                        IntegerStamp equalsXStamp = (IntegerStamp) equals.getX().stamp(view);
                        if (equalsXStamp.upMask() == 1)
                        {
                            long equalsY = equals.getY().asJavaConstant().asLong();
                            if (equalsY == 0)
                            {
                                if (constTrueValue == 0 && constFalseValue == 1)
                                {
                                    // return x when: x == 0 ? 0 : 1;
                                    return IntegerConvertNode.convertUnsigned(equals.getX(), stamp, view);
                                }
                                else if (constTrueValue == 1 && constFalseValue == 0)
                                {
                                    // negate a boolean value via xor
                                    return IntegerConvertNode.convertUnsigned(XorNode.create(equals.getX(), ConstantNode.forIntegerStamp(equals.getX().stamp(view), 1), view), stamp, view);
                                }
                            }
                            else if (equalsY == 1)
                            {
                                if (constTrueValue == 1 && constFalseValue == 0)
                                {
                                    // return x when: x == 1 ? 1 : 0;
                                    return IntegerConvertNode.convertUnsigned(equals.getX(), stamp, view);
                                }
                                else if (constTrueValue == 0 && constFalseValue == 1)
                                {
                                    // negate a boolean value via xor
                                    return IntegerConvertNode.convertUnsigned(XorNode.create(equals.getX(), ConstantNode.forIntegerStamp(equals.getX().stamp(view), 1), view), stamp, view);
                                }
                            }
                        }
                    }
                }
                else if (condition instanceof IntegerTestNode)
                {
                    // replace IntegerTestNode with AndNode for the following patterns:
                    // (value & 1) == 0 ? 0 : 1
                    // (value & 1) == 1 ? 1 : 0
                    IntegerTestNode integerTestNode = (IntegerTestNode) condition;
                    if (integerTestNode.getY().isConstant())
                    {
                        long testY = integerTestNode.getY().asJavaConstant().asLong();
                        if (testY == 1 && constTrueValue == 0 && constFalseValue == 1)
                        {
                            return IntegerConvertNode.convertUnsigned(AndNode.create(integerTestNode.getX(), integerTestNode.getY(), view), stamp, view);
                        }
                    }
                }
            }

            if (condition instanceof IntegerLessThanNode)
            {
                // Convert a conditional add ((x < 0) ? (x + y) : x) into (x + (y & (x >> (bits - 1)))) to avoid the test.
                IntegerLessThanNode lt = (IntegerLessThanNode) condition;
                if (lt.getY().isConstant() && lt.getY().asConstant().isDefaultForKind())
                {
                    if (falseValue == lt.getX())
                    {
                        if (trueValue instanceof AddNode)
                        {
                            AddNode add = (AddNode) trueValue;
                            if (add.getX() == falseValue)
                            {
                                int bits = ((IntegerStamp) trueValue.stamp(NodeView.DEFAULT)).getBits();
                                ValueNode shift = new RightShiftNode(lt.getX(), ConstantNode.forIntegerBits(32, bits - 1));
                                ValueNode and = new AndNode(shift, add.getY());
                                return new AddNode(add.getX(), and);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static ValueNode findSynonym(ValueNode condition, ValueNode trueValue, ValueNode falseValue, NodeView view)
    {
        if (condition instanceof LogicNegationNode)
        {
            LogicNegationNode negated = (LogicNegationNode) condition;
            return ConditionalNode.create(negated.getValue(), falseValue, trueValue, view);
        }
        if (condition instanceof LogicConstantNode)
        {
            LogicConstantNode c = (LogicConstantNode) condition;
            if (c.getValue())
            {
                return trueValue;
            }
            else
            {
                return falseValue;
            }
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool generator)
    {
        generator.emitConditional(this);
    }

    public ConditionalNode(StructuredGraph graph, CanonicalCondition condition, ValueNode x, ValueNode y)
    {
        this(CompareNode.createCompareNode(graph, condition, x, y, null, NodeView.DEFAULT));
    }
}
