package giraaff.graph;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

// @class NodeFlood
public final class NodeFlood implements Iterable<Node>
{
    private final NodeBitMap visited;
    private final Queue<Node> worklist;
    private int totalMarkedCount;

    // @cons
    public NodeFlood(Graph graph)
    {
        super();
        visited = graph.createNodeBitMap();
        worklist = new ArrayDeque<>();
    }

    public void add(Node node)
    {
        if (node != null && !visited.isMarked(node))
        {
            visited.mark(node);
            worklist.add(node);
            totalMarkedCount++;
        }
    }

    public int getTotalMarkedCount()
    {
        return totalMarkedCount;
    }

    public void addAll(Iterable<? extends Node> nodes)
    {
        for (Node node : nodes)
        {
            this.add(node);
        }
    }

    public NodeBitMap getVisited()
    {
        return visited;
    }

    public boolean isMarked(Node node)
    {
        return visited.isMarked(node);
    }

    public boolean isNew(Node node)
    {
        return visited.isNew(node);
    }

    // @class NodeFlood.QueueConsumingIterator
    private static final class QueueConsumingIterator implements Iterator<Node>
    {
        private final Queue<Node> queue;

        // @cons
        QueueConsumingIterator(Queue<Node> queue)
        {
            super();
            this.queue = queue;
        }

        @Override
        public boolean hasNext()
        {
            return !queue.isEmpty();
        }

        @Override
        public Node next()
        {
            return queue.remove();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Node> iterator()
    {
        return new QueueConsumingIterator(worklist);
    }

    // @class NodeFlood.UnmarkedNodeIterator
    private static final class UnmarkedNodeIterator implements Iterator<Node>
    {
        private final NodeBitMap visited;
        private Iterator<Node> nodes;
        private Node nextNode;

        // @cons
        UnmarkedNodeIterator(NodeBitMap visited, Iterator<Node> nodes)
        {
            super();
            this.visited = visited;
            this.nodes = nodes;
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
