package giraaff.nodes.calc;

import giraaff.core.common.type.ArithmeticOpTable;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Rem;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.lir.gen.ArithmeticLIRGeneratorTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class RemNode
public final class RemNode extends BinaryArithmeticNode<Rem> implements Lowerable
{
    public static final NodeClass<RemNode> TYPE = NodeClass.create(RemNode.class);

    // @cons
    protected RemNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

    // @cons
    protected RemNode(NodeClass<? extends RemNode> c, ValueNode x, ValueNode y)
    {
        super(c, ArithmeticOpTable::getRem, x, y);
    }

    public static ValueNode create(ValueNode forX, ValueNode forY, NodeView view)
    {
        BinaryOp<Rem> op = ArithmeticOpTable.forStamp(forX.stamp(view)).getRem();
        Stamp stamp = op.foldStamp(forX.stamp(view), forY.stamp(view));
        ConstantNode tryConstantFold = tryConstantFold(op, forX, forY, stamp, view);
        if (tryConstantFold != null)
        {
            return tryConstantFold;
        }
        return new RemNode(forX, forY);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitRem(nodeValueMap.operand(getX()), nodeValueMap.operand(getY()), null));
    }
}
