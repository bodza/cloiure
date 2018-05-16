package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.code.CodeUtil;

@NodeInfo(shortName = "%")
public class SignedRemNode extends IntegerDivRemNode implements LIRLowerable
{
    public static final NodeClass<SignedRemNode> TYPE = NodeClass.create(SignedRemNode.class);

    protected SignedRemNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

    protected SignedRemNode(NodeClass<? extends SignedRemNode> c, ValueNode x, ValueNode y)
    {
        super(c, IntegerStamp.OPS.getRem().foldStamp(x.stamp(NodeView.DEFAULT), y.stamp(NodeView.DEFAULT)), Op.REM, Type.SIGNED, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        Stamp stamp = IntegerStamp.OPS.getRem().foldStamp(x.stamp(view), y.stamp(view));
        return canonical(null, x, y, stamp, view);
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(IntegerStamp.OPS.getRem().foldStamp(getX().stamp(NodeView.DEFAULT), getY().stamp(NodeView.DEFAULT)));
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        return canonical(this, forX, forY, stamp(view), view);
    }

    private static ValueNode canonical(SignedRemNode self, ValueNode forX, ValueNode forY, Stamp stamp, NodeView view)
    {
        if (forX.isConstant() && forY.isConstant())
        {
            long y = forY.asJavaConstant().asLong();
            if (y == 0)
            {
                return self != null ? self : new SignedRemNode(forX, forY); // this will trap, can
                                                                            // not canonicalize
            }
            return ConstantNode.forIntegerStamp(stamp, forX.asJavaConstant().asLong() % y);
        }
        else if (forY.isConstant() && forX.stamp(view) instanceof IntegerStamp && forY.stamp(view) instanceof IntegerStamp)
        {
            long constY = forY.asJavaConstant().asLong();
            IntegerStamp xStamp = (IntegerStamp) forX.stamp(view);
            IntegerStamp yStamp = (IntegerStamp) forY.stamp(view);
            if (constY < 0 && constY != CodeUtil.minValue(yStamp.getBits()))
            {
                Stamp newStamp = IntegerStamp.OPS.getRem().foldStamp(forX.stamp(view), forY.stamp(view));
                return canonical(null, forX, ConstantNode.forIntegerStamp(yStamp, -constY), newStamp, view);
            }

            if (constY == 1)
            {
                return ConstantNode.forIntegerStamp(stamp, 0);
            }
            else if (CodeUtil.isPowerOf2(constY))
            {
                if (xStamp.isPositive())
                {
                    // x & (y - 1)
                    return new AndNode(forX, ConstantNode.forIntegerStamp(stamp, constY - 1));
                }
                else if (xStamp.isNegative())
                {
                    // -((-x) & (y - 1))
                    return new NegateNode(new AndNode(new NegateNode(forX), ConstantNode.forIntegerStamp(stamp, constY - 1)));
                }
                else
                {
                    // x - ((x / y) << log2(y))
                    return SubNode.create(forX, LeftShiftNode.create(SignedDivNode.canonical(forX, constY, view), ConstantNode.forInt(CodeUtil.log2(constY)), view), view);
                }
            }
        }
        return self != null ? self : new SignedRemNode(forX, forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.setResult(this, gen.getLIRGeneratorTool().getArithmetic().emitRem(gen.operand(getX()), gen.operand(getY()), gen.state(this)));
    }
}
