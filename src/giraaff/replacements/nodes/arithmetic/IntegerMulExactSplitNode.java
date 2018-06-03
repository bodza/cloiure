package giraaff.replacements.nodes.arithmetic;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.MulNode;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class IntegerMulExactSplitNode
public final class IntegerMulExactSplitNode extends IntegerExactArithmeticSplitNode
{
    // @def
    public static final NodeClass<IntegerMulExactSplitNode> TYPE = NodeClass.create(IntegerMulExactSplitNode.class);

    // @cons
    public IntegerMulExactSplitNode(Stamp __stamp, ValueNode __x, ValueNode __y, AbstractBeginNode __next, AbstractBeginNode __overflowSuccessor)
    {
        super(TYPE, __stamp, __x, __y, __next, __overflowSuccessor);
    }

    @Override
    protected Value generateArithmetic(NodeLIRBuilderTool __gen)
    {
        return __gen.getLIRGeneratorTool().getArithmetic().emitMul(__gen.operand(getX()), __gen.operand(getY()), true);
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        NodeView __view = NodeView.from(__tool);
        if (!IntegerStamp.multiplicationCanOverflow((IntegerStamp) this.___x.stamp(__view), (IntegerStamp) this.___y.stamp(__view)))
        {
            __tool.deleteBranch(this.___overflowSuccessor);
            __tool.addToWorkList(this.___next);
            MulNode __replacement = graph().unique(new MulNode(this.___x, this.___y));
            graph().replaceSplitWithFloating(this, __replacement, this.___next);
            __tool.addToWorkList(__replacement);
        }
    }
}
