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

    // @enum IntegerDivRemNode.DivRemOp
    public enum DivRemOp
    {
        DIV,
        REM
    }

    // @enum IntegerDivRemNode.Signedness
    public enum Signedness
    {
        SIGNED,
        UNSIGNED
    }

    // @field
    private final IntegerDivRemNode.DivRemOp ___op;
    // @field
    private final IntegerDivRemNode.Signedness ___type;
    // @field
    private final boolean ___canDeopt;

    // @cons IntegerDivRemNode
    protected IntegerDivRemNode(NodeClass<? extends IntegerDivRemNode> __c, Stamp __stamp, IntegerDivRemNode.DivRemOp __op, IntegerDivRemNode.Signedness __type, ValueNode __x, ValueNode __y)
    {
        super(__c, __stamp, __x, __y);
        this.___op = __op;
        this.___type = __type;

        // Assigning canDeopt during constructor, because it must never change during lifetime of the node.
        IntegerStamp __yStamp = (IntegerStamp) getY().stamp(NodeView.DEFAULT);
        this.___canDeopt = __yStamp.contains(0) || __yStamp.contains(-1);
    }

    public final IntegerDivRemNode.DivRemOp getOp()
    {
        return this.___op;
    }

    public final IntegerDivRemNode.Signedness getType()
    {
        return this.___type;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public boolean canDeoptimize()
    {
        return this.___canDeopt;
    }
}
