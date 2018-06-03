package giraaff.phases.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.StructuredGraph;

/**
 * A PostOrderNodeIterator iterates the fixed nodes of the graph in post order starting from a
 * specified fixed node.
 *
 * For this iterator the CFG is defined by the classical CFG nodes ({@link ControlSplitNode},
 * {@link AbstractMergeNode}...) and the {@link FixedWithNextNode#next() next} pointers of
 * {@link FixedWithNextNode}.
 *
 * While iterating it maintains a user-defined state by calling the methods available in
 * {@link MergeableState}.
 *
 * @param <T> the type of {@link MergeableState} handled by this PostOrderNodeIterator
 */
// @class PostOrderNodeIterator
public abstract class PostOrderNodeIterator<T extends MergeableState<T>>
{
    // @field
    private final NodeBitMap visitedEnds;
    // @field
    private final Deque<AbstractBeginNode> nodeQueue;
    // @field
    private final EconomicMap<FixedNode, T> nodeStates;
    // @field
    private final FixedNode start;

    // @field
    protected T state;

    // @cons
    public PostOrderNodeIterator(FixedNode __start, T __initialState)
    {
        super();
        StructuredGraph __graph = __start.graph();
        visitedEnds = __graph.createNodeBitMap();
        nodeQueue = new ArrayDeque<>();
        nodeStates = EconomicMap.create(Equivalence.IDENTITY);
        this.start = __start;
        this.state = __initialState;
    }

    public void apply()
    {
        FixedNode __current = start;

        do
        {
            if (__current instanceof InvokeWithExceptionNode)
            {
                invoke((Invoke) __current);
                queueSuccessors(__current, null);
                __current = nextQueuedNode();
            }
            else if (__current instanceof LoopBeginNode)
            {
                state.loopBegin((LoopBeginNode) __current);
                nodeStates.put(__current, state);
                state = state.clone();
                loopBegin((LoopBeginNode) __current);
                __current = ((LoopBeginNode) __current).next();
            }
            else if (__current instanceof LoopEndNode)
            {
                loopEnd((LoopEndNode) __current);
                finishLoopEnds((LoopEndNode) __current);
                __current = nextQueuedNode();
            }
            else if (__current instanceof AbstractMergeNode)
            {
                merge((AbstractMergeNode) __current);
                __current = ((AbstractMergeNode) __current).next();
            }
            else if (__current instanceof FixedWithNextNode)
            {
                FixedNode __next = ((FixedWithNextNode) __current).next();
                node(__current);
                __current = __next;
            }
            else if (__current instanceof EndNode)
            {
                end((EndNode) __current);
                queueMerge((EndNode) __current);
                __current = nextQueuedNode();
            }
            else if (__current instanceof ControlSinkNode)
            {
                node(__current);
                __current = nextQueuedNode();
            }
            else if (__current instanceof ControlSplitNode)
            {
                Set<Node> __successors = controlSplit((ControlSplitNode) __current);
                queueSuccessors(__current, __successors);
                __current = nextQueuedNode();
            }
        } while (__current != null);
        finished();
    }

    private void queueSuccessors(FixedNode __x, Set<Node> __successors)
    {
        nodeStates.put(__x, state);
        if (__successors != null)
        {
            for (Node __node : __successors)
            {
                if (__node != null)
                {
                    nodeStates.put((FixedNode) __node.predecessor(), state);
                    nodeQueue.addFirst((AbstractBeginNode) __node);
                }
            }
        }
        else
        {
            for (Node __node : __x.successors())
            {
                if (__node != null)
                {
                    nodeQueue.addFirst((AbstractBeginNode) __node);
                }
            }
        }
    }

    private FixedNode nextQueuedNode()
    {
        int __maxIterations = nodeQueue.size();
        while (__maxIterations-- > 0)
        {
            AbstractBeginNode __node = nodeQueue.removeFirst();
            if (__node instanceof AbstractMergeNode)
            {
                AbstractMergeNode __merge = (AbstractMergeNode) __node;
                state = nodeStates.get(__merge.forwardEndAt(0)).clone();
                ArrayList<T> __states = new ArrayList<>(__merge.forwardEndCount() - 1);
                for (int __i = 1; __i < __merge.forwardEndCount(); __i++)
                {
                    T __other = nodeStates.get(__merge.forwardEndAt(__i));
                    __states.add(__other);
                }
                boolean __ready = state.merge(__merge, __states);
                if (__ready)
                {
                    return __merge;
                }
                else
                {
                    nodeQueue.addLast(__merge);
                }
            }
            else
            {
                state = nodeStates.get((FixedNode) __node.predecessor()).clone();
                state.afterSplit(__node);
                return __node;
            }
        }
        return null;
    }

    private void finishLoopEnds(LoopEndNode __end)
    {
        nodeStates.put(__end, state);
        visitedEnds.mark(__end);
        LoopBeginNode __begin = __end.loopBegin();
        boolean __endsVisited = true;
        for (LoopEndNode __le : __begin.loopEnds())
        {
            if (!visitedEnds.isMarked(__le))
            {
                __endsVisited = false;
                break;
            }
        }
        if (__endsVisited)
        {
            ArrayList<T> __states = new ArrayList<>(__begin.loopEnds().count());
            for (LoopEndNode __le : __begin.orderedLoopEnds())
            {
                __states.add(nodeStates.get(__le));
            }
            T __loopBeginState = nodeStates.get(__begin);
            if (__loopBeginState != null)
            {
                __loopBeginState.loopEnds(__begin, __states);
            }
        }
    }

    private void queueMerge(EndNode __end)
    {
        nodeStates.put(__end, state);
        visitedEnds.mark(__end);
        AbstractMergeNode __merge = __end.merge();
        boolean __endsVisited = true;
        for (int __i = 0; __i < __merge.forwardEndCount(); __i++)
        {
            if (!visitedEnds.isMarked(__merge.forwardEndAt(__i)))
            {
                __endsVisited = false;
                break;
            }
        }
        if (__endsVisited)
        {
            nodeQueue.add(__merge);
        }
    }

    protected abstract void node(FixedNode node);

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

    protected Set<Node> controlSplit(ControlSplitNode __controlSplit)
    {
        node(__controlSplit);
        return null;
    }

    protected void invoke(Invoke __invoke)
    {
        node(__invoke.asNode());
    }

    protected void finished()
    {
        // nothing to do
    }
}
