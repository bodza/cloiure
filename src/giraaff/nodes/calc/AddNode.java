package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class AddNode
public class AddNode extends BinaryArithmeticNode<ArithmeticOpTable.BinaryOp.Add> implements NarrowableArithmeticNode, Canonicalizable.BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<AddNode> TYPE = NodeClass.create(AddNode.class);

    // @cons AddNode
    public AddNode(ValueNode __x, ValueNode __y)
    {
        this(TYPE, __x, __y);
    }

    // @cons AddNode
    protected AddNode(NodeClass<? extends AddNode> __c, ValueNode __x, ValueNode __y)
    {
        super(__c, ArithmeticOpTable::getAdd, __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add> __op = ArithmeticOpTable.forStamp(__x.stamp(__view)).getAdd();
        Stamp __stamp = __op.foldStamp(__x.stamp(__view), __y.stamp(__view));
        ConstantNode __tryConstantFold = tryConstantFold(__op, __x, __y, __stamp, __view);
        if (__tryConstantFold != null)
        {
            return __tryConstantFold;
        }
        if (__x.isConstant() && !__y.isConstant())
        {
            return canonical(null, __op, __y, __x, __view);
        }
        else
        {
            return canonical(null, __op, __x, __y, __view);
        }
    }

    private static ValueNode canonical(AddNode __addNode, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add> __op, ValueNode __forX, ValueNode __forY, NodeView __view)
    {
        AddNode __self = __addNode;
        boolean __associative = __op.isAssociative();
        if (__associative)
        {
            if (__forX instanceof SubNode)
            {
                SubNode __sub = (SubNode) __forX;
                if (__sub.getY() == __forY)
                {
                    // (a - b) + b
                    return __sub.getX();
                }
            }
            if (__forY instanceof SubNode)
            {
                SubNode __sub = (SubNode) __forY;
                if (__sub.getY() == __forX)
                {
                    // b + (a - b)
                    return __sub.getX();
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
                // canonicalize expressions like "(a + 1) + 2"
                ValueNode __reassociated = reassociate(__self, ValueNode.isConstantPredicate(), __forX, __forY, __view);
                if (__reassociated != __self)
                {
                    return __reassociated;
                }
            }
        }
        if (__forX instanceof NegateNode)
        {
            return BinaryArithmeticNode.sub(__forY, ((NegateNode) __forX).getValue(), __view);
        }
        else if (__forY instanceof NegateNode)
        {
            return BinaryArithmeticNode.sub(__forX, ((NegateNode) __forY).getValue(), __view);
        }
        if (__self == null)
        {
            __self = (AddNode) new AddNode(__forX, __forY).maybeCommuteInputs();
        }
        return __self;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        ValueNode __ret = super.canonical(__tool, __forX, __forY);
        if (__ret != this)
        {
            return __ret;
        }

        if (__forX.isConstant() && !__forY.isConstant())
        {
            // we try to swap and canonicalize
            ValueNode __improvement = canonical(__tool, __forY, __forX);
            if (__improvement != this)
            {
                return __improvement;
            }
            // if this fails we only swap
            return new AddNode(__forY, __forX);
        }
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add> __op = getOp(__forX, __forY);
        NodeView __view = NodeView.from(__tool);
        return canonical(this, __op, __forX, __forY, __view);
    }

    @Override
    public void generate(NodeLIRBuilderTool __nodeValueMap, ArithmeticLIRGeneratorTool __gen)
    {
        Value __op1 = __nodeValueMap.operand(getX());
        Value __op2 = __nodeValueMap.operand(getY());
        if (shouldSwapInputs(__nodeValueMap))
        {
            Value __tmp = __op1;
            __op1 = __op2;
            __op2 = __tmp;
        }
        __nodeValueMap.setResult(this, __gen.emitAdd(__op1, __op2, false));
    }
}
