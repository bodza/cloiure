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

// @class IntegerAddExactSplitNode
public final class IntegerAddExactSplitNode extends IntegerExactArithmeticSplitNode
{
    // @def
    public static final NodeClass<IntegerAddExactSplitNode> TYPE = NodeClass.create(IntegerAddExactSplitNode.class);

    // @cons
    public IntegerAddExactSplitNode(Stamp __stamp, ValueNode __x, ValueNode __y, AbstractBeginNode __next, AbstractBeginNode __overflowSuccessor)
    {
        super(TYPE, __stamp, __x, __y, __next, __overflowSuccessor);
    }

    @Override
    protected Value generateArithmetic(NodeLIRBuilderTool __gen)
    {
        return __gen.getLIRGeneratorTool().getArithmetic().emitAdd(__gen.operand(getX()), __gen.operand(getY()), true);
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        if (!IntegerStamp.addCanOverflow((IntegerStamp) x.stamp(__view), (IntegerStamp) y.stamp(__view)))
        {
            __tool.deleteBranch(overflowSuccessor);
            __tool.addToWorkList(next);
            AddNode __replacement = graph().unique(new AddNode(x, y));
            graph().replaceSplitWithFloating(this, __replacement, next);
            __tool.addToWorkList(__replacement);
        }
    }
}
