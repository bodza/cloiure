package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.StampInverter;

///
// The {@code NegateNode} node negates its operand.
///
// @class NegateNode
public final class NegateNode extends UnaryArithmeticNode<ArithmeticOpTable.UnaryOp.Neg> implements NarrowableArithmeticNode, StampInverter
{
    // @def
    public static final NodeClass<NegateNode> TYPE = NodeClass.create(NegateNode.class);

    // @cons NegateNode
    public NegateNode(ValueNode __value)
    {
        super(TYPE, ArithmeticOpTable::getNeg, __value);
    }

    public static ValueNode create(ValueNode __value, NodeView __view)
    {
        ValueNode __synonym = findSynonym(__value, __view);
        if (__synonym != null)
        {
            return __synonym;
        }
        return new NegateNode(__value);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        ValueNode __synonym = findSynonym(__forValue, getOp(__forValue));
        if (__synonym != null)
        {
            return __synonym;
        }
        return this;
    }

    protected static ValueNode findSynonym(ValueNode __forValue, NodeView __view)
    {
        ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Neg> __negOp = ArithmeticOpTable.forStamp(__forValue.stamp(__view)).getNeg();
        ValueNode __synonym = UnaryArithmeticNode.findSynonym(__forValue, __negOp);
        if (__synonym != null)
        {
            return __synonym;
        }
        if (__forValue instanceof NegateNode)
        {
            return ((NegateNode) __forValue).getValue();
        }
        if (__forValue instanceof SubNode)
        {
            SubNode __sub = (SubNode) __forValue;
            return SubNode.create(__sub.getY(), __sub.getX(), __view);
        }
        return null;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitNegate(__nodeValueMap.operand(getValue())));
    }

    @Override
    public Stamp invertStamp(Stamp __outStamp)
    {
        return getArithmeticOp().foldStamp(__outStamp);
    }
}
