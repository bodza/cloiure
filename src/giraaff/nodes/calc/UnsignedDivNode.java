package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public class UnsignedDivNode extends IntegerDivRemNode implements LIRLowerable
{
    public static final NodeClass<UnsignedDivNode> TYPE = NodeClass.create(UnsignedDivNode.class);

    public UnsignedDivNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

    protected UnsignedDivNode(NodeClass<? extends UnsignedDivNode> c, ValueNode x, ValueNode y)
    {
        super(c, x.stamp(NodeView.DEFAULT).unrestricted(), Op.DIV, Type.UNSIGNED, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        Stamp stamp = x.stamp(view).unrestricted();
        return canonical(null, x, y, stamp, view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        return canonical(this, forX, forY, stamp(view), view);
    }

    @SuppressWarnings("unused")
    private static ValueNode canonical(UnsignedDivNode self, ValueNode forX, ValueNode forY, Stamp stamp, NodeView view)
    {
        int bits = ((IntegerStamp) stamp).getBits();
        if (forX.isConstant() && forY.isConstant())
        {
            long yConst = CodeUtil.zeroExtend(forY.asJavaConstant().asLong(), bits);
            if (yConst == 0)
            {
                return self != null ? self : new UnsignedDivNode(forX, forY); // this will trap,
                                                                              // cannot canonicalize
            }
            return ConstantNode.forIntegerStamp(stamp, Long.divideUnsigned(CodeUtil.zeroExtend(forX.asJavaConstant().asLong(), bits), yConst));
        }
        else if (forY.isConstant())
        {
            long c = CodeUtil.zeroExtend(forY.asJavaConstant().asLong(), bits);
            if (c == 1)
            {
                return forX;
            }
            if (CodeUtil.isPowerOf2(c))
            {
                return new UnsignedRightShiftNode(forX, ConstantNode.forInt(CodeUtil.log2(c)));
            }
        }
        return self != null ? self : new UnsignedDivNode(forX, forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.setResult(this, gen.getLIRGeneratorTool().getArithmetic().emitUDiv(gen.operand(getX()), gen.operand(getY()), gen.state(this)));
    }
}