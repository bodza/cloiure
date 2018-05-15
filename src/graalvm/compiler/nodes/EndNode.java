package graalvm.compiler.nodes;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeInfo;

@NodeInfo(allowedUsageTypes = {InputType.Association}, nameTemplate = "End")
public final class EndNode extends AbstractEndNode {
    public static final NodeClass<EndNode> TYPE = NodeClass.create(EndNode.class);

    public EndNode() {
        super(TYPE);
    }

}