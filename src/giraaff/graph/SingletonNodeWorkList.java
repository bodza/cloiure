package giraaff.graph;

import java.util.Iterator;

// @class SingletonNodeWorkList
public final class SingletonNodeWorkList extends NodeWorkList
{
    // @field
    private final NodeBitMap ___visited;

    // @cons SingletonNodeWorkList
    public SingletonNodeWorkList(Graph __graph)
    {
        super(__graph, false);
        this.___visited = __graph.createNodeBitMap();
    }

    @Override
    public void add(Node __node)
    {
        if (__node != null)
        {
            if (!this.___visited.isMarkedAndGrow(__node))
            {
                this.___visited.mark(__node);
                this.___worklist.add(__node);
            }
        }
    }

    @Override
    public boolean contains(Node __node)
    {
        return this.___visited.isMarked(__node);
    }

    @Override
    public Iterator<Node> iterator()
    {
        // @closure
        return new Iterator<Node>()
        {
            private void dropDeleted()
            {
                while (!SingletonNodeWorkList.this.___worklist.isEmpty() && SingletonNodeWorkList.this.___worklist.peek().isDeleted())
                {
                    SingletonNodeWorkList.this.___worklist.remove();
                }
            }

            @Override
            public boolean hasNext()
            {
                dropDeleted();
                return !SingletonNodeWorkList.this.___worklist.isEmpty();
            }

            @Override
            public Node next()
            {
                dropDeleted();
                return SingletonNodeWorkList.this.___worklist.remove();
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
