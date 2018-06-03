package giraaff.nodes.calc;

import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.BinaryOpLogicNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

///
// This node will perform a "test" operation on its arguments. Its result is equivalent to the
// expression "(x &amp; y) == 0", meaning that it will return true if (and only if) no bit is set in
// both x and y.
///
// @class IntegerTestNode
public final class IntegerTestNode extends BinaryOpLogicNode implements BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<IntegerTestNode> TYPE = NodeClass.create(IntegerTestNode.class);

    // @cons
    public IntegerTestNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, __x, __y);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        if (__forX.isConstant() && __forY.isConstant())
        {
            return LogicConstantNode.forBoolean((__forX.asJavaConstant().asLong() & __forY.asJavaConstant().asLong()) == 0);
        }
        if (__forX.stamp(__view) instanceof IntegerStamp && __forY.stamp(__view) instanceof IntegerStamp)
        {
            IntegerStamp __xStamp = (IntegerStamp) __forX.stamp(__view);
            IntegerStamp __yStamp = (IntegerStamp) __forY.stamp(__view);
            if ((__xStamp.upMask() & __yStamp.upMask()) == 0)
            {
                return LogicConstantNode.tautology();
            }
            else if ((__xStamp.downMask() & __yStamp.downMask()) != 0)
            {
                return LogicConstantNode.contradiction();
            }
        }
        return this;
    }

    @Override
    public Stamp getSucceedingStampForX(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        return getSucceedingStamp(__negated, __xStamp, __yStamp);
    }

    private static Stamp getSucceedingStamp(boolean __negated, Stamp __xStampGeneric, Stamp __otherStampGeneric)
    {
        if (__xStampGeneric instanceof IntegerStamp && __otherStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp __xStamp = (IntegerStamp) __xStampGeneric;
            IntegerStamp __otherStamp = (IntegerStamp) __otherStampGeneric;
            if (__negated)
            {
                if (Long.bitCount(__otherStamp.upMask()) == 1)
                {
                    long __newDownMask = __xStamp.downMask() | __otherStamp.upMask();
                    if (__xStamp.downMask() != __newDownMask)
                    {
                        return IntegerStamp.stampForMask(__xStamp.getBits(), __newDownMask, __xStamp.upMask()).join(__xStamp);
                    }
                }
            }
            else
            {
                long __restrictedUpMask = ((~__otherStamp.downMask()) & __xStamp.upMask());
                if (__xStamp.upMask() != __restrictedUpMask)
                {
                    return IntegerStamp.stampForMask(__xStamp.getBits(), __xStamp.downMask(), __restrictedUpMask).join(__xStamp);
                }
            }
        }
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        return getSucceedingStamp(__negated, __yStamp, __xStamp);
    }

    @Override
    public TriState tryFold(Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        if (__xStampGeneric instanceof IntegerStamp && __yStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp __xStamp = (IntegerStamp) __xStampGeneric;
            IntegerStamp __yStamp = (IntegerStamp) __yStampGeneric;
            if ((__xStamp.upMask() & __yStamp.upMask()) == 0)
            {
                return TriState.TRUE;
            }
            else if ((__xStamp.downMask() & __yStamp.downMask()) != 0)
            {
                return TriState.FALSE;
            }
        }
        return TriState.UNKNOWN;
    }
}
