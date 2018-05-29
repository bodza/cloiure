package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.DeoptimizingFixedWithNextNode;
import giraaff.nodes.ValueNode;

// @class FixedBinaryNode
public abstract class FixedBinaryNode extends DeoptimizingFixedWithNextNode implements Canonicalizable.Binary<ValueNode>
{
    public static final NodeClass<FixedBinaryNode> TYPE = NodeClass.create(FixedBinaryNode.class);

    @Input protected ValueNode x;
    @Input protected ValueNode y;

    // @cons
    public FixedBinaryNode(NodeClass<? extends FixedBinaryNode> c, Stamp stamp, ValueNode x, ValueNode y)
    {
        super(c, stamp);
        this.x = x;
        this.y = y;
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
