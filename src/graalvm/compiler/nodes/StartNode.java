package graalvm.compiler.nodes;

import org.graalvm.word.LocationIdentity;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;

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
