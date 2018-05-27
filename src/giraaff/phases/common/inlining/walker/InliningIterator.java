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

/**
 * Given a graph, visit all fixed nodes in dominator-based order, collecting in the process the
 * {@link Invoke} nodes with {@link MethodCallTargetNode}. Such list of callsites is returned by
 * {@link #apply()}
 */
public class InliningIterator
{
    private final StartNode start;
    private final Deque<FixedNode> nodeQueue;
    private final NodeBitMap queuedNodes;

    public InliningIterator(StructuredGraph graph)
    {
        this.start = graph.start();
        this.nodeQueue = new ArrayDeque<>();
        this.queuedNodes = graph.createNodeBitMap();
    }

    public LinkedList<Invoke> apply()
    {
        LinkedList<Invoke> invokes = new LinkedList<>();
        FixedNode current;
        forcedQueue(start);

        while ((current = nextQueuedNode()) != null)
        {
            if (current instanceof Invoke && ((Invoke) current).callTarget() instanceof MethodCallTargetNode)
            {
                if (current != start)
                {
                    invokes.addLast((Invoke) current);
                }
                queueSuccessors(current);
            }
            else if (current instanceof LoopBeginNode)
            {
                queueSuccessors(current);
            }
            else if (current instanceof LoopEndNode)
            {
                // nothing to do
            }
            else if (current instanceof AbstractMergeNode)
            {
                queueSuccessors(current);
            }
            else if (current instanceof FixedWithNextNode)
            {
                queueSuccessors(current);
            }
            else if (current instanceof EndNode)
            {
                queueMerge((EndNode) current);
            }
            else if (current instanceof ControlSinkNode)
            {
                // nothing to do
            }
            else if (current instanceof ControlSplitNode)
            {
                queueSuccessors(current);
            }
        }

        return invokes;
    }

    private void queueSuccessors(FixedNode x)
    {
        for (Node node : x.successors())
        {
            queue(node);
        }
    }

    private void queue(Node node)
    {
        if (node != null && !queuedNodes.isMarked(node))
        {
            forcedQueue(node);
        }
    }

    private void forcedQueue(Node node)
    {
        queuedNodes.mark(node);
        nodeQueue.addFirst((FixedNode) node);
    }

    private FixedNode nextQueuedNode()
    {
        if (nodeQueue.isEmpty())
        {
            return null;
        }

        return nodeQueue.removeFirst();
    }

    private void queueMerge(AbstractEndNode end)
    {
        AbstractMergeNode merge = end.merge();
        if (!queuedNodes.isMarked(merge) && visitedAllEnds(merge))
        {
            queuedNodes.mark(merge);
            nodeQueue.add(merge);
        }
    }

    private boolean visitedAllEnds(AbstractMergeNode merge)
    {
        for (int i = 0; i < merge.forwardEndCount(); i++)
        {
            if (!queuedNodes.isMarked(merge.forwardEndAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    private static int count(Iterable<Invoke> invokes)
    {
        int count = 0;
        Iterator<Invoke> iterator = invokes.iterator();
        while (iterator.hasNext())
        {
            iterator.next();
            count++;
        }
        return count;
    }
}
