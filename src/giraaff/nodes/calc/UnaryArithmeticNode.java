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

// @class UnaryArithmeticNode
public abstract class UnaryArithmeticNode<OP> extends UnaryNode implements ArithmeticOperation, ArithmeticLIRLowerable
{
    @SuppressWarnings("rawtypes")
    // @def
    public static final NodeClass<UnaryArithmeticNode> TYPE = NodeClass.create(UnaryArithmeticNode.class);

    // @iface UnaryArithmeticNode.SerializableUnaryFunction
    protected interface SerializableUnaryFunction<T> extends Function<ArithmeticOpTable, UnaryOp<T>>
    {
    }

    // @field
    protected final SerializableUnaryFunction<OP> ___getOp;

    // @cons
    protected UnaryArithmeticNode(NodeClass<? extends UnaryArithmeticNode<OP>> __c, SerializableUnaryFunction<OP> __getOp, ValueNode __value)
    {
        super(__c, __getOp.apply(ArithmeticOpTable.forStamp(__value.stamp(NodeView.DEFAULT))).foldStamp(__value.stamp(NodeView.DEFAULT)), __value);
        this.___getOp = __getOp;
    }

    protected final UnaryOp<OP> getOp(ValueNode __forValue)
    {
        return this.___getOp.apply(ArithmeticOpTable.forStamp(__forValue.stamp(NodeView.DEFAULT)));
    }

    @Override
    public final UnaryOp<OP> getArithmeticOp()
    {
        return getOp(getValue());
    }

    @Override
    public Stamp foldStamp(Stamp __newStamp)
    {
        return getOp(getValue()).foldStamp(__newStamp);
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

    protected static <OP> ValueNode findSynonym(ValueNode __forValue, UnaryOp<OP> __op)
    {
        if (__forValue.isConstant())
        {
            return ConstantNode.forPrimitive(__op.foldStamp(__forValue.stamp(NodeView.DEFAULT)), __op.foldConstant(__forValue.asConstant()));
        }
        return null;
    }
}
