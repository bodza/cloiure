package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;

@NodeInfo
public abstract class FixedNode extends ValueNode implements FixedNodeInterface {
    public static final NodeClass<FixedNode> TYPE = NodeClass.create(FixedNode.class);

    protected FixedNode(NodeClass<? extends FixedNode> c, Stamp stamp) {
        super(c, stamp);
    }

    @Override
    public boolean verify() {
        assertTrue(this.successors().isNotEmpty() || this.predecessor() != null, "FixedNode should not float");
        return super.verify();
    }

    @Override
    public FixedNode asNode() {
        return this;
    }
}
