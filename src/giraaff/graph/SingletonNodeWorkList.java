package giraaff.graph;

import java.util.Iterator;

// @class SingletonNodeWorkList
public final class SingletonNodeWorkList extends NodeWorkList
{
    private final NodeBitMap visited;

    // @cons
    public SingletonNodeWorkList(Graph graph)
    {
        super(graph, false);
        this.visited = graph.createNodeBitMap();
    }

    @Override
    public void add(Node node)
    {
        if (node != null)
        {
            if (!this.visited.isMarkedAndGrow(node))
            {
                this.visited.mark(node);
                this.worklist.add(node);
            }
        }
    }

    @Override
    public boolean contains(Node node)
    {
        return this.visited.isMarked(node);
    }

    @Override
    public Iterator<Node> iterator()
    {
        // @closure
        return new Iterator<Node>()
        {
            private void dropDeleted()
            {
                while (!SingletonNodeWorkList.this.worklist.isEmpty() && SingletonNodeWorkList.this.worklist.peek().isDeleted())
                {
                    SingletonNodeWorkList.this.worklist.remove();
                }
            }

            @Override
            public boolean hasNext()
            {
                dropDeleted();
                return !SingletonNodeWorkList.this.worklist.isEmpty();
            }

            @Override
            public Node next()
            {
                dropDeleted();
                return SingletonNodeWorkList.this.worklist.remove();
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
