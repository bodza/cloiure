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

/**
 * Utility methods to perform integer math with some obvious constant folding first.
 */
// @class MathUtil
public final class MathUtil
{
    // @cons
    private MathUtil()
    {
        super();
    }

    private static boolean isConstantOne(ValueNode v1)
    {
        return v1.isConstant() && v1.stamp(NodeView.DEFAULT) instanceof IntegerStamp && v1.asJavaConstant().asLong() == 1;
    }

    private static boolean isConstantZero(ValueNode v1)
    {
        return v1.isConstant() && v1.stamp(NodeView.DEFAULT) instanceof IntegerStamp && v1.asJavaConstant().asLong() == 0;
    }

    public static ValueNode add(StructuredGraph graph, ValueNode v1, ValueNode v2)
    {
        if (isConstantZero(v1))
        {
            return v2;
        }
        if (isConstantZero(v2))
        {
            return v1;
        }
        return BinaryArithmeticNode.add(graph, v1, v2, NodeView.DEFAULT);
    }

    public static ValueNode mul(StructuredGraph graph, ValueNode v1, ValueNode v2)
    {
        if (isConstantOne(v1))
        {
            return v2;
        }
        if (isConstantOne(v2))
        {
            return v1;
        }
        return BinaryArithmeticNode.mul(graph, v1, v2, NodeView.DEFAULT);
    }

    public static ValueNode sub(StructuredGraph graph, ValueNode v1, ValueNode v2)
    {
        if (isConstantZero(v2))
        {
            return v1;
        }
        return BinaryArithmeticNode.sub(graph, v1, v2, NodeView.DEFAULT);
    }

    public static ValueNode divBefore(StructuredGraph graph, FixedNode before, ValueNode dividend, ValueNode divisor)
    {
        return fixedDivBefore(graph, before, dividend, divisor, (dend, sor) -> SignedDivNode.create(dend, sor, NodeView.DEFAULT));
    }

    public static ValueNode unsignedDivBefore(StructuredGraph graph, FixedNode before, ValueNode dividend, ValueNode divisor)
    {
        return fixedDivBefore(graph, before, dividend, divisor, (dend, sor) -> UnsignedDivNode.create(dend, sor, NodeView.DEFAULT));
    }

    private static ValueNode fixedDivBefore(StructuredGraph graph, FixedNode before, ValueNode dividend, ValueNode divisor, BiFunction<ValueNode, ValueNode, ValueNode> createDiv)
    {
        if (isConstantOne(divisor))
        {
            return dividend;
        }
        ValueNode div = graph.addOrUniqueWithInputs(createDiv.apply(dividend, divisor));
        if (div instanceof FixedBinaryNode)
        {
            FixedBinaryNode fixedDiv = (FixedBinaryNode) div;
            if (before.predecessor() instanceof FixedBinaryNode)
            {
                FixedBinaryNode binaryPredecessor = (FixedBinaryNode) before.predecessor();
                if (fixedDiv.dataFlowEquals(binaryPredecessor))
                {
                    fixedDiv.safeDelete();
                    return binaryPredecessor;
                }
            }
            graph.addBeforeFixed(before, fixedDiv);
        }
        return div;
    }
}
