package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Association;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_0;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * This node will be inserted at point specified by {@link StructuredGraph#getEntryBCI()}, usually
 * by the graph builder.
 */
@NodeInfo(allowedUsageTypes = Association, cycles = CYCLES_0, size = SIZE_0)
public final class EntryMarkerNode extends BeginStateSplitNode implements IterableNodeType, LIRLowerable {
    public static final NodeClass<EntryMarkerNode> TYPE = NodeClass.create(EntryMarkerNode.class);

    public EntryMarkerNode() {
        super(TYPE);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        throw new GraalError("OnStackReplacementNode should not survive");
    }
}
