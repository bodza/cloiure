package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Not;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.StampInverter;

/**
 * Binary negation of long or integer values.
 */
// @class NotNode
public final class NotNode extends UnaryArithmeticNode<Not> implements ArithmeticLIRLowerable, NarrowableArithmeticNode, StampInverter
{
    public static final NodeClass<NotNode> TYPE = NodeClass.create(NotNode.class);

    // @cons
    protected NotNode(ValueNode x)
    {
        super(TYPE, ArithmeticOpTable::getNot, x);
    }

    public static ValueNode create(ValueNode x)
    {
        return canonicalize(null, x);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        ValueNode ret = super.canonical(tool, forValue);
        if (ret != this)
        {
            return ret;
        }
        return canonicalize(this, forValue);
    }

    private static ValueNode canonicalize(NotNode node, ValueNode x)
    {
        if (x instanceof NotNode)
        {
            return ((NotNode) x).getValue();
        }
        if (node != null)
        {
            return node;
        }
        return new NotNode(x);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitNot(nodeValueMap.operand(getValue())));
    }

    @Override
    public Stamp invertStamp(Stamp outStamp)
    {
        return getArithmeticOp().foldStamp(outStamp);
    }
}
