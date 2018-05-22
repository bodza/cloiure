package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Neg;
import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.StampInverter;

/**
 * The {@code NegateNode} node negates its operand.
 */
public final class NegateNode extends UnaryArithmeticNode<Neg> implements NarrowableArithmeticNode, StampInverter
{
    public static final NodeClass<NegateNode> TYPE = NodeClass.create(NegateNode.class);

    public NegateNode(ValueNode value)
    {
        super(TYPE, ArithmeticOpTable::getNeg, value);
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

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode synonym = findSynonym(forValue, getOp(forValue));
        if (synonym != null)
        {
            return synonym;
        }
        return this;
    }

    protected static ValueNode findSynonym(ValueNode forValue, NodeView view)
    {
        ArithmeticOpTable.UnaryOp<Neg> negOp = ArithmeticOpTable.forStamp(forValue.stamp(view)).getNeg();
        ValueNode synonym = UnaryArithmeticNode.findSynonym(forValue, negOp);
        if (synonym != null)
        {
            return synonym;
        }
        if (forValue instanceof NegateNode)
        {
            return ((NegateNode) forValue).getValue();
        }
        if (forValue instanceof SubNode && !(forValue.stamp(view) instanceof FloatStamp))
        {
            SubNode sub = (SubNode) forValue;
            return SubNode.create(sub.getY(), sub.getX(), view);
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitNegate(nodeValueMap.operand(getValue())));
    }

    @Override
    public Stamp invertStamp(Stamp outStamp)
    {
        return getArithmeticOp().foldStamp(outStamp);
    }
}
