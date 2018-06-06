package giraaff.nodes;

import giraaff.graph.NodeClass;

// @NodeInfo.allowedUsageTypes "InputType.Association"
// @class EndNode
public final class EndNode extends AbstractEndNode
{
    // @def
    public static final NodeClass<EndNode> TYPE = NodeClass.create(EndNode.class);

    // @cons EndNode
    public EndNode()
    {
        super(TYPE);
    }
}
