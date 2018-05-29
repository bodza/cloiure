package giraaff.nodes.extended;

import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.StartNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

// @class OSRStartNode
public final class OSRStartNode extends StartNode implements Lowerable
{
    public static final NodeClass<OSRStartNode> TYPE = NodeClass.create(OSRStartNode.class);

    // @cons
    public OSRStartNode()
    {
        super(TYPE);
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    public NodeIterable<OSRLocalNode> getOSRLocals()
    {
        return usages().filter(OSRLocalNode.class);
    }
}
