package giraaff.nodes.calc;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.ShiftOp.UShr;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public final class UnsignedRightShiftNode extends ShiftNode<UShr>
{
    public static final NodeClass<UnsignedRightShiftNode> TYPE = NodeClass.create(UnsignedRightShiftNode.class);

    public UnsignedRightShiftNode(ValueNode x, ValueNode y)
    {
        super(TYPE, ArithmeticOpTable::getUShr, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        ArithmeticOpTable.ShiftOp<UShr> op = ArithmeticOpTable.forStamp(x.stamp(view)).getUShr();
        Stamp stamp = op.foldStamp(x.stamp(view), (IntegerStamp) y.stamp(view));
        ValueNode value = ShiftNode.canonical(op, stamp, x, y, view);
        if (value != null)
        {
            return value;
        }

        return canonical(null, op, stamp, x, y, view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode ret = super.canonical(tool, forX, forY);
        if (ret != this)
        {
            return ret;
        }

        return canonical(this, this.getArithmeticOp(), this.stamp(view), forX, forY, view);
    }

    @SuppressWarnings("unused")
    private static ValueNode canonical(UnsignedRightShiftNode node, ArithmeticOpTable.ShiftOp<UShr> op, Stamp stamp, ValueNode forX, ValueNode forY, NodeView view)
    {
        if (forY.isConstant())
        {
            int amount = forY.asJavaConstant().asInt();
            int originalAmout = amount;
            int mask = op.getShiftAmountMask(stamp);
            amount &= mask;
            if (amount == 0)
            {
                return forX;
            }
            if (forX instanceof ShiftNode)
            {
                ShiftNode<?> other = (ShiftNode<?>) forX;
                if (other.getY().isConstant())
                {
                    int otherAmount = other.getY().asJavaConstant().asInt() & mask;
                    if (other instanceof UnsignedRightShiftNode)
                    {
                        int total = amount + otherAmount;
                        if (total != (total & mask))
                        {
                            return ConstantNode.forIntegerKind(stamp.getStackKind(), 0);
                        }
                        return new UnsignedRightShiftNode(other.getX(), ConstantNode.forInt(total));
                    }
                    else if (other instanceof LeftShiftNode && otherAmount == amount)
                    {
                        if (stamp.getStackKind() == JavaKind.Long)
                        {
                            return new AndNode(other.getX(), ConstantNode.forLong(-1L >>> amount));
                        }
                        else
                        {
                            return new AndNode(other.getX(), ConstantNode.forInt(-1 >>> amount));
                        }
                    }
                }
            }
            if (originalAmout != amount)
            {
                return new UnsignedRightShiftNode(forX, ConstantNode.forInt(amount));
            }
        }

        if (node != null)
        {
            return node;
        }
        return new UnsignedRightShiftNode(forX, forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitUShr(nodeValueMap.operand(getX()), nodeValueMap.operand(getY())));
    }

    @Override
    public boolean isNarrowable(int resultBits)
    {
        if (super.isNarrowable(resultBits))
        {
            // For unsigned right shifts, the narrow can be done before the shift if the cut off bits are all zero.
            IntegerStamp inputStamp = (IntegerStamp) getX().stamp(NodeView.DEFAULT);
            return (inputStamp.upMask() & ~(resultBits - 1)) == 0;
        }
        else
        {
            return false;
        }
    }
}
