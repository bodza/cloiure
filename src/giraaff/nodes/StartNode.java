package giraaff.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodes.memory.MemoryCheckpoint;

///
// The start node of a graph.
///
// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class StartNode
public class StartNode extends BeginStateSplitNode implements MemoryCheckpoint.Single
{
    // @def
    public static final NodeClass<StartNode> TYPE = NodeClass.create(StartNode.class);

    // @cons StartNode
    protected StartNode(NodeClass<? extends StartNode> __c)
    {
        super(__c);
    }

    // @cons StartNode
    public StartNode()
    {
        super(TYPE);
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }
}
