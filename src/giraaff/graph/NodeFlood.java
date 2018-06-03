package giraaff.graph;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

// @class NodeFlood
public final class NodeFlood implements Iterable<Node>
{
    // @field
    private final NodeBitMap visited;
    // @field
    private final Queue<Node> worklist = new ArrayDeque<>();
    // @field
    private int totalMarkedCount;

    // @cons
    public NodeFlood(Graph __graph)
    {
        super();
        visited = __graph.createNodeBitMap();
    }

    public void add(Node __node)
    {
        if (__node != null && !visited.isMarked(__node))
        {
            visited.mark(__node);
            worklist.add(__node);
            totalMarkedCount++;
        }
    }

    public int getTotalMarkedCount()
    {
        return totalMarkedCount;
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
        return visited;
    }

    public boolean isMarked(Node __node)
    {
        return visited.isMarked(__node);
    }

    public boolean isNew(Node __node)
    {
        return visited.isNew(__node);
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
                return !NodeFlood.this.worklist.isEmpty();
            }

            @Override
            public Node next()
            {
                return NodeFlood.this.worklist.remove();
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
        private final NodeBitMap visited;
        // @field
        private Iterator<Node> nodes;
        // @field
        private Node nextNode;

        // @cons
        UnmarkedNodeIterator(NodeBitMap __visited, Iterator<Node> __nodes)
        {
            super();
            this.visited = __visited;
            this.nodes = __nodes;
            forward();
        }

        private void forward()
        {
            do
            {
                if (!nodes.hasNext())
                {
                    nextNode = null;
                    return;
                }
                nextNode = nodes.next();
            } while (visited.isMarked(nextNode));
        }

        @Override
        public boolean hasNext()
        {
            return nextNode != null;
        }

        @Override
        public Node next()
        {
            try
            {
                return nextNode;
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
                return new UnmarkedNodeIterator(visited, visited.graph().getNodes().iterator());
            }
        };
    }
}
