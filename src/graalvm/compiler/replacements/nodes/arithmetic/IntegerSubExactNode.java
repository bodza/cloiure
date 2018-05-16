package graalvm.compiler.replacements.nodes.arithmetic;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.SubNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.util.GraphUtil;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * Node representing an exact integer substraction that will throw an {@link ArithmeticException} in
 * case the addition would overflow the 32 bit range.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_2)
public final class IntegerSubExactNode extends SubNode implements IntegerExactArithmeticNode
{
    public static final NodeClass<IntegerSubExactNode> TYPE = NodeClass.create(IntegerSubExactNode.class);

    public IntegerSubExactNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y);
        setStamp(x.stamp(NodeView.DEFAULT).unrestricted());
        assert x.stamp(NodeView.DEFAULT).isCompatible(y.stamp(NodeView.DEFAULT)) && x.stamp(NodeView.DEFAULT) instanceof IntegerStamp;
    }

    @Override
    public boolean inferStamp()
    {
        /*
         * Note: it is not allowed to use the foldStamp method of the regular sub node as we do not
         * know the result stamp of this node if we do not know whether we may deopt. If we know we
         * can never overflow we will replace this node with its non overflow checking counterpart
         * anyway.
         */
        return false;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        if (GraphUtil.unproxify(forX) == GraphUtil.unproxify(forY))
        {
            return ConstantNode.forIntegerStamp(stamp(NodeView.DEFAULT), 0);
        }
        if (forX.isConstant() && forY.isConstant())
        {
            return canonicalXYconstant(forX, forY);
        }
        else if (forY.isConstant())
        {
            long c = forY.asJavaConstant().asLong();
            if (c == 0)
            {
                return forX;
            }
        }
        if (!IntegerStamp.subtractionCanOverflow((IntegerStamp) x.stamp(NodeView.DEFAULT), (IntegerStamp) y.stamp(NodeView.DEFAULT)))
        {
            return new SubNode(x, y).canonical(tool);
        }
        return this;
    }

    private ValueNode canonicalXYconstant(ValueNode forX, ValueNode forY)
    {
        JavaConstant xConst = forX.asJavaConstant();
        JavaConstant yConst = forY.asJavaConstant();
        assert xConst.getJavaKind() == yConst.getJavaKind();
        try
        {
            if (xConst.getJavaKind() == JavaKind.Int)
            {
                return ConstantNode.forInt(Math.subtractExact(xConst.asInt(), yConst.asInt()));
            }
            else
            {
                assert xConst.getJavaKind() == JavaKind.Long;
                return ConstantNode.forLong(Math.subtractExact(xConst.asLong(), yConst.asLong()));
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
        return graph().add(new IntegerSubExactSplitNode(stamp(NodeView.DEFAULT), getX(), getY(), next, deopt));
    }

    @Override
    public void lower(LoweringTool tool)
    {
        IntegerExactArithmeticSplitNode.lower(tool, this);
    }
}
