package giraaff.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

// @class IterativeNodeWorkList
public final class IterativeNodeWorkList extends NodeWorkList
{
    // @def
    private static final int EXPLICIT_BITMAP_THRESHOLD = 10;

    // @field
    private int ___iterationLimit;
    // @field
    private NodeBitMap ___inQueue;

    // @cons IterativeNodeWorkList
    public IterativeNodeWorkList(Graph __graph, boolean __fill, int __iterationLimitPerNode)
    {
        super(__graph, __fill);
        this.___iterationLimit = (int) Long.min(__graph.getNodeCount() * (long) __iterationLimitPerNode, Integer.MAX_VALUE);
    }

    @Override
    public Iterator<Node> iterator()
    {
        // @closure
        return new Iterator<Node>()
        {
            private void dropDeleted()
            {
                while (!IterativeNodeWorkList.this.___worklist.isEmpty() && IterativeNodeWorkList.this.___worklist.peek().isDeleted())
                {
                    IterativeNodeWorkList.this.___worklist.remove();
                }
            }

            @Override
            public boolean hasNext()
            {
                dropDeleted();
                if (IterativeNodeWorkList.this.___iterationLimit <= 0)
                {
                    return false;
                }
                return !IterativeNodeWorkList.this.___worklist.isEmpty();
            }

            @Override
            public Node next()
            {
                if (IterativeNodeWorkList.this.___iterationLimit-- <= 0)
                {
                    throw new NoSuchElementException();
                }
                dropDeleted();
                Node __node = IterativeNodeWorkList.this.___worklist.remove();
                if (IterativeNodeWorkList.this.___inQueue != null)
                {
                    IterativeNodeWorkList.this.___inQueue.clearAndGrow(__node);
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
            if (this.___inQueue == null && this.___worklist.size() > EXPLICIT_BITMAP_THRESHOLD)
            {
                inflateToBitMap(__node.graph());
            }

            if (this.___inQueue != null)
            {
                if (this.___inQueue.isMarkedAndGrow(__node))
                {
                    return;
                }
            }
            else
            {
                for (Node __queuedNode : this.___worklist)
                {
                    if (__queuedNode == __node)
                    {
                        return;
                    }
                }
            }
            if (this.___inQueue != null)
            {
                this.___inQueue.markAndGrow(__node);
            }
            this.___worklist.add(__node);
        }
    }

    @Override
    public boolean contains(Node __node)
    {
        if (this.___inQueue != null)
        {
            return this.___inQueue.isMarked(__node);
        }
        else
        {
            for (Node __queuedNode : this.___worklist)
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
        this.___inQueue = __graph.createNodeBitMap();
        for (Node __queuedNode : this.___worklist)
        {
            if (__queuedNode.isAlive())
            {
                this.___inQueue.mark(__queuedNode);
            }
        }
    }
}
