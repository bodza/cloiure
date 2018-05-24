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
public final class IntegerMulExactNode extends MulNode implements IntegerExactArithmeticNode
{
    public static final NodeClass<IntegerMulExactNode> TYPE = NodeClass.create(IntegerMulExactNode.class);

    public IntegerMulExactNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y);
        setStamp(x.stamp(NodeView.DEFAULT).unrestricted());
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
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        if (forX.isConstant() && !forY.isConstant())
        {
            return new IntegerMulExactNode(forY, forX).canonical(tool);
        }
        if (forX.isConstant())
        {
            return canonicalXconstant(forX, forY);
        }
        else if (forY.isConstant())
        {
            long c = forY.asJavaConstant().asLong();
            if (c == 1)
            {
                return forX;
            }
            if (c == 0)
            {
                return ConstantNode.forIntegerStamp(stamp(NodeView.DEFAULT), 0);
            }
        }
        if (!IntegerStamp.multiplicationCanOverflow((IntegerStamp) x.stamp(NodeView.DEFAULT), (IntegerStamp) y.stamp(NodeView.DEFAULT)))
        {
            return new MulNode(x, y).canonical(tool);
        }
        return this;
    }

    private ValueNode canonicalXconstant(ValueNode forX, ValueNode forY)
    {
        JavaConstant xConst = forX.asJavaConstant();
        JavaConstant yConst = forY.asJavaConstant();
        try
        {
            if (xConst.getJavaKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Math.multiplyExact(xConst.asInt(), yConst.asInt()));
            }
            else
            {
                return ConstantNode.forLong(Math.multiplyExact(xConst.asLong(), yConst.asLong()));
            }
        }
        catch (ArithmeticException ex)
        {
            // The operation will result in an overflow exception, so do not canonicalize.
        }
        return this;
    }

    @Override
    public IntegerExactArithmeticSplitNode createSplit(AbstractBeginNode next, AbstractBeginNode deopt)
    {
        return graph().add(new IntegerMulExactSplitNode(stamp(NodeView.DEFAULT), getX(), getY(), next, deopt));
    }

    @Override
    public void lower(LoweringTool tool)
    {
        IntegerExactArithmeticSplitNode.lower(tool, this);
    }
}
