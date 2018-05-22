package giraaff.nodes.calc;

import java.util.function.Function;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ArithmeticOperation;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;

public abstract class UnaryArithmeticNode<OP> extends UnaryNode implements ArithmeticOperation, ArithmeticLIRLowerable
{
    @SuppressWarnings("rawtypes") public static final NodeClass<UnaryArithmeticNode> TYPE = NodeClass.create(UnaryArithmeticNode.class);

    protected interface SerializableUnaryFunction<T> extends Function<ArithmeticOpTable, UnaryOp<T>>
    {
    }

    protected final SerializableUnaryFunction<OP> getOp;

    protected UnaryArithmeticNode(NodeClass<? extends UnaryArithmeticNode<OP>> c, SerializableUnaryFunction<OP> getOp, ValueNode value)
    {
        super(c, getOp.apply(ArithmeticOpTable.forStamp(value.stamp(NodeView.DEFAULT))).foldStamp(value.stamp(NodeView.DEFAULT)), value);
        this.getOp = getOp;
    }

    protected final UnaryOp<OP> getOp(ValueNode forValue)
    {
        return getOp.apply(ArithmeticOpTable.forStamp(forValue.stamp(NodeView.DEFAULT)));
    }

    @Override
    public final UnaryOp<OP> getArithmeticOp()
    {
        return getOp(getValue());
    }

    @Override
    public Stamp foldStamp(Stamp newStamp)
    {
        return getOp(getValue()).foldStamp(newStamp);
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

    protected static <OP> ValueNode findSynonym(ValueNode forValue, UnaryOp<OP> op)
    {
        if (forValue.isConstant())
        {
            return ConstantNode.forPrimitive(op.foldStamp(forValue.stamp(NodeView.DEFAULT)), op.foldConstant(forValue.asConstant()));
        }
        return null;
    }
}
