package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

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