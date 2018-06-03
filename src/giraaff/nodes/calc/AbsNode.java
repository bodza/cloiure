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

///
// Absolute value.
///
// @class AbsNode
public final class AbsNode extends UnaryArithmeticNode<Abs> implements ArithmeticLIRLowerable, NarrowableArithmeticNode
{
    // @def
    public static final NodeClass<AbsNode> TYPE = NodeClass.create(AbsNode.class);

    // @cons
    public AbsNode(ValueNode __x)
    {
        super(TYPE, ArithmeticOpTable::getAbs, __x);
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

    protected static ValueNode findSynonym(ValueNode __forValue, NodeView __view)
    {
        ArithmeticOpTable.UnaryOp<Abs> __absOp = ArithmeticOpTable.forStamp(__forValue.stamp(__view)).getAbs();
        ValueNode __synonym = UnaryArithmeticNode.findSynonym(__forValue, __absOp);
        if (__synonym != null)
        {
            return __synonym;
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        ValueNode __ret = super.canonical(__tool, __forValue);
        if (__ret != this)
        {
            return __ret;
        }
        if (__forValue instanceof AbsNode)
        {
            return __forValue;
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitMathAbs(__nodeValueMap.operand(getValue())));
    }
}
