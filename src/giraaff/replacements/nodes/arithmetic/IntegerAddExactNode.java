package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.spi.LoweringTool;

/**
 * Node representing an exact integer addition that will throw an {@link ArithmeticException} in
 * case the addition would overflow the 32 bit range.
 */
// @class IntegerAddExactNode
public final class IntegerAddExactNode extends AddNode implements IntegerExactArithmeticNode
{
    // @def
    public static final NodeClass<IntegerAddExactNode> TYPE = NodeClass.create(IntegerAddExactNode.class);

    // @cons
    public IntegerAddExactNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, __x, __y);
        setStamp(__x.stamp(NodeView.DEFAULT).unrestricted());
    }

    @Override
    public boolean inferStamp()
    {
        /*
         * Note: it is not allowed to use the foldStamp method of the regular add node as we do not know
         * the result stamp of this node if we do not know whether we may deopt. If we know we can never
         * overflow we will replace this node with its non overflow checking counterpart anyway.
         */
        return false;
    }

    @Override
    public Stamp foldStamp(Stamp __stampX, Stamp __stampY)
    {
        IntegerStamp __a = (IntegerStamp) __stampX;
        IntegerStamp __b = (IntegerStamp) __stampY;

        int __bits = __a.getBits();

        long __defaultMask = CodeUtil.mask(__bits);
        long __variableBits = (__a.downMask() ^ __a.upMask()) | (__b.downMask() ^ __b.upMask());
        long __variableBitsWithCarry = __variableBits | (IntegerStamp.carryBits(__a.downMask(), __b.downMask()) ^ IntegerStamp.carryBits(__a.upMask(), __b.upMask()));
        long __newDownMask = (__a.downMask() + __b.downMask()) & ~__variableBitsWithCarry;
        long __newUpMask = (__a.downMask() + __b.downMask()) | __variableBitsWithCarry;

        __newDownMask &= __defaultMask;
        __newUpMask &= __defaultMask;

        long __newLowerBound;
        long __newUpperBound;
        boolean __lowerOverflowsPositively = IntegerStamp.addOverflowsPositively(__a.lowerBound(), __b.lowerBound(), __bits);
        boolean __upperOverflowsPositively = IntegerStamp.addOverflowsPositively(__a.upperBound(), __b.upperBound(), __bits);
        boolean __lowerOverflowsNegatively = IntegerStamp.addOverflowsNegatively(__a.lowerBound(), __b.lowerBound(), __bits);
        boolean __upperOverflowsNegatively = IntegerStamp.addOverflowsNegatively(__a.upperBound(), __b.upperBound(), __bits);
        if (__lowerOverflowsPositively)
        {
            __newLowerBound = CodeUtil.maxValue(__bits);
        }
        else if (__lowerOverflowsNegatively)
        {
            __newLowerBound = CodeUtil.minValue(__bits);
        }
        else
        {
            __newLowerBound = CodeUtil.signExtend((__a.lowerBound() + __b.lowerBound()) & __defaultMask, __bits);
        }

        if (__upperOverflowsPositively)
        {
            __newUpperBound = CodeUtil.maxValue(__bits);
        }
        else if (__upperOverflowsNegatively)
        {
            __newUpperBound = CodeUtil.minValue(__bits);
        }
        else
        {
            __newUpperBound = CodeUtil.signExtend((__a.upperBound() + __b.upperBound()) & __defaultMask, __bits);
        }

        IntegerStamp __limit = StampFactory.forInteger(__bits, __newLowerBound, __newUpperBound);
        __newUpMask &= __limit.upMask();
        __newUpperBound = CodeUtil.signExtend(__newUpperBound & __newUpMask, __bits);
        __newDownMask |= __limit.downMask();
        __newLowerBound |= __newDownMask;
        return IntegerStamp.create(__bits, __newLowerBound, __newUpperBound, __newDownMask, __newUpMask);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        if (__forX.isConstant() && !__forY.isConstant())
        {
            return new IntegerAddExactNode(__forY, __forX).canonical(__tool);
        }
        if (__forX.isConstant())
        {
            ConstantNode __constantNode = canonicalXconstant(__forX, __forY);
            if (__constantNode != null)
            {
                return __constantNode;
            }
        }
        else if (__forY.isConstant())
        {
            long __c = __forY.asJavaConstant().asLong();
            if (__c == 0)
            {
                return __forX;
            }
        }
        if (!IntegerStamp.addCanOverflow((IntegerStamp) __forX.stamp(NodeView.DEFAULT), (IntegerStamp) __forY.stamp(NodeView.DEFAULT)))
        {
            return new AddNode(__forX, __forY).canonical(__tool);
        }
        return this;
    }

    private static ConstantNode canonicalXconstant(ValueNode __forX, ValueNode __forY)
    {
        JavaConstant __xConst = __forX.asJavaConstant();
        JavaConstant __yConst = __forY.asJavaConstant();
        if (__xConst != null && __yConst != null)
        {
            try
            {
                if (__xConst.getJavaKind() == JavaKind.Int)
                {
                    return ConstantNode.forInt(Math.addExact(__xConst.asInt(), __yConst.asInt()));
                }
                else
                {
                    return ConstantNode.forLong(Math.addExact(__xConst.asLong(), __yConst.asLong()));
                }
            }
            catch (ArithmeticException __ex)
            {
                // The operation will result in an overflow exception, so do not canonicalize.
            }
        }
        return null;
    }

    @Override
    public IntegerExactArithmeticSplitNode createSplit(AbstractBeginNode __next, AbstractBeginNode __deopt)
    {
        return graph().add(new IntegerAddExactSplitNode(stamp(NodeView.DEFAULT), getX(), getY(), __next, __deopt));
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        IntegerExactArithmeticSplitNode.lower(__tool, this);
    }
}
