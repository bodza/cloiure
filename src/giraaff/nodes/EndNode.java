package giraaff.nodes;

import giraaff.graph.NodeClass;

// @NodeInfo.allowedUsageTypes "Association"
// @class EndNode
public final class EndNode extends AbstractEndNode
{
    public static final NodeClass<EndNode> TYPE = NodeClass.create(EndNode.class);

    // @cons
    public EndNode()
    {
        super(TYPE);
    }
}
