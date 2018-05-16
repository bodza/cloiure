package graalvm.compiler.graph;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public final class NodeFlood implements Iterable<Node>
{
    private final NodeBitMap visited;
    private final Queue<Node> worklist;
    private int totalMarkedCount;

    public NodeFlood(Graph graph)
    {
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

    private static class QueueConsumingIterator implements Iterator<Node>
    {
        private final Queue<Node> queue;

        QueueConsumingIterator(Queue<Node> queue)
        {
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

    private static class UnmarkedNodeIterator implements Iterator<Node>
    {
        private final NodeBitMap visited;
        private Iterator<Node> nodes;
        private Node nextNode;

        UnmarkedNodeIterator(NodeBitMap visited, Iterator<Node> nodes)
        {
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
