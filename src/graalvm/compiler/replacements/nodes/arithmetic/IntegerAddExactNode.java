package graalvm.compiler.replacements.nodes.arithmetic;

import static graalvm.compiler.core.common.type.IntegerStamp.addOverflowsNegatively;
import static graalvm.compiler.core.common.type.IntegerStamp.addOverflowsPositively;
import static graalvm.compiler.core.common.type.IntegerStamp.carryBits;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.AddNode;
import graalvm.compiler.nodes.spi.LoweringTool;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

/**
 * Node representing an exact integer addition that will throw an {@link ArithmeticException} in
 * case the addition would overflow the 32 bit range.
 */
@NodeInfo(cycles = CYCLES_2, size = SIZE_2)
public final class IntegerAddExactNode extends AddNode implements IntegerExactArithmeticNode
{
    public static final NodeClass<IntegerAddExactNode> TYPE = NodeClass.create(IntegerAddExactNode.class);

    public IntegerAddExactNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y);
        setStamp(x.stamp(NodeView.DEFAULT).unrestricted());
        assert x.stamp(NodeView.DEFAULT).isCompatible(y.stamp(NodeView.DEFAULT)) && x.stamp(NodeView.DEFAULT) instanceof IntegerStamp;
    }

    @Override
    public boolean inferStamp()
    {
        /*
         * Note: it is not allowed to use the foldStamp method of the regular add node as we do not
         * know the result stamp of this node if we do not know whether we may deopt. If we know we
         * can never overflow we will replace this node with its non overflow checking counterpart
         * anyway.
         */
        return false;
    }

    @Override
    public Stamp foldStamp(Stamp stampX, Stamp stampY)
    {
        IntegerStamp a = (IntegerStamp) stampX;
        IntegerStamp b = (IntegerStamp) stampY;

        int bits = a.getBits();
        assert bits == b.getBits();

        long defaultMask = CodeUtil.mask(bits);
        long variableBits = (a.downMask() ^ a.upMask()) | (b.downMask() ^ b.upMask());
        long variableBitsWithCarry = variableBits | (carryBits(a.downMask(), b.downMask()) ^ carryBits(a.upMask(), b.upMask()));
        long newDownMask = (a.downMask() + b.downMask()) & ~variableBitsWithCarry;
        long newUpMask = (a.downMask() + b.downMask()) | variableBitsWithCarry;

        newDownMask &= defaultMask;
        newUpMask &= defaultMask;

        long newLowerBound;
        long newUpperBound;
        boolean lowerOverflowsPositively = addOverflowsPositively(a.lowerBound(), b.lowerBound(), bits);
        boolean upperOverflowsPositively = addOverflowsPositively(a.upperBound(), b.upperBound(), bits);
        boolean lowerOverflowsNegatively = addOverflowsNegatively(a.lowerBound(), b.lowerBound(), bits);
        boolean upperOverflowsNegatively = addOverflowsNegatively(a.upperBound(), b.upperBound(), bits);
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
            assert xConst.getJavaKind() == yConst.getJavaKind();
            try
            {
                if (xConst.getJavaKind() == JavaKind.Int)
                {
                    return ConstantNode.forInt(Math.addExact(xConst.asInt(), yConst.asInt()));
                }
                else
                {
                    assert xConst.getJavaKind() == JavaKind.Long;
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
