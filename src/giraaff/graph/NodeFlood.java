package giraaff.graph;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

// @class NodeFlood
public final class NodeFlood implements Iterable<Node>
{
    // @field
    private final NodeBitMap ___visited;
    // @field
    private final Queue<Node> ___worklist = new ArrayDeque<>();
    // @field
    private int ___totalMarkedCount;

    // @cons
    public NodeFlood(Graph __graph)
    {
        super();
        this.___visited = __graph.createNodeBitMap();
    }

    public void add(Node __node)
    {
        if (__node != null && !this.___visited.isMarked(__node))
        {
            this.___visited.mark(__node);
            this.___worklist.add(__node);
            this.___totalMarkedCount++;
        }
    }

    public int getTotalMarkedCount()
    {
        return this.___totalMarkedCount;
    }

    public void addAll(Iterable<? extends Node> __nodes)
    {
        for (Node __node : __nodes)
        {
            this.add(__node);
        }
    }

    public NodeBitMap getVisited()
    {
        return this.___visited;
    }

    public boolean isMarked(Node __node)
    {
        return this.___visited.isMarked(__node);
    }

    public boolean isNew(Node __node)
    {
        return this.___visited.isNew(__node);
    }

    @Override
    public Iterator<Node> iterator()
    {
        // @closure
        return new Iterator<Node>()
        {
            @Override
            public boolean hasNext()
            {
                return !NodeFlood.this.___worklist.isEmpty();
            }

            @Override
            public Node next()
            {
                return NodeFlood.this.___worklist.remove();
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    // @class NodeFlood.UnmarkedNodeIterator
    private static final class UnmarkedNodeIterator implements Iterator<Node>
    {
        // @field
        private final NodeBitMap ___visited;
        // @field
        private Iterator<Node> ___nodes;
        // @field
        private Node ___nextNode;

        // @cons
        UnmarkedNodeIterator(NodeBitMap __visited, Iterator<Node> __nodes)
        {
            super();
            this.___visited = __visited;
            this.___nodes = __nodes;
            forward();
        }

        private void forward()
        {
            do
            {
                if (!this.___nodes.hasNext())
                {
                    this.___nextNode = null;
                    return;
                }
                this.___nextNode = this.___nodes.next();
            } while (this.___visited.isMarked(this.___nextNode));
        }

        @Override
        public boolean hasNext()
        {
            return this.___nextNode != null;
        }

        @Override
        public Node next()
        {
            try
            {
                return this.___nextNode;
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

    public Iterable<Node> unmarkedNodes()
    {
        // @closure
        return new Iterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                return new UnmarkedNodeIterator(NodeFlood.this.___visited, NodeFlood.this.___visited.graph().getNodes().iterator());
            }
        };
    }
}
