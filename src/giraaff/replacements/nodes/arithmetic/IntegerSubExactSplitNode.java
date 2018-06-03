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
    // @def
    public static final NodeClass<IntegerSubExactSplitNode> TYPE = NodeClass.create(IntegerSubExactSplitNode.class);

    // @cons
    public IntegerSubExactSplitNode(Stamp __stamp, ValueNode __x, ValueNode __y, AbstractBeginNode __next, AbstractBeginNode __overflowSuccessor)
    {
        super(TYPE, __stamp, __x, __y, __next, __overflowSuccessor);
    }

    @Override
    protected Value generateArithmetic(NodeLIRBuilderTool __gen)
    {
        return __gen.getLIRGeneratorTool().getArithmetic().emitSub(__gen.operand(getX()), __gen.operand(getY()), true);
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        if (!IntegerStamp.subtractionCanOverflow((IntegerStamp) this.___x.stamp(__view), (IntegerStamp) this.___y.stamp(__view)))
        {
            __tool.deleteBranch(this.___overflowSuccessor);
            __tool.addToWorkList(this.___next);
            SubNode __replacement = graph().unique(new SubNode(this.___x, this.___y));
            graph().replaceSplitWithFloating(this, __replacement, this.___next);
            __tool.addToWorkList(__replacement);
        }
    }
}
