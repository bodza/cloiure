package giraaff.phases.common.inlining.walker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.java.MethodCallTargetNode;

///
// Given a graph, visit all fixed nodes in dominator-based order, collecting in the process the
// {@link Invoke} nodes with {@link MethodCallTargetNode}. Such list of callsites is returned by
// {@link #apply()}
///
// @class InliningIterator
public final class InliningIterator
{
    // @field
    private final StartNode ___start;
    // @field
    private final Deque<FixedNode> ___nodeQueue;
    // @field
    private final NodeBitMap ___queuedNodes;

    // @cons
    public InliningIterator(StructuredGraph __graph)
    {
        super();
        this.___start = __graph.start();
        this.___nodeQueue = new ArrayDeque<>();
        this.___queuedNodes = __graph.createNodeBitMap();
    }

    public LinkedList<Invoke> apply()
    {
        LinkedList<Invoke> __invokes = new LinkedList<>();
        FixedNode __current;
        forcedQueue(this.___start);

        while ((__current = nextQueuedNode()) != null)
        {
            if (__current instanceof Invoke && ((Invoke) __current).callTarget() instanceof MethodCallTargetNode)
            {
                if (__current != this.___start)
                {
                    __invokes.addLast((Invoke) __current);
                }
                queueSuccessors(__current);
            }
            else if (__current instanceof LoopBeginNode)
            {
                queueSuccessors(__current);
            }
            else if (__current instanceof LoopEndNode)
            {
                // nothing to do
            }
            else if (__current instanceof AbstractMergeNode)
            {
                queueSuccessors(__current);
            }
            else if (__current instanceof FixedWithNextNode)
            {
                queueSuccessors(__current);
            }
            else if (__current instanceof EndNode)
            {
                queueMerge((EndNode) __current);
            }
            else if (__current instanceof ControlSinkNode)
            {
                // nothing to do
            }
            else if (__current instanceof ControlSplitNode)
            {
                queueSuccessors(__current);
            }
        }

        return __invokes;
    }

    private void queueSuccessors(FixedNode __x)
    {
        for (Node __node : __x.successors())
        {
            queue(__node);
        }
    }

    private void queue(Node __node)
    {
        if (__node != null && !this.___queuedNodes.isMarked(__node))
        {
            forcedQueue(__node);
        }
    }

    private void forcedQueue(Node __node)
    {
        this.___queuedNodes.mark(__node);
        this.___nodeQueue.addFirst((FixedNode) __node);
    }

    private FixedNode nextQueuedNode()
    {
        if (this.___nodeQueue.isEmpty())
        {
            return null;
        }

        return this.___nodeQueue.removeFirst();
    }

    private void queueMerge(AbstractEndNode __end)
    {
        AbstractMergeNode __merge = __end.merge();
        if (!this.___queuedNodes.isMarked(__merge) && visitedAllEnds(__merge))
        {
            this.___queuedNodes.mark(__merge);
            this.___nodeQueue.add(__merge);
        }
    }

    private boolean visitedAllEnds(AbstractMergeNode __merge)
    {
        for (int __i = 0; __i < __merge.forwardEndCount(); __i++)
        {
            if (!this.___queuedNodes.isMarked(__merge.forwardEndAt(__i)))
            {
                return false;
            }
        }
        return true;
    }

    private static int count(Iterable<Invoke> __invokes)
    {
        int __count = 0;
        Iterator<Invoke> __iterator = __invokes.iterator();
        while (__iterator.hasNext())
        {
            __iterator.next();
            __count++;
        }
        return __count;
    }
}
