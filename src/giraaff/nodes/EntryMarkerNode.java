package giraaff.nodes;

import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.util.GraalError;

/**
 * This node will be inserted at point specified by {@link StructuredGraph#getEntryBCI()}, usually
 * by the graph builder.
 */
// @NodeInfo.allowedUsageTypes "Association"
public final class EntryMarkerNode extends BeginStateSplitNode implements IterableNodeType, LIRLowerable
{
    public static final NodeClass<EntryMarkerNode> TYPE = NodeClass.create(EntryMarkerNode.class);

    public EntryMarkerNode()
    {
        super(TYPE);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        throw new GraalError("OnStackReplacementNode should not survive");
    }
}
