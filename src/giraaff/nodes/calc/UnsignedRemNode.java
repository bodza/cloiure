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

// @class UnsignedRemNode
public final class UnsignedRemNode extends IntegerDivRemNode implements LIRLowerable
{
    // @def
    public static final NodeClass<UnsignedRemNode> TYPE = NodeClass.create(UnsignedRemNode.class);

    // @cons
    public UnsignedRemNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons
    protected UnsignedRemNode(NodeClass<? extends UnsignedRemNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, __x.stamp(NodeView.DEFAULT).unrestricted(), Op.REM, Type.UNSIGNED, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        Stamp __stamp = __x.stamp(__view).unrestricted();
        return canonical(null, __x, __y, __stamp, __view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        return canonical(this, __forX, __forY, stamp(__view), __view);
    }

    @SuppressWarnings("unused")
    public static ValueNode canonical(UnsignedRemNode __self, ValueNode __forX, ValueNode __forY, Stamp __stamp, NodeView __view)
    {
        int __bits = ((IntegerStamp) __stamp).getBits();
        if (__forX.isConstant() && __forY.isConstant())
        {
            long __yConst = CodeUtil.zeroExtend(__forY.asJavaConstant().asLong(), __bits);
            if (__yConst == 0)
            {
                return __self != null ? __self : new UnsignedRemNode(__forX, __forY); // this will trap,
                                                                              // cannot canonicalize
            }
            return ConstantNode.forIntegerStamp(__stamp, Long.remainderUnsigned(CodeUtil.zeroExtend(__forX.asJavaConstant().asLong(), __bits), __yConst));
        }
        else if (__forY.isConstant())
        {
            long __c = CodeUtil.zeroExtend(__forY.asJavaConstant().asLong(), __bits);
            if (__c == 1)
            {
                return ConstantNode.forIntegerStamp(__stamp, 0);
            }
            else if (CodeUtil.isPowerOf2(__c))
            {
                return new AndNode(__forX, ConstantNode.forIntegerStamp(__stamp, __c - 1));
            }
        }
        return __self != null ? __self : new UnsignedRemNode(__forX, __forY);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, __gen.getLIRGeneratorTool().getArithmetic().emitURem(__gen.operand(getX()), __gen.operand(getY()), __gen.state(this)));
    }
}
