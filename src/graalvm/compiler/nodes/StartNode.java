package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import org.graalvm.word.LocationIdentity;

/**
 * The start node of a graph.
 */
@NodeInfo(allowedUsageTypes = {Memory}, nameTemplate = "Start", cycles = CYCLES_0, size = SIZE_0)
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
