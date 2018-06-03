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
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.util.GraphUtil;

///
// Node representing an exact integer substraction that will throw an {@link ArithmeticException} in
// case the addition would overflow the 32 bit range.
///
// @class IntegerSubExactNode
public final class IntegerSubExactNode extends SubNode implements IntegerExactArithmeticNode
{
    // @def
    public static final NodeClass<IntegerSubExactNode> TYPE = NodeClass.create(IntegerSubExactNode.class);

    // @cons
    public IntegerSubExactNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, __x, __y);
        setStamp(__x.stamp(NodeView.DEFAULT).unrestricted());
    }

    @Override
    public boolean inferStamp()
    {
        // Note: it is not allowed to use the foldStamp method of the regular sub node as we do not know
        // the result stamp of this node if we do not know whether we may deopt. If we know we can never
        // overflow we will replace this node with its non overflow checking counterpart anyway.
        return false;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY))
        {
            return ConstantNode.forIntegerStamp(stamp(NodeView.DEFAULT), 0);
        }
        if (__forX.isConstant() && __forY.isConstant())
        {
            return canonicalXYconstant(__forX, __forY);
        }
        else if (__forY.isConstant())
        {
            long __c = __forY.asJavaConstant().asLong();
            if (__c == 0)
            {
                return __forX;
            }
        }
        if (!IntegerStamp.subtractionCanOverflow((IntegerStamp) this.___x.stamp(NodeView.DEFAULT), (IntegerStamp) this.___y.stamp(NodeView.DEFAULT)))
        {
            return new SubNode(this.___x, this.___y).canonical(__tool);
        }
        return this;
    }

    private ValueNode canonicalXYconstant(ValueNode __forX, ValueNode __forY)
    {
        JavaConstant __xConst = __forX.asJavaConstant();
        JavaConstant __yConst = __forY.asJavaConstant();
        try
        {
            if (__xConst.getJavaKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Math.subtractExact(__xConst.asInt(), __yConst.asInt()));
            }
            else
            {
                return ConstantNode.forLong(Math.subtractExact(__xConst.asLong(), __yConst.asLong()));
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
        return graph().add(new IntegerSubExactSplitNode(stamp(NodeView.DEFAULT), getX(), getY(), __next, __deopt));
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        IntegerExactArithmeticSplitNode.lower(__tool, this);
    }
}
