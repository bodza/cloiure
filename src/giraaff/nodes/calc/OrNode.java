package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Or;
import giraaff.core.common.type.PrimitiveStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.util.GraphUtil;

public final class OrNode extends BinaryArithmeticNode<Or> implements BinaryCommutative<ValueNode>, NarrowableArithmeticNode
{
    public static final NodeClass<OrNode> TYPE = NodeClass.create(OrNode.class);

    public OrNode(ValueNode x, ValueNode y)
    {
        super(TYPE, ArithmeticOpTable::getOr, x, y);
    }

    public static ValueNode create(ValueNode x, ValueNode y, NodeView view)
    {
        BinaryOp<Or> op = ArithmeticOpTable.forStamp(x.stamp(view)).getOr();
        Stamp stamp = op.foldStamp(x.stamp(view), y.stamp(view));
        ConstantNode tryConstantFold = tryConstantFold(op, x, y, stamp, view);
        if (tryConstantFold != null)
        {
            return tryConstantFold;
        }
        return canonical(null, op, stamp, x, y, view);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode ret = super.canonical(tool, forX, forY);
        if (ret != this)
        {
            return ret;
        }

        return canonical(this, getOp(forX, forY), stamp(view), forX, forY, view);
    }

    private static ValueNode canonical(OrNode self, BinaryOp<Or> op, Stamp stamp, ValueNode forX, ValueNode forY, NodeView view)
    {
        if (GraphUtil.unproxify(forX) == GraphUtil.unproxify(forY))
        {
            return forX;
        }
        if (forX.isConstant() && !forY.isConstant())
        {
            return new OrNode(forY, forX);
        }
        if (forY.isConstant())
        {
            Constant c = forY.asConstant();
            if (op.isNeutral(c))
            {
                return forX;
            }

            if (c instanceof PrimitiveConstant && ((PrimitiveConstant) c).getJavaKind().isNumericInteger())
            {
                long rawY = ((PrimitiveConstant) c).asLong();
                long mask = CodeUtil.mask(PrimitiveStamp.getBits(stamp));
                if ((rawY & mask) == mask)
                {
                    return ConstantNode.forIntegerStamp(stamp, mask);
                }
            }
            return reassociate(self != null ? self : (OrNode) new OrNode(forX, forY).maybeCommuteInputs(), ValueNode.isConstantPredicate(), forX, forY, view);
        }
        if (forX instanceof NotNode && forY instanceof NotNode)
        {
            return new NotNode(AndNode.create(((NotNode) forX).getValue(), ((NotNode) forY).getValue(), view));
        }
        return self != null ? self : new OrNode(forX, forY).maybeCommuteInputs();
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitOr(nodeValueMap.operand(getX()), nodeValueMap.operand(getY())));
    }
}
