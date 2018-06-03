package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.IntegerStamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.MulNode;
import giraaff.nodes.spi.LoweringTool;

/**
 * Node representing an exact integer multiplication that will throw an {@link ArithmeticException}
 * in case the addition would overflow the 32 bit range.
 */
// @class IntegerMulExactNode
public final class IntegerMulExactNode extends MulNode implements IntegerExactArithmeticNode
{
    // @def
    public static final NodeClass<IntegerMulExactNode> TYPE = NodeClass.create(IntegerMulExactNode.class);

    // @cons
    public IntegerMulExactNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, __x, __y);
        setStamp(__x.stamp(NodeView.DEFAULT).unrestricted());
    }

    @Override
    public boolean inferStamp()
    {
        /*
         * Note: it is not allowed to use the foldStamp method of the regular mul node as we do not know
         * the result stamp of this node if we do not know whether we may deopt. If we know we can never
         * overflow we will replace this node with its non overflow checking counterpart anyway.
         */
        return false;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        if (__forX.isConstant() && !__forY.isConstant())
        {
            return new IntegerMulExactNode(__forY, __forX).canonical(__tool);
        }
        if (__forX.isConstant())
        {
            return canonicalXconstant(__forX, __forY);
        }
        else if (__forY.isConstant())
        {
            long __c = __forY.asJavaConstant().asLong();
            if (__c == 1)
            {
                return __forX;
            }
            if (__c == 0)
            {
                return ConstantNode.forIntegerStamp(stamp(NodeView.DEFAULT), 0);
            }
        }
        if (!IntegerStamp.multiplicationCanOverflow((IntegerStamp) x.stamp(NodeView.DEFAULT), (IntegerStamp) y.stamp(NodeView.DEFAULT)))
        {
            return new MulNode(x, y).canonical(__tool);
        }
        return this;
    }

    private ValueNode canonicalXconstant(ValueNode __forX, ValueNode __forY)
    {
        JavaConstant __xConst = __forX.asJavaConstant();
        JavaConstant __yConst = __forY.asJavaConstant();
        try
        {
            if (__xConst.getJavaKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Math.multiplyExact(__xConst.asInt(), __yConst.asInt()));
            }
            else
            {
                return ConstantNode.forLong(Math.multiplyExact(__xConst.asLong(), __yConst.asLong()));
            }
        }
        catch (ArithmeticException __ex)
        {
            // The operation will result in an overflow exception, so do not canonicalize.
        }
        return this;
    }

    @Override
    public IntegerExactArithmeticSplitNode createSplit(AbstractBeginNode __next, AbstractBeginNode __deopt)
    {
        return graph().add(new IntegerMulExactSplitNode(stamp(NodeView.DEFAULT), getX(), getY(), __next, __deopt));
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        IntegerExactArithmeticSplitNode.lower(__tool, this);
    }
}
