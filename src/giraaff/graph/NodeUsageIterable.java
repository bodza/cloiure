package giraaff.graph;

import java.util.Iterator;

import giraaff.graph.iterators.NodeIterable;

// @class NodeUsageIterable
final class NodeUsageIterable implements NodeIterable<Node>
{
    private final Node node;

    // @cons
    NodeUsageIterable(Node node)
    {
        super();
        this.node = node;
    }

    @Override
    public NodeUsageIterator iterator()
    {
        return new NodeUsageIterator(node);
    }

    @Override
    public Node first()
    {
        return node.usage0;
    }

    @Override
    public boolean isEmpty()
    {
        return node.usage0 == null;
    }

    @Override
    public boolean isNotEmpty()
    {
        return node.usage0 != null;
    }

    @Override
    public int count()
    {
        return node.getUsageCount();
    }
}
