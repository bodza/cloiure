package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_1;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import java.io.Serializable;
import java.util.function.Function;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.ShiftOp;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ArithmeticOperation;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * The {@code ShiftOp} class represents shift operations.
 */
@NodeInfo(cycles = CYCLES_1, size = SIZE_1)
public abstract class ShiftNode<OP> extends BinaryNode implements ArithmeticOperation, ArithmeticLIRLowerable, NarrowableArithmeticNode
{
    @SuppressWarnings("rawtypes") public static final NodeClass<ShiftNode> TYPE = NodeClass.create(ShiftNode.class);

    protected interface SerializableShiftFunction<T> extends Function<ArithmeticOpTable, ShiftOp<T>>, Serializable
    {
    }

    protected final SerializableShiftFunction<OP> getOp;

    /**
     * Creates a new shift operation.
     *
     * @param x the first input value
     * @param s the second input value
     */
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
         * Shifts are special because narrowing them also changes the implicit mask of the shift
         * amount. We can narrow only if (y & wideMask) == (y & narrowMask) for all possible values
         * of y.
         */
        IntegerStamp yStamp = (IntegerStamp) getY().stamp(NodeView.DEFAULT);
        return (yStamp.upMask() & (wideMask & ~narrowMask)) == 0;
    }
}
