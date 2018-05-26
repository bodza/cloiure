package giraaff.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.memory.MemoryCheckpoint;

/**
 * The start node of a graph.
 */
// @NodeInfo.allowedUsageTypes "Memory"
public class StartNode extends BeginStateSplitNode implements MemoryCheckpoint.Single
{
    public static final NodeClass<StartNode> TYPE = NodeClass.create(StartNode.class);

    protected StartNode(NodeClass<? extends StartNode> c)
    {
        super(c);
    }

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
