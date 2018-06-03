package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Sub;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

// @class SubNode
public class SubNode extends BinaryArithmeticNode<Sub> implements NarrowableArithmeticNode
{
    // @def
    public static final NodeClass<SubNode> TYPE = NodeClass.create(SubNode.class);

    // @cons
    public SubNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons
    protected SubNode(NodeClass<? extends SubNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, ArithmeticOpTable::getSub, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        BinaryOp<Sub> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getSub();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), __y.stamp(__view));
        ConstantNode __tryConstantFold = tryConstantFold(__op, __x, __y, __stamp, __view);
        if (__tryConstantFold != null)
        {
            return __tryConstantFold;
        }
        return canonical(null, __op, __stamp, __x, __y, __view);
    }

    private static ValueNode canonical(SubNode __subNode, BinaryOp<Sub> __op, Stamp __stamp, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        SubNode __self = __subNode;
        if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY))
        {
            Constant __zero = __op.getZero(__forX.stamp(__view));
            if (__zero != null)
            {
                return ConstantNode.forPrimitive(__stamp, __zero);
            }
        }
        boolean __associative = __op.isAssociative();
        if (__associative)
        {
            if (__forX instanceof AddNode)
            {
                AddNode __x = (AddNode) __forX;
                if (__x.getY() == __forY)
                {
                    // (a + b) - b
                    return __x.getX();
                }
                if (__x.getX() == __forY)
                {
                    // (a + b) - a
                    return __x.getY();
                }
            }
            else if (__forX instanceof SubNode)
            {
                SubNode __x = (SubNode) __forX;
                if (__x.getX() == __forY)
                {
                    // (a - b) - a
                    return NegateNode.create(__x.getY(), __view);
                }
            }
            if (__forY instanceof AddNode)
            {
                AddNode __y = (AddNode) __forY;
                if (__y.getX() == __forX)
                {
                    // a - (a + b)
                    return NegateNode.create(__y.getY(), __view);
                }
                if (__y.getY() == __forX)
                {
                    // b - (a + b)
                    return NegateNode.create(__y.getX(), __view);
                }
            }
            else if (__forY instanceof SubNode)
            {
                SubNode __y = (SubNode) __forY;
                if (__y.getX() == __forX)
                {
                    // a - (a - b)
                    return __y.getY();
                }
            }
        }
        if (__forY.isConstant())
        {
            Constant __c = __forY.asConstant();
            if (__op.isNeutral(__c))
            {
                return __forX;
            }
            if (__associative && __self != null)
            {
                ValueNode __reassociated = reassociate(__self, ValueNode.isConstantPredicate(), __forX, __forY, __view);
                if (__reassociated != __self)
                {
                    return __reassociated;
                }
            }
            if (__c instanceof PrimitiveConstant && ((PrimitiveConstant) __c).getJavaKind().isNumericInteger())
            {
                long __i = ((PrimitiveConstant) __c).asLong();
                if (__i < 0 || ((IntegerStamp) StampFactory.forKind(__forY.getStackKind())).contains(-__i))
                {
                    // Adding a negative is more friendly to the backend since adds are
                    // commutative, so prefer add when it fits.
                    return BinaryArithmeticNode.add(__forX, ConstantNode.forIntegerStamp(__stamp, -__i), __view);
                }
            }
        }
        else if (__forX.isConstant())
        {
            Constant __c = __forX.asConstant();
            if (ArithmeticOpTable.forStamp(__stamp).getAdd().isNeutral(__c))
            {
                /*
                 * Note that for floating point numbers, + and - have different neutral elements.
                 * We have to test for the neutral element of +, because we are doing this
                 * transformation: 0 - x == (-x) + 0 == -x.
                 */
                return NegateNode.create(__forY, __view);
            }
            if (__associative && __self != null)
            {
                return reassociate(__self, ValueNode.isConstantPredicate(), __forX, __forY, __view);
            }
        }
        if (__forY instanceof NegateNode)
        {
            return BinaryArithmeticNode.add(__forX, ((NegateNode) __forY).getValue(), __view);
        }
        return __self != null ? __self : new SubNode(__forX, __forY);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        BinaryOp<Sub> __op = getOp(__forX, __forY);
        return canonical(this, __op, stamp, __forX, __forY, __view);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        __nodeValueMap.setResult(this, __gen.emitSub(__nodeValueMap.operand(getX()), __nodeValueMap.operand(getY()), false));
    }
}
