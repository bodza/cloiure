package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Memory;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import org.graalvm.word.LocationIdentity;

/**
 * The start node of a graph.
 */
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
