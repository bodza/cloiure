package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.SubNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class IntegerSubExactSplitNode
public final class IntegerSubExactSplitNode extends IntegerExactArithmeticSplitNode
{
    public static final NodeClass<IntegerSubExactSplitNode> TYPE = NodeClass.create(IntegerSubExactSplitNode.class);

    // @cons
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
