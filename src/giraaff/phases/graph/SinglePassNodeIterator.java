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

///
// A SinglePassNodeIterator iterates the fixed nodes of the graph in post order starting from its
// start node. Unlike in iterative dataflow analysis, a single pass is performed, which allows
// keeping a smaller working set of pending {@link MergeableState}. This iteration scheme requires:
//
// <li>{@link MergeableState#merge(AbstractMergeNode, List)} to always return <code>true</code> (an
// assertion checks this)</li>
// <li>{@link #controlSplit(ControlSplitNode)} to always return all successors (otherwise, not all
// associated {@link EndNode} will be visited. In turn, visiting all the end nodes for a given
// {@link AbstractMergeNode} is a precondition before that merge node can be visited)</li>
//
// For this iterator the CFG is defined by the classical CFG nodes (
// {@link giraaff.nodes.ControlSplitNode},
// {@link giraaff.nodes.AbstractMergeNode} ...) and the
// {@link giraaff.nodes.FixedWithNextNode#next() next} pointers of
// {@link giraaff.nodes.FixedWithNextNode}.
//
// The lifecycle that single-pass node iterators go through is described in {@link #apply()}
//
// @param <T> the type of {@link MergeableState} handled by this SinglePassNodeIterator
///
// @class SinglePassNodeIterator
public abstract class SinglePassNodeIterator<T extends MergeableState<T>>
{
    // @field
    private final NodeBitMap ___visitedEnds;

    ///
    // @see SinglePassNodeIterator.PathStart
    ///
    // @field
    private final Deque<PathStart<T>> ___nodeQueue;

    ///
    // The keys in this map may be:
    //
    // <li>loop-begins and loop-ends, see {@link #finishLoopEnds(LoopEndNode)}</li>
    // <li>forward-ends of merge-nodes, see {@link #queueMerge(EndNode)}</li>
    //
    // It's tricky to answer whether the state an entry contains is the pre-state or the post-state
    // for the key in question, because states are mutable. Thus an entry may be created to contain
    // a pre-state (at the time, as done for a loop-begin in {@link #apply()}) only to make it a
    // post-state soon after (continuing with the loop-begin example, also in {@link #apply()}). In
    // any case, given that keys are limited to the nodes mentioned in the previous paragraph, in
    // all cases an entry can be considered to hold a post-state by the time such entry is retrieved.
    //
    // The only method that makes this map grow is {@link #keepForLater(FixedNode, MergeableState)}
    // and the only one that shrinks it is {@link #pruneEntry(FixedNode)}. To make sure no entry is
    // left behind inadvertently, asserts in {@link #finished()} are in place.
    ///
    // @field
    private final EconomicMap<FixedNode, T> ___nodeStates;

    // @field
    private final StartNode ___start;

    // @field
    protected T ___state;

    ///
    // An item queued in {@link #nodeQueue} can be used to continue with the single-pass visit after
    // the previous path can't be followed anymore. Such items are:
    //
    // <li>de-queued via {@link #nextQueuedNode()}</li>
    // <li>en-queued via {@link #queueMerge(EndNode)} and {@link #queueSuccessors(FixedNode)}</li>
    //
    // Correspondingly each item may stand for:
    //
    // <li>a {@link AbstractMergeNode} whose pre-state results from merging those of its
    // forward-ends, see {@link #nextQueuedNode()}</li>
    // <li>a successor of a control-split node, in which case the state on entry to it (the
    // successor) is also stored in the item, see {@link #nextQueuedNode()}</li>
    ///
    // @class SinglePassNodeIterator.PathStart
    private static final class PathStart<U>
    {
        // @field
        private final AbstractBeginNode ___node;
        // @field
        private final U ___stateOnEntry;

        // @cons
        private PathStart(AbstractBeginNode __node, U __stateOnEntry)
        {
            super();
            this.___node = __node;
            this.___stateOnEntry = __stateOnEntry;
        }

        ///
        // @return true iff this instance is internally consistent (ie, its "representation is OK")
        ///
        private boolean repOK()
        {
            if (this.___node == null)
            {
                return false;
            }
            if (this.___node instanceof AbstractMergeNode)
            {
                return this.___stateOnEntry == null;
            }
            return (this.___stateOnEntry != null);
        }
    }

    // @cons
    public SinglePassNodeIterator(StartNode __start, T __initialState)
    {
        super();
        StructuredGraph __graph = __start.graph();
        this.___visitedEnds = __graph.createNodeBitMap();
        this.___nodeQueue = new ArrayDeque<>();
        this.___nodeStates = EconomicMap.create(Equivalence.IDENTITY);
        this.___start = __start;
        this.___state = __initialState;
    }

    ///
    // Performs a single-pass iteration.
    //
    // After this method has been invoked, the {@link SinglePassNodeIterator} instance can't be used
    // again. This saves clearing up fields in {@link #finished()}, the assumption being that this
    // instance will be garbage-collected soon afterwards.
    ///
    public void apply()
    {
        FixedNode __current = this.___start;

        do
        {
            if (__current instanceof InvokeWithExceptionNode)
            {
                invoke((Invoke) __current);
                queueSuccessors(__current);
                __current = nextQueuedNode();
            }
            else if (__current instanceof LoopBeginNode)
            {
                this.___state.loopBegin((LoopBeginNode) __current);
                keepForLater(__current, this.___state);
                this.___state = this.___state.clone();
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
                controlSplit((ControlSplitNode) __current);
                queueSuccessors(__current);
                __current = nextQueuedNode();
            }
        } while (__current != null);
        finished();
    }

    ///
    // Two methods enqueue items in {@link #nodeQueue}. Of them, only this method enqueues items
    // with non-null state (the other method being {@link #queueMerge(EndNode)}).
    //
    // A space optimization is made: the state is cloned for all successors except the first. Given
    // that right after invoking this method, {@link #nextQueuedNode()} is invoked, that single
    // non-cloned state instance is in effect "handed over" to its next owner (thus realizing an
    // owner-is-mutator access protocol).
    ///
    private void queueSuccessors(FixedNode __x)
    {
        T __startState = this.___state;
        T __curState = __startState;
        for (Node __succ : __x.successors())
        {
            if (__succ != null)
            {
                if (__curState == null)
                {
                    // the current state isn't cloned for the first successor
                    // conceptually, the state is handed over to it
                    __curState = __startState.clone();
                }
                AbstractBeginNode __begin = (AbstractBeginNode) __succ;
                this.___nodeQueue.addFirst(new PathStart<>(__begin, __curState));
            }
        }
    }

    ///
    // This method is invoked upon not having a (single) next {@link FixedNode} to visit. This
    // method picks such next-node-to-visit from {@link #nodeQueue} and updates {@link #state} with
    // the pre-state for that node.
    //
    // Upon reaching a {@link AbstractMergeNode}, some entries are pruned from {@link #nodeStates}
    // (ie, the entries associated to forward-ends for that merge-node).
    ///
    private FixedNode nextQueuedNode()
    {
        if (this.___nodeQueue.isEmpty())
        {
            return null;
        }
        PathStart<T> __elem = this.___nodeQueue.removeFirst();
        if (__elem.___node instanceof AbstractMergeNode)
        {
            AbstractMergeNode __merge = (AbstractMergeNode) __elem.___node;
            this.___state = pruneEntry(__merge.forwardEndAt(0));
            ArrayList<T> __states = new ArrayList<>(__merge.forwardEndCount() - 1);
            for (int __i = 1; __i < __merge.forwardEndCount(); __i++)
            {
                T __other = pruneEntry(__merge.forwardEndAt(__i));
                __states.add(__other);
            }
            boolean __ready = this.___state.merge(__merge, __states);
            return __merge;
        }
        else
        {
            AbstractBeginNode __begin = __elem.___node;
            this.___state = __elem.___stateOnEntry;
            this.___state.afterSplit(__begin);
            return __begin;
        }
    }

    ///
    // Once all loop-end-nodes for a given loop-node have been visited.
    //
    // <li>the state for that loop-node is updated based on the states of the loop-end-nodes</li>
    // <li>entries in {@link #nodeStates} are pruned for the loop (they aren't going to be looked up again, anyway)</li>
    //
    // The entries removed by this method were inserted:
    //
    // <li>for the loop-begin, by {@link #apply()}</li>
    // <li>for loop-ends, by (previous) invocations of this method</li>
    ///
    private void finishLoopEnds(LoopEndNode __end)
    {
        this.___visitedEnds.mark(__end);
        keepForLater(__end, this.___state);
        LoopBeginNode __begin = __end.loopBegin();
        boolean __endsVisited = true;
        for (LoopEndNode __le : __begin.loopEnds())
        {
            if (!this.___visitedEnds.isMarked(__le))
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
                T __leState = pruneEntry(__le);
                __states.add(__leState);
            }
            T __loopBeginState = pruneEntry(__begin);
            __loopBeginState.loopEnds(__begin, __states);
        }
    }

    ///
    // Once all end-nodes for a given merge-node have been visited, that merge-node is added to the
    // {@link #nodeQueue}
    //
    // {@link #nextQueuedNode()} is in charge of pruning entries (held by {@link #nodeStates}) for
    // the forward-ends inserted by this method.
    ///
    private void queueMerge(EndNode __end)
    {
        this.___visitedEnds.mark(__end);
        keepForLater(__end, this.___state);
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
            this.___nodeQueue.add(new PathStart<>(__merge, null));
        }
    }

    protected abstract void node(FixedNode __node);

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

    protected void invoke(Invoke __invoke)
    {
        node(__invoke.asNode());
    }

    ///
    // The lifecycle that single-pass node iterators go through is described in {@link #apply()}
    //
    // When overriding this method don't forget to invoke this implementation, otherwise the
    // assertions will be skipped.
    ///
    protected void finished()
    {
    }

    private void keepForLater(FixedNode __x, T __s)
    {
        this.___nodeStates.put(__x, __s);
    }

    private T pruneEntry(FixedNode __x)
    {
        return this.___nodeStates.removeKey(__x);
    }
}
