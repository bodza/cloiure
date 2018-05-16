package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_16;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_1;

import graalvm.compiler.core.common.type.ArithmeticOpTable;
import graalvm.compiler.core.common.type.ArithmeticOpTable.UnaryOp.Sqrt;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.lir.gen.ArithmeticLIRGeneratorTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArithmeticLIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Square root.
 */
@NodeInfo(cycles = CYCLES_16, size = SIZE_1)
public final class SqrtNode extends UnaryArithmeticNode<Sqrt> implements ArithmeticLIRLowerable
{
    public static final NodeClass<SqrtNode> TYPE = NodeClass.create(SqrtNode.class);

    protected SqrtNode(ValueNode x)
    {
        super(TYPE, ArithmeticOpTable::getSqrt, x);
    }

    public static ValueNode create(ValueNode x, NodeView view)
    {
        if (x.isConstant())
        {
            ArithmeticOpTable.UnaryOp<Sqrt> op = ArithmeticOpTable.forStamp(x.stamp(view)).getSqrt();
            return ConstantNode.forPrimitive(op.foldStamp(x.stamp(view)), op.foldConstant(x.asConstant()));
        }
        return new SqrtNode(x);
    }

    @Override
    public void generate(NodeLIRBuilderTool nodeValueMap, ArithmeticLIRGeneratorTool gen)
    {
        nodeValueMap.setResult(this, gen.emitMathSqrt(nodeValueMap.operand(getValue())));
    }
}
