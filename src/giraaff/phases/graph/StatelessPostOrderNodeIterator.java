package giraaff.phases.graph;

import java.util.ArrayDeque;
import java.util.Deque;

import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;

/**
 * This iterator implements a reverse post order iteration over the fixed nodes in the graph,
 * starting at the given fixed node.
 *
 * No changes to the fixed nodes are expected during the iteration, they cause undefined behavior.
 */
public abstract class StatelessPostOrderNodeIterator
{
    private final NodeBitMap visitedEnds;
    private final Deque<AbstractBeginNode> nodeQueue;
    private final FixedNode start;

    public StatelessPostOrderNodeIterator(FixedNode start)
    {
        visitedEnds = start.graph().createNodeBitMap();
        nodeQueue = new ArrayDeque<>();
        this.start = start;
    }

    public void apply()
    {
        FixedNode current = start;

        do
        {
            if (current instanceof LoopBeginNode)
            {
                loopBegin((LoopBeginNode) current);
                current = ((LoopBeginNode) current).next();
            }
            else if (current instanceof LoopEndNode)
            {
                loopEnd((LoopEndNode) current);
                visitedEnds.mark(current);
                current = nodeQueue.pollFirst();
            }
            else if (current instanceof AbstractMergeNode)
            {
                merge((AbstractMergeNode) current);
                current = ((AbstractMergeNode) current).next();
            }
            else if (current instanceof FixedWithNextNode)
            {
                node(current);
                current = ((FixedWithNextNode) current).next();
            }
            else if (current instanceof EndNode)
            {
                end((EndNode) current);
                queueMerge((EndNode) current);
                current = nodeQueue.pollFirst();
            }
            else if (current instanceof ControlSinkNode)
            {
                node(current);
                current = nodeQueue.pollFirst();
            }
            else if (current instanceof ControlSplitNode)
            {
                controlSplit((ControlSplitNode) current);
                for (Node node : current.successors())
                {
                    nodeQueue.addFirst((AbstractBeginNode) node);
                }
                current = nodeQueue.pollFirst();
            }
        } while (current != null);
        finished();
    }

    private void queueMerge(EndNode end)
    {
        visitedEnds.mark(end);
        AbstractMergeNode merge = end.merge();
        boolean endsVisited = true;
        for (int i = 0; i < merge.forwardEndCount(); i++)
        {
            if (!visitedEnds.isMarked(merge.forwardEndAt(i)))
            {
                endsVisited = false;
                break;
            }
        }
        if (endsVisited)
        {
            nodeQueue.add(merge);
        }
    }

    protected void node(@SuppressWarnings("unused") FixedNode node)
    {
        // empty default implementation
    }

    protected void end(EndNode endNode)
    {
        node(endNode);
    }

    protected void merge(AbstractMergeNode merge)
    {
        node(merge);
    }

    protected void loopBegin(LoopBeginNode loopBegin)
    {
        node(loopBegin);
    }

    protected void loopEnd(LoopEndNode loopEnd)
    {
        node(loopEnd);
    }

    protected void controlSplit(ControlSplitNode controlSplit)
    {
        node(controlSplit);
    }

    protected void finished()
    {
        // nothing to do
    }
}
