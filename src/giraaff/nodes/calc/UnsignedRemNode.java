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

public class UnsignedRemNode extends IntegerDivRemNode implements LIRLowerable
{
    public static final NodeClass<UnsignedRemNode> TYPE = NodeClass.create(UnsignedRemNode.class);

    public UnsignedRemNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

    protected UnsignedRemNode(NodeClass<? extends UnsignedRemNode> c, ValueNode x, ValueNode y)
    {
        super(c, x.stamp(NodeView.DEFAULT).unrestricted(), Op.REM, Type.UNSIGNED, x, y);
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
    public static ValueNode canonical(UnsignedRemNode self, ValueNode forX, ValueNode forY, Stamp stamp, NodeView view)
    {
        int bits = ((IntegerStamp) stamp).getBits();
        if (forX.isConstant() && forY.isConstant())
        {
            long yConst = CodeUtil.zeroExtend(forY.asJavaConstant().asLong(), bits);
            if (yConst == 0)
            {
                return self != null ? self : new UnsignedRemNode(forX, forY); // this will trap,
                                                                              // cannot canonicalize
            }
            return ConstantNode.forIntegerStamp(stamp, Long.remainderUnsigned(CodeUtil.zeroExtend(forX.asJavaConstant().asLong(), bits), yConst));
        }
        else if (forY.isConstant())
        {
            long c = CodeUtil.zeroExtend(forY.asJavaConstant().asLong(), bits);
            if (c == 1)
            {
                return ConstantNode.forIntegerStamp(stamp, 0);
            }
            else if (CodeUtil.isPowerOf2(c))
            {
                return new AndNode(forX, ConstantNode.forIntegerStamp(stamp, c - 1));
            }
        }
        return self != null ? self : new UnsignedRemNode(forX, forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.setResult(this, gen.getLIRGeneratorTool().getArithmetic().emitURem(gen.operand(getX()), gen.operand(getY()), gen.state(this)));
    }
}
