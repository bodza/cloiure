package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;

/**
 * Base class of all nodes that are fixed within the control flow graph and have an immediate
 * successor.
 */
@NodeInfo
public abstract class FixedWithNextNode extends FixedNode {
    public static final NodeClass<FixedWithNextNode> TYPE = NodeClass.create(FixedWithNextNode.class);

    @Successor protected FixedNode next;

    public FixedNode next() {
        return next;
    }

    public void setNext(FixedNode x) {
        updatePredecessor(next, x);
        next = x;
    }

    public FixedWithNextNode(NodeClass<? extends FixedWithNextNode> c, Stamp stamp) {
        super(c, stamp);
    }

    @Override
    public FixedWithNextNode asNode() {
        return this;
    }
}
