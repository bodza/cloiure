package giraaff.nodes.calc;

import java.util.function.Function;

import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.ShiftOp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ArithmeticOperation;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArithmeticLIRLowerable;

/**
 * The {@code ShiftOp} class represents shift operations.
 */
// @class ShiftNode
public abstract class ShiftNode<OP> extends BinaryNode implements ArithmeticOperation, ArithmeticLIRLowerable, NarrowableArithmeticNode
{
    @SuppressWarnings("rawtypes") public static final NodeClass<ShiftNode> TYPE = NodeClass.create(ShiftNode.class);

    // @iface ShiftNode.SerializableShiftFunction
    protected interface SerializableShiftFunction<T> extends Function<ArithmeticOpTable, ShiftOp<T>>
    {
    }

    protected final SerializableShiftFunction<OP> getOp;

    /**
     * Creates a new shift operation.
     *
     * @param x the first input value
     * @param s the second input value
     */
    // @cons
    protected ShiftNode(NodeClass<? extends ShiftNode<OP>> c, SerializableShiftFunction<OP> getOp, ValueNode x, ValueNode s)
    {
        super(c, getOp.apply(ArithmeticOpTable.forStamp(x.stamp(NodeView.DEFAULT))).foldStamp(x.stamp(NodeView.DEFAULT), (IntegerStamp) s.stamp(NodeView.DEFAULT)), x, s);
        this.getOp = getOp;
    }

    protected final ShiftOp<OP> getOp(ValueNode forValue)
    {
        return getOp.apply(ArithmeticOpTable.forStamp(forValue.stamp(NodeView.DEFAULT)));
    }

    @Override
    public final ShiftOp<OP> getArithmeticOp()
    {
        return getOp(getX());
    }

    @Override
    public Stamp foldStamp(Stamp stampX, Stamp stampY)
    {
        return getArithmeticOp().foldStamp(stampX, (IntegerStamp) stampY);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode valueNode = canonical(getOp(forX), stamp(NodeView.DEFAULT), forX, forY, view);
        if (valueNode != null)
        {
            return valueNode;
        }
        return this;
    }

    @SuppressWarnings("unused")
    public static <OP> ValueNode canonical(ShiftOp<OP> op, Stamp stamp, ValueNode forX, ValueNode forY, NodeView view)
    {
        if (forX.isConstant() && forY.isConstant())
        {
            JavaConstant amount = forY.asJavaConstant();
            return ConstantNode.forPrimitive(stamp, op.foldConstant(forX.asConstant(), amount.asInt()));
        }
        return null;
    }

    public int getShiftAmountMask()
    {
        return getArithmeticOp().getShiftAmountMask(stamp(NodeView.DEFAULT));
    }

    @Override
    public boolean isNarrowable(int resultBits)
    {
        int narrowMask = resultBits - 1;
        int wideMask = getShiftAmountMask();

        /*
         * Shifts are special because narrowing them also changes the implicit mask of the shift amount.
         * We can narrow only if (y & wideMask) == (y & narrowMask) for all possible values of y.
         */
        IntegerStamp yStamp = (IntegerStamp) getY().stamp(NodeView.DEFAULT);
        return (yStamp.upMask() & (wideMask & ~narrowMask)) == 0;
    }
}
