package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Abs;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Absolute value.
 */
// @class AbsNode
public final class AbsNode extends UnaryArithmeticNode<Abs> implements ArithmeticLIRLowerable, NarrowableArithmeticNode
{
    public static final NodeClass<AbsNode> TYPE = NodeClass.create(AbsNode.class);

    // @cons
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
