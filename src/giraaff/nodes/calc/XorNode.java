package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

// @class XorNode
public final class XorNode extends BinaryArithmeticNode<ArithmeticOpTable.BinaryOp.Xor> implements Canonicalizable.BinaryCommutative<ValueNode>, NarrowableArithmeticNode
{
    // @def
    public static final NodeClass<XorNode> TYPE = NodeClass.create(XorNode.class);

    // @cons XorNode
    public XorNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, ArithmeticOpTable::getXor, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Xor> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getXor();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), __y.stamp(__view));
        ConstantNode __tryConstantFold = tryConstantFold(__op, __x, __y, __stamp, __view);
        if (__tryConstantFold != null)
        {
            return __tryConstantFold;
        }
        return canonical(null, __op, __stamp, __x, __y, __view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        NodeView __view = NodeView.from(__tool);
        return canonical(this, getOp(__forX, __forY), stamp(NodeView.DEFAULT), __forX, __forY, __view);
    }

    private static ValueNode canonical(XorNode __self, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Xor> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY))
        {
            return ConstantNode.forPrimitive(__stamp, __op.getZero(__forX.stamp(__view)));
        }
        if (__forX.isConstant() && !__forY.isConstant())
        {
            return new XorNode(__forY, __forX);
        }
        if (__forY.isConstant())
        {
            Constant __c = __forY.asConstant();
            if (__op.isNeutral(__c))
            {
                return __forX;
            }

            if (__c instanceof PrimitiveConstant && ((PrimitiveConstant) __c).getJavaKind().isNumericInteger())
            {
                long __rawY = ((PrimitiveConstant) __c).asLong();
                long __mask = CodeUtil.mask(PrimitiveStamp.getBits(__stamp));
                if ((__rawY & __mask) == __mask)
                {
                    return new NotNode(__forX);
                }
            }
            return reassociate(__self != null ? __self : (XorNode) new XorNode(__forX, __forY).maybeCommuteInputs(), ValueNode.isConstantPredicate(), __forX, __forY, __view);
        }
        return __self != null ? __self : new XorNode(__forX, __forY).maybeCommuteInputs();
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitXor(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY())));
    }
}
