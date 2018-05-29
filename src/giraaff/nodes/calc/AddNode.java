package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Add;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class AddNode
public class AddNode extends BinaryArithmeticNode<Add> implements NarrowableArithmeticNode, BinaryCommutative<ValueNode>
{
    public static final NodeClass<AddNode> TYPE = NodeClass.create(AddNode.class);

    // @cons
    public AddNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

    // @cons
    protected AddNode(NodeClass<? extends AddNode> c, ValueNode x, ValueNode y)
    {
        super(c, ArithmeticOpTable::getAdd, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        BinaryOp<Add> op = ArithmeticOpTable.forStamp(x.stamp(view)).getAdd();
        Stamp stamp = op.foldStamp(x.stamp(view), y.stamp(view));
        ConstantNode tryConstantFold = tryConstantFold(op, x, y, stamp, view);
        if (tryConstantFold != null)
        {
            return tryConstantFold;
        }
        if (x.isConstant() && !y.isConstant())
        {
            return canonical(null, op, y, x, view);
        }
        else
        {
            return canonical(null, op, x, y, view);
        }
    }

    private static ValueNode canonical(AddNode addNode, BinaryOp<Add> op, ValueNode forX, ValueNode forY, NodeView view)
    {
        AddNode self = addNode;
        boolean associative = op.isAssociative();
        if (associative)
        {
            if (forX instanceof SubNode)
            {
                SubNode sub = (SubNode) forX;
                if (sub.getY() == forY)
                {
                    // (a - b) + b
                    return sub.getX();
                }
            }
            if (forY instanceof SubNode)
            {
                SubNode sub = (SubNode) forY;
                if (sub.getY() == forX)
                {
                    // b + (a - b)
                    return sub.getX();
                }
            }
        }
        if (forY.isConstant())
        {
            Constant c = forY.asConstant();
            if (op.isNeutral(c))
            {
                return forX;
            }
            if (associative && self != null)
            {
                // canonicalize expressions like "(a + 1) + 2"
                ValueNode reassociated = reassociate(self, ValueNode.isConstantPredicate(), forX, forY, view);
                if (reassociated != self)
                {
                    return reassociated;
                }
            }
        }
        if (forX instanceof NegateNode)
        {
            return BinaryArithmeticNode.sub(forY, ((NegateNode) forX).getValue(), view);
        }
        else if (forY instanceof NegateNode)
        {
            return BinaryArithmeticNode.sub(forX, ((NegateNode) forY).getValue(), view);
        }
        if (self == null)
        {
            self = (AddNode) new AddNode(forX, forY).maybeCommuteInputs();
        }
        return self;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        ValueNode ret = super.canonical(tool, forX, forY);
        if (ret != this)
        {
            return ret;
        }

        if (forX.isConstant() && !forY.isConstant())
        {
            // we try to swap and canonicalize
            ValueNode improvement = canonical(tool, forY, forX);
            if (improvement != this)
            {
                return improvement;
            }
            // if this fails we only swap
            return new AddNode(forY, forX);
        }
        BinaryOp<Add> op = getOp(forX, forY);
        NodeView view = NodeView.from(tool);
        return canonical(this, op, forX, forY, view);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        Value op1 = nodeValueMap.operand(getX());
        Value op2 = nodeValueMap.operand(getY());
        if (shouldSwapInputs(nodeValueMap))
        {
            Value tmp = op1;
            op1 = op2;
            op2 = tmp;
        }
        nodeValueMap.setResult(this, gen.emitAdd(op1, op2, false));
    }
}
