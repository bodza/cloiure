package giraaff.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

// @class IterativeNodeWorkList
public final class IterativeNodeWorkList extends NodeWorkList
{
    private static final int EXPLICIT_BITMAP_THRESHOLD = 10;

    private int iterationLimit;
    private NodeBitMap inQueue;

    // @cons
    public IterativeNodeWorkList(Graph graph, boolean fill, int iterationLimitPerNode)
    {
        super(graph, fill);
        iterationLimit = (int) Long.min(graph.getNodeCount() * (long) iterationLimitPerNode, Integer.MAX_VALUE);
    }

    @Override
    public Iterator<Node> iterator()
    {
        // @closure
        return new Iterator<Node>()
        {
            private void dropDeleted()
            {
                while (!IterativeNodeWorkList.this.worklist.isEmpty() && IterativeNodeWorkList.this.worklist.peek().isDeleted())
                {
                    IterativeNodeWorkList.this.worklist.remove();
                }
            }

            @Override
            public boolean hasNext()
            {
                dropDeleted();
                if (iterationLimit <= 0)
                {
                    return false;
                }
                return !IterativeNodeWorkList.this.worklist.isEmpty();
            }

            @Override
            public Node next()
            {
                if (iterationLimit-- <= 0)
                {
                    throw new NoSuchElementException();
                }
                dropDeleted();
                Node node = IterativeNodeWorkList.this.worklist.remove();
                if (IterativeNodeWorkList.this.inQueue != null)
                {
                    IterativeNodeWorkList.this.inQueue.clearAndGrow(node);
                }
                return node;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void add(Node node)
    {
        if (node != null)
        {
            if (this.inQueue == null && this.worklist.size() > EXPLICIT_BITMAP_THRESHOLD)
            {
                inflateToBitMap(node.graph());
            }

            if (this.inQueue != null)
            {
                if (this.inQueue.isMarkedAndGrow(node))
                {
                    return;
                }
            }
            else
            {
                for (Node queuedNode : this.worklist)
                {
                    if (queuedNode == node)
                    {
                        return;
                    }
                }
            }
            if (this.inQueue != null)
            {
                this.inQueue.markAndGrow(node);
            }
            this.worklist.add(node);
        }
    }

    @Override
    public boolean contains(Node node)
    {
        if (this.inQueue != null)
        {
            return this.inQueue.isMarked(node);
        }
        else
        {
            for (Node queuedNode : this.worklist)
            {
                if (queuedNode == node)
                {
                    return true;
                }
            }
            return false;
        }
    }

    private void inflateToBitMap(Graph graph)
    {
        this.inQueue = graph.createNodeBitMap();
        for (Node queuedNode : this.worklist)
        {
            if (queuedNode.isAlive())
            {
                this.inQueue.mark(queuedNode);
            }
        }
    }
}
