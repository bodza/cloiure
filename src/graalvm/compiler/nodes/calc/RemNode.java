package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_32;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.BinaryOp;
import graalvm.compiler.core.common.type.ArithmeticOpTable.BinaryOp.Rem;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

@NodeInfo(shortName = "%", cycles = CYCLES_32/* div */)
public class RemNode extends BinaryArithmeticNode<Rem> implements Lowerable
{
    public static final NodeClass<RemNode> TYPE = NodeClass.create(RemNode.class);

    protected RemNode(ValueNode x, ValueNode y)
    {
        this(TYPE, x, y);
    }

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
