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

// @class LeftShiftNode
public final class LeftShiftNode extends ShiftNode<Shl>
{
    // @def
    public static final NodeClass<LeftShiftNode> TYPE = NodeClass.create(LeftShiftNode.class);

    // @cons
    public LeftShiftNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, ArithmeticOpTable::getShl, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        ArithmeticOpTable.ShiftOp<Shl> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getShl();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), (IntegerStamp) __y.stamp(__view));
        ValueNode __value = ShiftNode.canonical(__op, __stamp, __x, __y, __view);
        if (__value != null)
        {
            return __value;
        }

        return canonical(null, __op, __stamp, __x, __y);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        return canonical(this, getArithmeticOp(), stamp(NodeView.DEFAULT), __forX, __forY);
    }

    private static ValueNode canonical(LeftShiftNode __leftShiftNode, ArithmeticOpTable.ShiftOp<Shl> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY)
    {
        LeftShiftNode __self = __leftShiftNode;
        if (__forY.isConstant())
        {
            int __amount = __forY.asJavaConstant().asInt();
            int __originalAmount = __amount;
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
                    if (__other instanceof LeftShiftNode)
                    {
                        int __total = __amount + __otherAmount;
                        if (__total != (__total & __mask))
                        {
                            return ConstantNode.forIntegerKind(__stamp.getStackKind(), 0);
                        }
                        return new LeftShiftNode(__other.getX(), ConstantNode.forInt(__total));
                    }
                    else if ((__other instanceof RightShiftNode || __other instanceof UnsignedRightShiftNode) && __otherAmount == __amount)
                    {
                        if (__stamp.getStackKind() == JavaKind.Long)
                        {
                            return new AndNode(__other.getX(), ConstantNode.forLong(-1L << __amount));
                        }
                        else
                        {
                            return new AndNode(__other.getX(), ConstantNode.forInt(-1 << __amount));
                        }
                    }
                }
            }
            if (__originalAmount != __amount)
            {
                return new LeftShiftNode(__forX, ConstantNode.forInt(__amount));
            }
        }
        if (__self == null)
        {
            __self = new LeftShiftNode(__forX, __forY);
        }
        return __self;
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitShl(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY())));
    }
}
