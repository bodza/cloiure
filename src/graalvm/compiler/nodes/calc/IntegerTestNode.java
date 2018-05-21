package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable.BinaryCommutative;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodes.BinaryOpLogicNode;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;

import jdk.vm.ci.meta.TriState;

/**
 * This node will perform a "test" operation on its arguments. Its result is equivalent to the
 * expression "(x &amp; y) == 0", meaning that it will return true if (and only if) no bit is set in
 * both x and y.
 */
public final class IntegerTestNode extends BinaryOpLogicNode implements BinaryCommutative<ValueNode>
{
    public static final NodeClass<IntegerTestNode> TYPE = NodeClass.create(IntegerTestNode.class);

    public IntegerTestNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        if (forX.isConstant() && forY.isConstant())
        {
            return LogicConstantNode.forBoolean((forX.asJavaConstant().asLong() & forY.asJavaConstant().asLong()) == 0);
        }
        if (forX.stamp(view) instanceof IntegerStamp && forY.stamp(view) instanceof IntegerStamp)
        {
            IntegerStamp xStamp = (IntegerStamp) forX.stamp(view);
            IntegerStamp yStamp = (IntegerStamp) forY.stamp(view);
            if ((xStamp.upMask() & yStamp.upMask()) == 0)
            {
                return LogicConstantNode.tautology();
            }
            else if ((xStamp.downMask() & yStamp.downMask()) != 0)
            {
                return LogicConstantNode.contradiction();
            }
        }
        return this;
    }

    @Override
    public Stamp getSucceedingStampForX(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        return getSucceedingStamp(negated, xStamp, yStamp);
    }

    private static Stamp getSucceedingStamp(boolean negated, Stamp xStampGeneric, Stamp otherStampGeneric)
    {
        if (xStampGeneric instanceof IntegerStamp && otherStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp xStamp = (IntegerStamp) xStampGeneric;
            IntegerStamp otherStamp = (IntegerStamp) otherStampGeneric;
            if (negated)
            {
                if (Long.bitCount(otherStamp.upMask()) == 1)
                {
                    long newDownMask = xStamp.downMask() | otherStamp.upMask();
                    if (xStamp.downMask() != newDownMask)
                    {
                        return IntegerStamp.stampForMask(xStamp.getBits(), newDownMask, xStamp.upMask()).join(xStamp);
                    }
                }
            }
            else
            {
                long restrictedUpMask = ((~otherStamp.downMask()) & xStamp.upMask());
                if (xStamp.upMask() != restrictedUpMask)
                {
                    return IntegerStamp.stampForMask(xStamp.getBits(), xStamp.downMask(), restrictedUpMask).join(xStamp);
                }
            }
        }
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        return getSucceedingStamp(negated, yStamp, xStamp);
    }

    @Override
    public TriState tryFold(Stamp xStampGeneric, Stamp yStampGeneric)
    {
        if (xStampGeneric instanceof IntegerStamp && yStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp xStamp = (IntegerStamp) xStampGeneric;
            IntegerStamp yStamp = (IntegerStamp) yStampGeneric;
            if ((xStamp.upMask() & yStamp.upMask()) == 0)
            {
                return TriState.TRUE;
            }
            else if ((xStamp.downMask() & yStamp.downMask()) != 0)
            {
                return TriState.FALSE;
            }
        }
        return TriState.UNKNOWN;
    }
}
