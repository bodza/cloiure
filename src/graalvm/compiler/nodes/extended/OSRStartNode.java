package graalvm.compiler.nodes.extended;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.StartNode;
import graalvm.compiler.nodes.spi.Lowerable;
import graalvm.compiler.nodes.spi.LoweringTool;

@NodeInfo
public final class OSRStartNode extends StartNode implements Lowerable {
    public static final NodeClass<OSRStartNode> TYPE = NodeClass.create(OSRStartNode.class);

    public OSRStartNode() {
        super(TYPE);
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    public NodeIterable<OSRLocalNode> getOSRLocals() {
        return usages().filter(OSRLocalNode.class);
    }
}
