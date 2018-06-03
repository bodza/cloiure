package giraaff.graph;

import java.util.Iterator;

///
// Iterates over the nodes in a given graph.
///
// @class GraphNodeIterator
final class GraphNodeIterator implements Iterator<Node>
{
    // @field
    private final Graph ___graph;
    // @field
    private int ___index;

    // @cons
    GraphNodeIterator(Graph __graph)
    {
        this(__graph, 0);
    }

    // @cons
    GraphNodeIterator(Graph __graph, int __index)
    {
        super();
        this.___graph = __graph;
        this.___index = __index - 1;
        forward();
    }

    private void forward()
    {
        if (this.___index < this.___graph.___nodesSize)
        {
            do
            {
                this.___index++;
            } while (this.___index < this.___graph.___nodesSize && this.___graph.___nodes[this.___index] == null);
        }
    }

    @Override
    public boolean hasNext()
    {
        checkForDeletedNode();
        return this.___index < this.___graph.___nodesSize;
    }

    private void checkForDeletedNode()
    {
        while (this.___index < this.___graph.___nodesSize && this.___graph.___nodes[this.___index] == null)
        {
            this.___index++;
        }
    }

    @Override
    public Node next()
    {
        try
        {
            return this.___graph.___nodes[this.___index];
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
