package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.UnaryOp.Abs;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Absolute value.
 */
public final class AbsNode extends UnaryArithmeticNode<Abs> implements ArithmeticLIRLowerable, NarrowableArithmeticNode
{
    public static final NodeClass<AbsNode> TYPE = NodeClass.create(AbsNode.class);

    public AbsNode(ValueNode x)
    {
        super(TYPE, ArithmeticOpTable::getAbs, x);
    }

    public static ValueNode create(ValueNode value, NodeView view)
    {
        ValueNode synonym = findSynonym(value, view);
        if (synonym != null)
        {
            return synonym;
        }
        return new NegateNode(value);
    }

    protected static ValueNode findSynonym(ValueNode forValue, NodeView view)
    {
        ArithmeticOpTable.UnaryOp<Abs> absOp = ArithmeticOpTable.forStamp(forValue.stamp(view)).getAbs();
        ValueNode synonym = UnaryArithmeticNode.findSynonym(forValue, absOp);
        if (synonym != null)
        {
            return synonym;
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode ret = super.canonical(tool, forValue);
        if (ret != this)
        {
            return ret;
        }
        if (forValue instanceof AbsNode)
        {
            return forValue;
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitMathAbs(nodeValueMap.operand(getValue())));
    }
}
