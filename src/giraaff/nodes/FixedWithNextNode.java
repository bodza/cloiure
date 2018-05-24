package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;

/**
 * Base class of all nodes that are fixed within the control flow graph and have an immediate successor.
 */
public abstract class FixedWithNextNode extends FixedNode
{
    public static final NodeClass<FixedWithNextNode> TYPE = NodeClass.create(FixedWithNextNode.class);

    @Successor protected FixedNode next;

    public FixedNode next()
    {
        return next;
    }

    public void setNext(FixedNode x)
    {
        updatePredecessor(next, x);
        next = x;
    }

    public FixedWithNextNode(NodeClass<? extends FixedWithNextNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    @Override
    public FixedWithNextNode asNode()
    {
        return this;
    }
}
