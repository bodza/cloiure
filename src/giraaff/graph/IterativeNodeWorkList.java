package giraaff.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

// @class IterativeNodeWorkList
public final class IterativeNodeWorkList extends NodeWorkList
{
    // @def
    private static final int EXPLICIT_BITMAP_THRESHOLD = 10;

    // @field
    private int iterationLimit;
    // @field
    private NodeBitMap inQueue;

    // @cons
    public IterativeNodeWorkList(Graph __graph, boolean __fill, int __iterationLimitPerNode)
    {
        super(__graph, __fill);
        iterationLimit = (int) Long.min(__graph.getNodeCount() * (long) __iterationLimitPerNode, Integer.MAX_VALUE);
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
                Node __node = IterativeNodeWorkList.this.worklist.remove();
                if (IterativeNodeWorkList.this.inQueue != null)
                {
                    IterativeNodeWorkList.this.inQueue.clearAndGrow(__node);
                }
                return __node;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public void add(Node __node)
    {
        if (__node != null)
        {
            if (this.inQueue == null && this.worklist.size() > EXPLICIT_BITMAP_THRESHOLD)
            {
                inflateToBitMap(__node.graph());
            }

            if (this.inQueue != null)
            {
                if (this.inQueue.isMarkedAndGrow(__node))
                {
                    return;
                }
            }
            else
            {
                for (Node __queuedNode : this.worklist)
                {
                    if (__queuedNode == __node)
                    {
                        return;
                    }
                }
            }
            if (this.inQueue != null)
            {
                this.inQueue.markAndGrow(__node);
            }
            this.worklist.add(__node);
        }
    }

    @Override
    public boolean contains(Node __node)
    {
        if (this.inQueue != null)
        {
            return this.inQueue.isMarked(__node);
        }
        else
        {
            for (Node __queuedNode : this.worklist)
            {
                if (__queuedNode == __node)
                {
                    return true;
                }
            }
            return false;
        }
    }

    private void inflateToBitMap(Graph __graph)
    {
        this.inQueue = __graph.createNodeBitMap();
        for (Node __queuedNode : this.worklist)
        {
            if (__queuedNode.isAlive())
            {
                this.inQueue.mark(__queuedNode);
            }
        }
    }
}
