package giraaff.nodes.calc;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class IntegerDivRemNode
public abstract class IntegerDivRemNode extends FixedBinaryNode implements Lowerable
{
    // @def
    public static final NodeClass<IntegerDivRemNode> TYPE = NodeClass.create(IntegerDivRemNode.class);

    // @enum IntegerDivRemNode.Op
    public enum Op
    {
        DIV,
        REM
    }

    // @enum IntegerDivRemNode.Type
    public enum Type
    {
        SIGNED,
        UNSIGNED
    }

    // @field
    private final Op op;
    // @field
    private final Type type;
    // @field
    private final boolean canDeopt;

    // @cons
    protected IntegerDivRemNode(NodeClass<? extends IntegerDivRemNode> __c, Stamp __stamp, Op __op, Type __type, ValueNode __x, ValueNode __y)
    {
        super(__c, __stamp, __x, __y);
        this.op = __op;
        this.type = __type;

        // Assigning canDeopt during constructor, because it must never change during lifetime of the node.
        IntegerStamp __yStamp = (IntegerStamp) getY().stamp(NodeView.DEFAULT);
        this.canDeopt = __yStamp.contains(0) || __yStamp.contains(-1);
    }

    public final Op getOp()
    {
        return op;
    }

    public final Type getType()
    {
        return type;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public boolean canDeoptimize()
    {
        return canDeopt;
    }
}
