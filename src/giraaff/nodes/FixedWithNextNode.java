package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;

/**
 * Base class of all nodes that are fixed within the control flow graph and have an immediate successor.
 */
// @class FixedWithNextNode
public abstract class FixedWithNextNode extends FixedNode
{
    // @def
    public static final NodeClass<FixedWithNextNode> TYPE = NodeClass.create(FixedWithNextNode.class);

    @Successor
    // @field
    protected FixedNode next;

    public FixedNode next()
    {
        return next;
    }

    public void setNext(FixedNode __x)
    {
        updatePredecessor(next, __x);
        next = __x;
    }

    // @cons
    public FixedWithNextNode(NodeClass<? extends FixedWithNextNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    @Override
    public FixedWithNextNode asNode()
    {
        return this;
    }
}
