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

///
// This iterator implements a reverse post order iteration over the fixed nodes in the graph,
// starting at the given fixed node.
//
// No changes to the fixed nodes are expected during the iteration, they cause undefined behavior.
///
// @class StatelessPostOrderNodeIterator
public abstract class StatelessPostOrderNodeIterator
{
    // @field
    private final NodeBitMap ___visitedEnds;
    // @field
    private final Deque<AbstractBeginNode> ___nodeQueue;
    // @field
    private final FixedNode ___start;

    // @cons StatelessPostOrderNodeIterator
    public StatelessPostOrderNodeIterator(FixedNode __start)
    {
        super();
        this.___visitedEnds = __start.graph().createNodeBitMap();
        this.___nodeQueue = new ArrayDeque<>();
        this.___start = __start;
    }

    public void apply()
    {
        FixedNode __current = this.___start;

        do
        {
            if (__current instanceof LoopBeginNode)
            {
                loopBegin((LoopBeginNode) __current);
                __current = ((LoopBeginNode) __current).next();
            }
            else if (__current instanceof LoopEndNode)
            {
                loopEnd((LoopEndNode) __current);
                this.___visitedEnds.mark(__current);
                __current = this.___nodeQueue.pollFirst();
            }
            else if (__current instanceof AbstractMergeNode)
            {
                merge((AbstractMergeNode) __current);
                __current = ((AbstractMergeNode) __current).next();
            }
            else if (__current instanceof FixedWithNextNode)
            {
                node(__current);
                __current = ((FixedWithNextNode) __current).next();
            }
            else if (__current instanceof EndNode)
            {
                end((EndNode) __current);
                queueMerge((EndNode) __current);
                __current = this.___nodeQueue.pollFirst();
            }
            else if (__current instanceof ControlSinkNode)
            {
                node(__current);
                __current = this.___nodeQueue.pollFirst();
            }
            else if (__current instanceof ControlSplitNode)
            {
                controlSplit((ControlSplitNode) __current);
                for (Node __node : __current.successors())
                {
                    this.___nodeQueue.addFirst((AbstractBeginNode) __node);
                }
                __current = this.___nodeQueue.pollFirst();
            }
        } while (__current != null);
        finished();
    }

    private void queueMerge(EndNode __end)
    {
        this.___visitedEnds.mark(__end);
        AbstractMergeNode __merge = __end.merge();
        boolean __endsVisited = true;
        for (int __i = 0; __i < __merge.forwardEndCount(); __i++)
        {
            if (!this.___visitedEnds.isMarked(__merge.forwardEndAt(__i)))
            {
                __endsVisited = false;
                break;
            }
        }
        if (__endsVisited)
        {
            this.___nodeQueue.add(__merge);
        }
    }

    protected void node(@SuppressWarnings("unused") FixedNode __node)
    {
        // empty default implementation
    }

    protected void end(EndNode __endNode)
    {
        node(__endNode);
    }

    protected void merge(AbstractMergeNode __merge)
    {
        node(__merge);
    }

    protected void loopBegin(LoopBeginNode __loopBegin)
    {
        node(__loopBegin);
    }

    protected void loopEnd(LoopEndNode __loopEnd)
    {
        node(__loopEnd);
    }

    protected void controlSplit(ControlSplitNode __controlSplit)
    {
        node(__controlSplit);
    }

    protected void finished()
    {
        // nothing to do
    }
}
