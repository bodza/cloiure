package giraaff.phases.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
import giraaff.nodes.StartNode;
import giraaff.nodes.StructuredGraph;

/**
 * A SinglePassNodeIterator iterates the fixed nodes of the graph in post order starting from its
 * start node. Unlike in iterative dataflow analysis, a single pass is performed, which allows
 * keeping a smaller working set of pending {@link MergeableState}. This iteration scheme requires:
 * <ul>
 * <li>{@link MergeableState#merge(AbstractMergeNode, List)} to always return <code>true</code> (an
 * assertion checks this)</li>
 * <li>{@link #controlSplit(ControlSplitNode)} to always return all successors (otherwise, not all
 * associated {@link EndNode} will be visited. In turn, visiting all the end nodes for a given
 * {@link AbstractMergeNode} is a precondition before that merge node can be visited)</li>
 * </ul>
 *
 * For this iterator the CFG is defined by the classical CFG nodes (
 * {@link giraaff.nodes.ControlSplitNode},
 * {@link giraaff.nodes.AbstractMergeNode} ...) and the
 * {@link giraaff.nodes.FixedWithNextNode#next() next} pointers of
 * {@link giraaff.nodes.FixedWithNextNode}.
 *
 * The lifecycle that single-pass node iterators go through is described in {@link #apply()}
 *
 * @param <T> the type of {@link MergeableState} handled by this SinglePassNodeIterator
 */
public abstract class SinglePassNodeIterator<T extends MergeableState<T>>
{
    private final NodeBitMap visitedEnds;

    /**
     * @see SinglePassNodeIterator.PathStart
     */
    private final Deque<PathStart<T>> nodeQueue;

    /**
     * The keys in this map may be:
     *
     * <li>loop-begins and loop-ends, see {@link #finishLoopEnds(LoopEndNode)}</li>
     * <li>forward-ends of merge-nodes, see {@link #queueMerge(EndNode)}</li>
     *
     * It's tricky to answer whether the state an entry contains is the pre-state or the post-state
     * for the key in question, because states are mutable. Thus an entry may be created to contain
     * a pre-state (at the time, as done for a loop-begin in {@link #apply()}) only to make it a
     * post-state soon after (continuing with the loop-begin example, also in {@link #apply()}). In
     * any case, given that keys are limited to the nodes mentioned in the previous paragraph, in
     * all cases an entry can be considered to hold a post-state by the time such entry is
     * retrieved.
     *
     * The only method that makes this map grow is {@link #keepForLater(FixedNode, MergeableState)}
     * and the only one that shrinks it is {@link #pruneEntry(FixedNode)}. To make sure no entry is
     * left behind inadvertently, asserts in {@link #finished()} are in place.
     */
    private final EconomicMap<FixedNode, T> nodeStates;

    private final StartNode start;

    protected T state;

    /**
     * An item queued in {@link #nodeQueue} can be used to continue with the single-pass visit after
     * the previous path can't be followed anymore. Such items are:
     *
     * <li>de-queued via {@link #nextQueuedNode()}</li>
     * <li>en-queued via {@link #queueMerge(EndNode)} and {@link #queueSuccessors(FixedNode)}</li>
     *
     * Correspondingly each item may stand for:
     *
     * <li>a {@link AbstractMergeNode} whose pre-state results from merging those of its
     * forward-ends, see {@link #nextQueuedNode()}</li>
     * <li>a successor of a control-split node, in which case the state on entry to it (the
     * successor) is also stored in the item, see {@link #nextQueuedNode()}</li>
     */
    private static final class PathStart<U>
    {
        private final AbstractBeginNode node;
        private final U stateOnEntry;

        private PathStart(AbstractBeginNode node, U stateOnEntry)
        {
            this.node = node;
            this.stateOnEntry = stateOnEntry;
        }

        /**
         * @return true iff this instance is internally consistent (ie, its "representation is OK")
         */
        private boolean repOK()
        {
            if (node == null)
            {
                return false;
            }
            if (node instanceof AbstractMergeNode)
            {
                return stateOnEntry == null;
            }
            return (stateOnEntry != null);
        }
    }

    public SinglePassNodeIterator(StartNode start, T initialState)
    {
        StructuredGraph graph = start.graph();
        visitedEnds = graph.createNodeBitMap();
        nodeQueue = new ArrayDeque<>();
        nodeStates = EconomicMap.create(Equivalence.IDENTITY);
        this.start = start;
        this.state = initialState;
    }

    /**
     * Performs a single-pass iteration.
     *
     * After this method has been invoked, the {@link SinglePassNodeIterator} instance can't be used
     * again. This saves clearing up fields in {@link #finished()}, the assumption being that this
     * instance will be garbage-collected soon afterwards.
     */
    public void apply()
    {
        FixedNode current = start;

        do
        {
            if (current instanceof InvokeWithExceptionNode)
            {
                invoke((Invoke) current);
                queueSuccessors(current);
                current = nextQueuedNode();
            }
            else if (current instanceof LoopBeginNode)
            {
                state.loopBegin((LoopBeginNode) current);
                keepForLater(current, state);
                state = state.clone();
                loopBegin((LoopBeginNode) current);
                current = ((LoopBeginNode) current).next();
            }
            else if (current instanceof LoopEndNode)
            {
                loopEnd((LoopEndNode) current);
                finishLoopEnds((LoopEndNode) current);
                current = nextQueuedNode();
            }
            else if (current instanceof AbstractMergeNode)
            {
                merge((AbstractMergeNode) current);
                current = ((AbstractMergeNode) current).next();
            }
            else if (current instanceof FixedWithNextNode)
            {
                FixedNode next = ((FixedWithNextNode) current).next();
                node(current);
                current = next;
            }
            else if (current instanceof EndNode)
            {
                end((EndNode) current);
                queueMerge((EndNode) current);
                current = nextQueuedNode();
            }
            else if (current instanceof ControlSinkNode)
            {
                node(current);
                current = nextQueuedNode();
            }
            else if (current instanceof ControlSplitNode)
            {
                controlSplit((ControlSplitNode) current);
                queueSuccessors(current);
                current = nextQueuedNode();
            }
        } while (current != null);
        finished();
    }

    /**
     * Two methods enqueue items in {@link #nodeQueue}. Of them, only this method enqueues items
     * with non-null state (the other method being {@link #queueMerge(EndNode)}).
     *
     * A space optimization is made: the state is cloned for all successors except the first. Given
     * that right after invoking this method, {@link #nextQueuedNode()} is invoked, that single
     * non-cloned state instance is in effect "handed over" to its next owner (thus realizing an
     * owner-is-mutator access protocol).
     */
    private void queueSuccessors(FixedNode x)
    {
        T startState = state;
        T curState = startState;
        for (Node succ : x.successors())
        {
            if (succ != null)
            {
                if (curState == null)
                {
                    // the current state isn't cloned for the first successor
                    // conceptually, the state is handed over to it
                    curState = startState.clone();
                }
                AbstractBeginNode begin = (AbstractBeginNode) succ;
                nodeQueue.addFirst(new PathStart<>(begin, curState));
            }
        }
    }

    /**
     * This method is invoked upon not having a (single) next {@link FixedNode} to visit. This
     * method picks such next-node-to-visit from {@link #nodeQueue} and updates {@link #state} with
     * the pre-state for that node.
     *
     * Upon reaching a {@link AbstractMergeNode}, some entries are pruned from {@link #nodeStates}
     * (ie, the entries associated to forward-ends for that merge-node).
     */
    private FixedNode nextQueuedNode()
    {
        if (nodeQueue.isEmpty())
        {
            return null;
        }
        PathStart<T> elem = nodeQueue.removeFirst();
        if (elem.node instanceof AbstractMergeNode)
        {
            AbstractMergeNode merge = (AbstractMergeNode) elem.node;
            state = pruneEntry(merge.forwardEndAt(0));
            ArrayList<T> states = new ArrayList<>(merge.forwardEndCount() - 1);
            for (int i = 1; i < merge.forwardEndCount(); i++)
            {
                T other = pruneEntry(merge.forwardEndAt(i));
                states.add(other);
            }
            boolean ready = state.merge(merge, states);
            return merge;
        }
        else
        {
            AbstractBeginNode begin = elem.node;
            state = elem.stateOnEntry;
            state.afterSplit(begin);
            return begin;
        }
    }

    /**
     * Once all loop-end-nodes for a given loop-node have been visited.
     *
     * <li>the state for that loop-node is updated based on the states of the loop-end-nodes</li>
     * <li>entries in {@link #nodeStates} are pruned for the loop (they aren't going to be looked up
     * again, anyway)</li>
     *
     * The entries removed by this method were inserted:
     *
     * <li>for the loop-begin, by {@link #apply()}</li>
     * <li>for loop-ends, by (previous) invocations of this method</li>
     */
    private void finishLoopEnds(LoopEndNode end)
    {
        visitedEnds.mark(end);
        keepForLater(end, state);
        LoopBeginNode begin = end.loopBegin();
        boolean endsVisited = true;
        for (LoopEndNode le : begin.loopEnds())
        {
            if (!visitedEnds.isMarked(le))
            {
                endsVisited = false;
                break;
            }
        }
        if (endsVisited)
        {
            ArrayList<T> states = new ArrayList<>(begin.loopEnds().count());
            for (LoopEndNode le : begin.orderedLoopEnds())
            {
                T leState = pruneEntry(le);
                states.add(leState);
            }
            T loopBeginState = pruneEntry(begin);
            loopBeginState.loopEnds(begin, states);
        }
    }

    /**
     * Once all end-nodes for a given merge-node have been visited, that merge-node is added to the
     * {@link #nodeQueue}
     *
     * {@link #nextQueuedNode()} is in charge of pruning entries (held by {@link #nodeStates}) for
     * the forward-ends inserted by this method.
     */
    private void queueMerge(EndNode end)
    {
        visitedEnds.mark(end);
        keepForLater(end, state);
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
            nodeQueue.add(new PathStart<>(merge, null));
        }
    }

    protected abstract void node(FixedNode node);

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

    protected void invoke(Invoke invoke)
    {
        node(invoke.asNode());
    }

    /**
     * The lifecycle that single-pass node iterators go through is described in {@link #apply()}
     *
     * When overriding this method don't forget to invoke this implementation, otherwise the
     * assertions will be skipped.
     */
    protected void finished()
    {
    }

    private void keepForLater(FixedNode x, T s)
    {
        nodeStates.put(x, s);
    }

    private T pruneEntry(FixedNode x)
    {
        T result = nodeStates.removeKey(x);
        return result;
    }
}
