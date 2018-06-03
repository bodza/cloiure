package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;

// @class FixedBinaryNode
public abstract class FixedBinaryNode extends DeoptimizingFixedWithNextNode implements Canonicalizable.Binary<ValueNode>
{
    // @def
    public static final NodeClass<FixedBinaryNode> TYPE = NodeClass.create(FixedBinaryNode.class);

    @Input
    // @field
    protected ValueNode x;
    @Input
    // @field
    protected ValueNode y;

    // @cons
    public FixedBinaryNode(NodeClass<? extends FixedBinaryNode> __c, Stamp __stamp, ValueNode __x, ValueNode __y)
    {
        super(__c, __stamp);
        this.x = __x;
        this.y = __y;
    }

    @Override
    public ValueNode getX()
    {
        return x;
    }

    @Override
    public ValueNode getY()
    {
        return y;
    }
}
