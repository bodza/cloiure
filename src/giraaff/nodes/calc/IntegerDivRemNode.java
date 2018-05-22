package giraaff.nodes.calc;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

public abstract class IntegerDivRemNode extends FixedBinaryNode implements Lowerable
{
    public static final NodeClass<IntegerDivRemNode> TYPE = NodeClass.create(IntegerDivRemNode.class);

    public enum Op
    {
        DIV,
        REM
    }

    public enum Type
    {
        SIGNED,
        UNSIGNED
    }

    private final Op op;
    private final Type type;
    private final boolean canDeopt;

    protected IntegerDivRemNode(NodeClass<? extends IntegerDivRemNode> c, Stamp stamp, Op op, Type type, ValueNode x, ValueNode y)
    {
        super(c, stamp, x, y);
        this.op = op;
        this.type = type;

        // Assigning canDeopt during constructor, because it must never change during lifetime of
        // the node.
        IntegerStamp yStamp = (IntegerStamp) getY().stamp(NodeView.DEFAULT);
        this.canDeopt = yStamp.contains(0) || yStamp.contains(-1);
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
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public boolean canDeoptimize()
    {
        return canDeopt;
    }
}
