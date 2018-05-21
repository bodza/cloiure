package graalvm.compiler.replacements.nodes.arithmetic;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.SubNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.Value;

public final class IntegerSubExactSplitNode extends IntegerExactArithmeticSplitNode
{
    public static final NodeClass<IntegerSubExactSplitNode> TYPE = NodeClass.create(IntegerSubExactSplitNode.class);

    public IntegerSubExactSplitNode(Stamp stamp, ValueNode x, ValueNode y, AbstractBeginNode next, AbstractBeginNode overflowSuccessor)
    {
        super(TYPE, stamp, x, y, next, overflowSuccessor);
    }

    @Override
    protected Value generateArithmetic(NodeLIRBuilderTool gen)
    {
        return gen.getLIRGeneratorTool().getArithmetic().emitSub(gen.operand(getX()), gen.operand(getY()), true);
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        NodeView view = NodeView.from(tool);
        if (!IntegerStamp.subtractionCanOverflow((IntegerStamp) x.stamp(view), (IntegerStamp) y.stamp(view)))
        {
            tool.deleteBranch(overflowSuccessor);
            tool.addToWorkList(next);
            SubNode replacement = graph().unique(new SubNode(x, y));
            graph().replaceSplitWithFloating(this, replacement, next);
            tool.addToWorkList(replacement);
        }
    }
}
