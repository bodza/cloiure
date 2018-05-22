package giraaff.nodes.calc;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.ShiftOp.Shl;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public final class LeftShiftNode extends ShiftNode<Shl>
{
    public static final NodeClass<LeftShiftNode> TYPE = NodeClass.create(LeftShiftNode.class);

    public LeftShiftNode(ValueNode x, ValueNode y)
    {
        super(TYPE, ArithmeticOpTable::getShl, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        ArithmeticOpTable.ShiftOp<Shl> op = ArithmeticOpTable.forStamp(x.stamp(view)).getShl();
        Stamp stamp = op.foldStamp(x.stamp(view), (IntegerStamp) y.stamp(view));
        ValueNode value = ShiftNode.canonical(op, stamp, x, y, view);
        if (value != null)
        {
            return value;
        }

        return canonical(null, op, stamp, x, y);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        ValueNode ret = super.canonical(tool, forX, forY);
        if (ret != this)
        {
            return ret;
        }

        return canonical(this, getArithmeticOp(), stamp(NodeView.DEFAULT), forX, forY);
    }

    private static ValueNode canonical(LeftShiftNode leftShiftNode, ArithmeticOpTable.ShiftOp<Shl> op, Stamp stamp, ValueNode forX, ValueNode forY)
    {
        LeftShiftNode self = leftShiftNode;
        if (forY.isConstant())
        {
            int amount = forY.asJavaConstant().asInt();
            int originalAmount = amount;
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
                    if (other instanceof LeftShiftNode)
                    {
                        int total = amount + otherAmount;
                        if (total != (total & mask))
                        {
                            return ConstantNode.forIntegerKind(stamp.getStackKind(), 0);
                        }
                        return new LeftShiftNode(other.getX(), ConstantNode.forInt(total));
                    }
                    else if ((other instanceof RightShiftNode || other instanceof UnsignedRightShiftNode) && otherAmount == amount)
                    {
                        if (stamp.getStackKind() == JavaKind.Long)
                        {
                            return new AndNode(other.getX(), ConstantNode.forLong(-1L << amount));
                        }
                        else
                        {
                            return new AndNode(other.getX(), ConstantNode.forInt(-1 << amount));
                        }
                    }
                }
            }
            if (originalAmount != amount)
            {
                return new LeftShiftNode(forX, ConstantNode.forInt(amount));
            }
        }
        if (self == null)
        {
            self = new LeftShiftNode(forX, forY);
        }
        return self;
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitShl(nodeValueMap.operand(getX()), nodeValueMap.operand(getY())));
    }
}
