package giraaff.nodes;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;

// @NodeInfo.allowedUsageTypes "Association"
public final class EndNode extends AbstractEndNode
{
    public static final NodeClass<EndNode> TYPE = NodeClass.create(EndNode.class);

    public EndNode()
    {
        super(TYPE);
    }
}
