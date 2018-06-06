package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;

// @class FixedBinaryNode
public abstract class FixedBinaryNode extends DeoptimizingFixedWithNextNode implements Canonicalizable.Binary<ValueNode>
{
    // @def
    public static final NodeClass<FixedBinaryNode> TYPE = NodeClass.create(FixedBinaryNode.class);

    @Node.Input
    // @field
    protected ValueNode ___x;
    @Node.Input
    // @field
    protected ValueNode ___y;

    // @cons FixedBinaryNode
    public FixedBinaryNode(NodeClass<? extends FixedBinaryNode> __c, Stamp __stamp, ValueNode __x, ValueNode __y)
    {
        super(__c, __stamp);
        this.___x = __x;
        this.___y = __y;
    }

    @Override
    public ValueNode getX()
    {
        return this.___x;
    }

    @Override
    public ValueNode getY()
    {
        return this.___y;
    }
}
