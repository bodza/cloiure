package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;

public abstract class FixedNode extends ValueNode implements FixedNodeInterface
{
    public static final NodeClass<FixedNode> TYPE = NodeClass.create(FixedNode.class);

    protected FixedNode(NodeClass<? extends FixedNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    @Override
    public FixedNode asNode()
    {
        return this;
    }
}
