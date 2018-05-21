package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Association;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * This node will be inserted at point specified by {@link StructuredGraph#getEntryBCI()}, usually
 * by the graph builder.
 */
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
