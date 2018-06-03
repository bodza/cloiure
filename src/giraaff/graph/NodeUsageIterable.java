package giraaff.graph;

import java.util.Iterator;

import giraaff.graph.iterators.NodeIterable;

// @class NodeUsageIterable
final class NodeUsageIterable implements NodeIterable<Node>
{
    // @field
    private final Node ___node;

    // @cons
    NodeUsageIterable(Node __node)
    {
        super();
        this.___node = __node;
    }

    @Override
    public NodeUsageIterator iterator()
    {
        return new NodeUsageIterator(this.___node);
    }

    @Override
    public Node first()
    {
        return this.___node.___usage0;
    }

    @Override
    public boolean isEmpty()
    {
        return this.___node.___usage0 == null;
    }

    @Override
    public boolean isNotEmpty()
    {
        return this.___node.___usage0 != null;
    }

    @Override
    public int count()
    {
        return this.___node.getUsageCount();
    }
}
