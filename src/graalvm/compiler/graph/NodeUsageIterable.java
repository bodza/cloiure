package graalvm.compiler.graph;

import static graalvm.compiler.graph.Graph.isModificationCountsEnabled;

import java.util.Iterator;

import graalvm.compiler.graph.iterators.NodeIterable;

class NodeUsageIterable implements NodeIterable<Node>
{
    private final Node node;

    NodeUsageIterable(Node node)
    {
        this.node = node;
    }

    @Override
    public NodeUsageIterator iterator()
    {
        if (isModificationCountsEnabled())
        {
            return new NodeUsageWithModCountIterator(node);
        }
        else
        {
            return new NodeUsageIterator(node);
        }
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

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        Iterator<Node> iterator = iterator();
        boolean first = true;
        sb.append("usages=");
        sb.append('[');
        while (iterator.hasNext())
        {
            Node input = iterator.next();
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(input);
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }
}
