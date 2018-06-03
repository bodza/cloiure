package giraaff.graph;

import java.util.Iterator;

/**
 * Iterates over the nodes in a given graph.
 */
// @class GraphNodeIterator
final class GraphNodeIterator implements Iterator<Node>
{
    // @field
    private final Graph graph;
    // @field
    private int index;

    // @cons
    GraphNodeIterator(Graph __graph)
    {
        this(__graph, 0);
    }

    // @cons
    GraphNodeIterator(Graph __graph, int __index)
    {
        super();
        this.graph = __graph;
        this.index = __index - 1;
        forward();
    }

    private void forward()
    {
        if (index < graph.nodesSize)
        {
            do
            {
                index++;
            } while (index < graph.nodesSize && graph.nodes[index] == null);
        }
    }

    @Override
    public boolean hasNext()
    {
        checkForDeletedNode();
        return index < graph.nodesSize;
    }

    private void checkForDeletedNode()
    {
        while (index < graph.nodesSize && graph.nodes[index] == null)
        {
            index++;
        }
    }

    @Override
    public Node next()
    {
        try
        {
            return graph.nodes[index];
        }
        finally
        {
            forward();
        }
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
