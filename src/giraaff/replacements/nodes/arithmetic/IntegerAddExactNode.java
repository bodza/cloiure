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
    public static final NodeClass<IntegerAddExactNode> TYPE = NodeClass.create(IntegerAddExactNode.class);

    // @cons
    public IntegerAddExactNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y);
        setStamp(x.stamp(NodeView.DEFAULT).unrestricted());
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
    public Stamp foldStamp(Stamp stampX, Stamp stampY)
    {
        IntegerStamp a = (IntegerStamp) stampX;
        IntegerStamp b = (IntegerStamp) stampY;

        int bits = a.getBits();

        long defaultMask = CodeUtil.mask(bits);
        long variableBits = (a.downMask() ^ a.upMask()) | (b.downMask() ^ b.upMask());
        long variableBitsWithCarry = variableBits | (IntegerStamp.carryBits(a.downMask(), b.downMask()) ^ IntegerStamp.carryBits(a.upMask(), b.upMask()));
        long newDownMask = (a.downMask() + b.downMask()) & ~variableBitsWithCarry;
        long newUpMask = (a.downMask() + b.downMask()) | variableBitsWithCarry;

        newDownMask &= defaultMask;
        newUpMask &= defaultMask;

        long newLowerBound;
        long newUpperBound;
        boolean lowerOverflowsPositively = IntegerStamp.addOverflowsPositively(a.lowerBound(), b.lowerBound(), bits);
        boolean upperOverflowsPositively = IntegerStamp.addOverflowsPositively(a.upperBound(), b.upperBound(), bits);
        boolean lowerOverflowsNegatively = IntegerStamp.addOverflowsNegatively(a.lowerBound(), b.lowerBound(), bits);
        boolean upperOverflowsNegatively = IntegerStamp.addOverflowsNegatively(a.upperBound(), b.upperBound(), bits);
        if (lowerOverflowsPositively)
        {
            newLowerBound = CodeUtil.maxValue(bits);
        }
        else if (lowerOverflowsNegatively)
        {
            newLowerBound = CodeUtil.minValue(bits);
        }
        else
        {
            newLowerBound = CodeUtil.signExtend((a.lowerBound() + b.lowerBound()) & defaultMask, bits);
        }

        if (upperOverflowsPositively)
        {
            newUpperBound = CodeUtil.maxValue(bits);
        }
        else if (upperOverflowsNegatively)
        {
            newUpperBound = CodeUtil.minValue(bits);
        }
        else
        {
            newUpperBound = CodeUtil.signExtend((a.upperBound() + b.upperBound()) & defaultMask, bits);
        }

        IntegerStamp limit = StampFactory.forInteger(bits, newLowerBound, newUpperBound);
        newUpMask &= limit.upMask();
        newUpperBound = CodeUtil.signExtend(newUpperBound & newUpMask, bits);
        newDownMask |= limit.downMask();
        newLowerBound |= newDownMask;
        return IntegerStamp.create(bits, newLowerBound, newUpperBound, newDownMask, newUpMask);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        if (forX.isConstant() && !forY.isConstant())
        {
            return new IntegerAddExactNode(forY, forX).canonical(tool);
        }
        if (forX.isConstant())
        {
            ConstantNode constantNode = canonicalXconstant(forX, forY);
            if (constantNode != null)
            {
                return constantNode;
            }
        }
        else if (forY.isConstant())
        {
            long c = forY.asJavaConstant().asLong();
            if (c == 0)
            {
                return forX;
            }
        }
        if (!IntegerStamp.addCanOverflow((IntegerStamp) forX.stamp(NodeView.DEFAULT), (IntegerStamp) forY.stamp(NodeView.DEFAULT)))
        {
            return new AddNode(forX, forY).canonical(tool);
        }
        return this;
    }

    private static ConstantNode canonicalXconstant(ValueNode forX, ValueNode forY)
    {
        JavaConstant xConst = forX.asJavaConstant();
        JavaConstant yConst = forY.asJavaConstant();
        if (xConst != null && yConst != null)
        {
            try
            {
                if (xConst.getJavaKind() == JavaKind.Int)
                {
                    return ConstantNode.forInt(Math.addExact(xConst.asInt(), yConst.asInt()));
                }
                else
                {
                    return ConstantNode.forLong(Math.addExact(xConst.asLong(), yConst.asLong()));
                }
            }
            catch (ArithmeticException ex)
            {
                // The operation will result in an overflow exception, so do not canonicalize.
            }
        }
        return null;
    }

    @Override
    public IntegerExactArithmeticSplitNode createSplit(AbstractBeginNode next, AbstractBeginNode deopt)
    {
        return graph().add(new IntegerAddExactSplitNode(stamp(NodeView.DEFAULT), getX(), getY(), next, deopt));
    }

    @Override
    public void lower(LoweringTool tool)
    {
        IntegerExactArithmeticSplitNode.lower(tool, this);
    }
}
