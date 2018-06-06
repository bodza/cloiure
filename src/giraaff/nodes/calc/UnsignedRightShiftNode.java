package giraaff.nodes.calc;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class UnsignedRightShiftNode
public final class UnsignedRightShiftNode extends ShiftNode<ArithmeticOpTable.ShiftOp.UShr>
{
    // @def
    public static final NodeClass<UnsignedRightShiftNode> TYPE = NodeClass.create(UnsignedRightShiftNode.class);

    // @cons UnsignedRightShiftNode
    public UnsignedRightShiftNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, ArithmeticOpTable::getUShr, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.UShr> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getUShr();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), (IntegerStamp) __y.stamp(__view));
        ValueNode __value = ShiftNode.canonical(__op, __stamp, __x, __y, __view);
        if (__value != null)
        {
            return __value;
        }

        return canonical(null, __op, __stamp, __x, __y, __view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        return canonical(this, this.getArithmeticOp(), this.stamp(__view), __forX, __forY, __view);
    }

    @SuppressWarnings("unused")
    private static ValueNode canonical(UnsignedRightShiftNode __node, ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.UShr> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        if (__forY.isConstant())
        {
            int __amount = __forY.asJavaConstant().asInt();
            int __originalAmout = __amount;
            int __mask = __op.getShiftAmountMask(__stamp);
            __amount &= __mask;
            if (__amount == 0)
            {
                return __forX;
            }
            if (__forX instanceof ShiftNode)
            {
                ShiftNode<?> __other = (ShiftNode<?>) __forX;
                if (__other.getY().isConstant())
                {
                    int __otherAmount = __other.getY().asJavaConstant().asInt() & __mask;
                    if (__other instanceof UnsignedRightShiftNode)
                    {
                        int __total = __amount + __otherAmount;
                        if (__total != (__total & __mask))
                        {
                            return ConstantNode.forIntegerKind(__stamp.getStackKind(), 0);
                        }
                        return new UnsignedRightShiftNode(__other.getX(), ConstantNode.forInt(__total));
                    }
                    else if (__other instanceof LeftShiftNode && __otherAmount == __amount)
                    {
                        if (__stamp.getStackKind() == JavaKind.Long)
                        {
                            return new AndNode(__other.getX(), ConstantNode.forLong(-1L >>> __amount));
                        }
                        else
                        {
                            return new AndNode(__other.getX(), ConstantNode.forInt(-1 >>> __amount));
                        }
                    }
                }
            }
            if (__originalAmout != __amount)
            {
                return new UnsignedRightShiftNode(__forX, ConstantNode.forInt(__amount));
            }
        }

        if (__node != null)
        {
            return __node;
        }
        return new UnsignedRightShiftNode(__forX, __forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitUShr(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY())));
    }

    @Override
    public boolean isNarrowable(int __resultBits)
    {
        if (super.isNarrowable(__resultBits))
        {
            // For unsigned right shifts, the narrow can be done before the shift if the cut off bits are all zero.
            IntegerStamp __inputStamp = (IntegerStamp) getX().stamp(NodeView.DEFAULT);
            return (__inputStamp.upMask() & ~(__resultBits - 1)) == 0;
        }
        else
        {
            return false;
        }
    }
}
