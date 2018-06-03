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

///
// The {@code ShiftOp} class represents shift operations.
///
// @class ShiftNode
public abstract class ShiftNode<OP> extends BinaryNode implements ArithmeticOperation, ArithmeticLIRLowerable, NarrowableArithmeticNode
{
    @SuppressWarnings("rawtypes")
    // @def
    public static final NodeClass<ShiftNode> TYPE = NodeClass.create(ShiftNode.class);

    // @iface ShiftNode.SerializableShiftFunction
    protected interface SerializableShiftFunction<T> extends Function<ArithmeticOpTable, ShiftOp<T>>
    {
    }

    // @field
    protected final SerializableShiftFunction<OP> ___getOp;

    ///
    // Creates a new shift operation.
    //
    // @param x the first input value
    // @param s the second input value
    ///
    // @cons
    protected ShiftNode(NodeClass<? extends ShiftNode<OP>> __c, SerializableShiftFunction<OP> __getOp, ValueNode __x, ValueNode __s)
    {
        super(__c, __getOp.apply(ArithmeticOpTable.forStamp(__x.stamp(NodeView.DEFAULT))).foldStamp(__x.stamp(NodeView.DEFAULT), (IntegerStamp) __s.stamp(NodeView.DEFAULT)), __x, __s);
        this.___getOp = __getOp;
    }

    protected final ShiftOp<OP> getOp(ValueNode __forValue)
    {
        return this.___getOp.apply(ArithmeticOpTable.forStamp(__forValue.stamp(NodeView.DEFAULT)));
    }

    @Override
    public final ShiftOp<OP> getArithmeticOp()
    {
        return getOp(getX());
    }

    @Override
    public Stamp foldStamp(Stamp __stampX, Stamp __stampY)
    {
        return getArithmeticOp().foldStamp(__stampX, (IntegerStamp) __stampY);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __valueNode = canonical(getOp(__forX), stamp(NodeView.DEFAULT), __forX, __forY, __view);
        if (__valueNode != null)
        {
            return __valueNode;
        }
        return this;
    }

    @SuppressWarnings("unused")
    public static <OP> ValueNode canonical(ShiftOp<OP> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        if (__forX.isConstant() && __forY.isConstant())
        {
            JavaConstant __amount = __forY.asJavaConstant();
            return ConstantNode.forPrimitive(__stamp, __op.foldConstant(__forX.asConstant(), __amount.asInt()));
        }
        return null;
    }

    public int getShiftAmountMask()
    {
        return getArithmeticOp().getShiftAmountMask(stamp(NodeView.DEFAULT));
    }

    @Override
    public boolean isNarrowable(int __resultBits)
    {
        int __narrowMask = __resultBits - 1;
        int __wideMask = getShiftAmountMask();

        // Shifts are special because narrowing them also changes the implicit mask of the shift amount.
        // We can narrow only if (y & wideMask) == (y & narrowMask) for all possible values of y.
        IntegerStamp __yStamp = (IntegerStamp) getY().stamp(NodeView.DEFAULT);
        return (__yStamp.upMask() & (__wideMask & ~__narrowMask)) == 0;
    }
}
