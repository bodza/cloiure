package graalvm.compiler.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.Value;

import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.AddNode;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

public final class IntegerAddExactSplitNode extends IntegerExactArithmeticSplitNode
{
    public static final NodeClass<IntegerAddExactSplitNode> TYPE = NodeClass.create(IntegerAddExactSplitNode.class);

    public IntegerAddExactSplitNode(Stamp stamp, ValueNode x, ValueNode y, AbstractBeginNode next, AbstractBeginNode overflowSuccessor)
    {
        super(TYPE, stamp, x, y, next, overflowSuccessor);
    }

    @Override
    protected Value generateArithmetic(NodeLIRBuilderTool gen)
    {
        return gen.getLIRGeneratorTool().getArithmetic().emitAdd(gen.operand(getX()), gen.operand(getY()), true);
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        NodeView view = NodeView.from(tool);
        if (!IntegerStamp.addCanOverflow((IntegerStamp) x.stamp(view), (IntegerStamp) y.stamp(view)))
        {
            tool.deleteBranch(overflowSuccessor);
            tool.addToWorkList(next);
            AddNode replacement = graph().unique(new AddNode(x, y));
            graph().replaceSplitWithFloating(this, replacement, next);
            tool.addToWorkList(replacement);
        }
    }
}
