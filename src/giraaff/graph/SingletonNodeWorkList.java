package giraaff.graph;

import java.util.Iterator;

// @class SingletonNodeWorkList
public final class SingletonNodeWorkList extends NodeWorkList
{
    // @field
    private final NodeBitMap visited;

    // @cons
    public SingletonNodeWorkList(Graph __graph)
    {
        super(__graph, false);
        this.visited = __graph.createNodeBitMap();
    }

    @Override
    public void add(Node __node)
    {
        if (__node != null)
        {
            if (!this.visited.isMarkedAndGrow(__node))
            {
                this.visited.mark(__node);
                this.worklist.add(__node);
            }
        }
    }

    @Override
    public boolean contains(Node __node)
    {
        return this.visited.isMarked(__node);
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
