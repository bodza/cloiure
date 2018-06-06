package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;

// @class FixedNode
public abstract class FixedNode extends ValueNode implements FixedNodeInterface
{
    // @def
    public static final NodeClass<FixedNode> TYPE = NodeClass.create(FixedNode.class);

    // @cons FixedNode
    protected FixedNode(NodeClass<? extends FixedNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    @Override
    public FixedNode asNode()
    {
        return this;
    }
}
