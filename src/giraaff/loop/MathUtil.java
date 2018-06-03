package giraaff.loop;

import java.util.function.BiFunction;

import giraaff.core.common.type.IntegerStamp;
import giraaff.nodes.FixedNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.nodes.calc.FixedBinaryNode;
import giraaff.nodes.calc.SignedDivNode;
import giraaff.nodes.calc.UnsignedDivNode;

///
// Utility methods to perform integer math with some obvious constant folding first.
///
// @class MathUtil
public final class MathUtil
{
    // @cons
    private MathUtil()
    {
        super();
    }

    private static boolean isConstantOne(ValueNode __v1)
    {
        return __v1.isConstant() && __v1.stamp(NodeView.DEFAULT) instanceof IntegerStamp && __v1.asJavaConstant().asLong() == 1;
    }

    private static boolean isConstantZero(ValueNode __v1)
    {
        return __v1.isConstant() && __v1.stamp(NodeView.DEFAULT) instanceof IntegerStamp && __v1.asJavaConstant().asLong() == 0;
    }

    public static ValueNode add(StructuredGraph __graph, ValueNode __v1, ValueNode __v2)
    {
        if (isConstantZero(__v1))
        {
            return __v2;
        }
        if (isConstantZero(__v2))
        {
            return __v1;
        }
        return BinaryArithmeticNode.add(__graph, __v1, __v2, NodeView.DEFAULT);
    }

    public static ValueNode mul(StructuredGraph __graph, ValueNode __v1, ValueNode __v2)
    {
        if (isConstantOne(__v1))
        {
            return __v2;
        }
        if (isConstantOne(__v2))
        {
            return __v1;
        }
        return BinaryArithmeticNode.mul(__graph, __v1, __v2, NodeView.DEFAULT);
    }

    public static ValueNode sub(StructuredGraph __graph, ValueNode __v1, ValueNode __v2)
    {
        if (isConstantZero(__v2))
        {
            return __v1;
        }
        return BinaryArithmeticNode.sub(__graph, __v1, __v2, NodeView.DEFAULT);
    }

    public static ValueNode divBefore(StructuredGraph __graph, FixedNode __before, ValueNode __dividend, ValueNode __divisor)
    {
        return fixedDivBefore(__graph, __before, __dividend, __divisor, (__dend, __sor) -> SignedDivNode.create(__dend, __sor, NodeView.DEFAULT));
    }

    public static ValueNode unsignedDivBefore(StructuredGraph __graph, FixedNode __before, ValueNode __dividend, ValueNode __divisor)
    {
        return fixedDivBefore(__graph, __before, __dividend, __divisor, (__dend, __sor) -> UnsignedDivNode.create(__dend, __sor, NodeView.DEFAULT));
    }

    private static ValueNode fixedDivBefore(StructuredGraph __graph, FixedNode __before, ValueNode __dividend, ValueNode __divisor, BiFunction<ValueNode, ValueNode, ValueNode> __createDiv)
    {
        if (isConstantOne(__divisor))
        {
            return __dividend;
        }
        ValueNode __div = __graph.addOrUniqueWithInputs(__createDiv.apply(__dividend, __divisor));
        if (__div instanceof FixedBinaryNode)
        {
            FixedBinaryNode __fixedDiv = (FixedBinaryNode) __div;
            if (__before.predecessor() instanceof FixedBinaryNode)
            {
                FixedBinaryNode __binaryPredecessor = (FixedBinaryNode) __before.predecessor();
                if (__fixedDiv.dataFlowEquals(__binaryPredecessor))
                {
                    __fixedDiv.safeDelete();
                    return __binaryPredecessor;
                }
            }
            __graph.addBeforeFixed(__before, __fixedDiv);
        }
        return __div;
    }
}
