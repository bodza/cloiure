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

///
// Binary negation of long or integer values.
///
// @class NotNode
public final class NotNode extends UnaryArithmeticNode<Not> implements ArithmeticLIRLowerable, NarrowableArithmeticNode, StampInverter
{
    // @def
    public static final NodeClass<NotNode> TYPE = NodeClass.create(NotNode.class);

    // @cons
    protected NotNode(ValueNode __x)
    {
        super(TYPE, ArithmeticOpTable::getNot, __x);
    }

    public static ValueNode create(ValueNode __x)
    {
        return canonicalize(null, __x);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        ValueNode __ret = super.canonical(__tool, __forValue);
        if (__ret != this)
        {
            return __ret;
        }
        return canonicalize(this, __forValue);
    }

    private static ValueNode canonicalize(NotNode __node, ValueNode __x)
    {
        if (__x instanceof NotNode)
        {
            return ((NotNode) __x).getValue();
        }
        if (__node != null)
        {
            return __node;
        }
        return new NotNode(__x);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitNot(__nodeValueMap.operand(getValue())));
    }

    @Override
    public Stamp invertStamp(Stamp __outStamp)
    {
        return getArithmeticOp().foldStamp(__outStamp);
    }
}
