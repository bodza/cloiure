package giraaff.phases.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;

import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;

// @class ReentrantNodeIterator
public final class ReentrantNodeIterator
{
    // @cons
    private ReentrantNodeIterator()
    {
        super();
    }

    // @class ReentrantNodeIterator.LoopInfo
    public static final class LoopInfo<StateT>
    {
        // @field
        public final EconomicMap<LoopEndNode, StateT> endStates;
        // @field
        public final EconomicMap<LoopExitNode, StateT> exitStates;

        // @cons
        public LoopInfo(int __endCount, int __exitCount)
        {
            super();
            endStates = EconomicMap.create(Equivalence.IDENTITY, __endCount);
            exitStates = EconomicMap.create(Equivalence.IDENTITY, __exitCount);
        }
    }

    // @class ReentrantNodeIterator.NodeIteratorClosure
    public abstract static class NodeIteratorClosure<StateT>
    {
        protected abstract StateT processNode(FixedNode node, StateT currentState);

        protected abstract StateT merge(AbstractMergeNode merge, List<StateT> states);

        protected abstract StateT afterSplit(AbstractBeginNode node, StateT oldState);

        protected abstract EconomicMap<LoopExitNode, StateT> processLoop(LoopBeginNode loop, StateT initialState);

        /**
         * Determine whether iteration should continue in the current state.
         */
        protected boolean continueIteration(StateT __currentState)
        {
            return true;
        }
    }

    public static <StateT> LoopInfo<StateT> processLoop(NodeIteratorClosure<StateT> __closure, LoopBeginNode __loop, StateT __initialState)
    {
        EconomicMap<FixedNode, StateT> __blockEndStates = apply(__closure, __loop, __initialState, __loop);

        LoopInfo<StateT> __info = new LoopInfo<>(__loop.loopEnds().count(), __loop.loopExits().count());
        for (LoopEndNode __end : __loop.loopEnds())
        {
            if (__blockEndStates.containsKey(__end))
            {
                __info.endStates.put(__end, __blockEndStates.get(__end));
            }
        }
        for (LoopExitNode __exit : __loop.loopExits())
        {
            if (__blockEndStates.containsKey(__exit))
            {
                __info.exitStates.put(__exit, __blockEndStates.get(__exit));
            }
        }
        return __info;
    }

    public static <StateT> void apply(NodeIteratorClosure<StateT> __closure, FixedNode __start, StateT __initialState)
    {
        apply(__closure, __start, __initialState, null);
    }

    private static <StateT> EconomicMap<FixedNode, StateT> apply(NodeIteratorClosure<StateT> __closure, FixedNode __start, StateT __initialState, LoopBeginNode __boundary)
    {
        Deque<AbstractBeginNode> __nodeQueue = new ArrayDeque<>();
        EconomicMap<FixedNode, StateT> __blockEndStates = EconomicMap.create(Equivalence.IDENTITY);

        StateT __state = __initialState;
        FixedNode __current = __start;
        do
        {
            while (__current instanceof FixedWithNextNode)
            {
                if (__boundary != null && __current instanceof LoopExitNode && ((LoopExitNode) __current).loopBegin() == __boundary)
                {
                    __blockEndStates.put(__current, __state);
                    __current = null;
                }
                else
                {
                    FixedNode __next = ((FixedWithNextNode) __current).next();
                    __state = __closure.processNode(__current, __state);
                    __current = __closure.continueIteration(__state) ? __next : null;
                }
            }

            if (__current != null)
            {
                __state = __closure.processNode(__current, __state);

                if (__closure.continueIteration(__state))
                {
                    Iterator<Node> __successors = __current.successors().iterator();
                    if (!__successors.hasNext())
                    {
                        if (__current instanceof LoopEndNode)
                        {
                            __blockEndStates.put(__current, __state);
                        }
                        else if (__current instanceof EndNode)
                        {
                            // add the end node and see if the merge is ready for processing
                            AbstractMergeNode __merge = ((EndNode) __current).merge();
                            if (__merge instanceof LoopBeginNode)
                            {
                                EconomicMap<LoopExitNode, StateT> __loopExitState = __closure.processLoop((LoopBeginNode) __merge, __state);
                                MapCursor<LoopExitNode, StateT> __entry = __loopExitState.getEntries();
                                while (__entry.advance())
                                {
                                    __blockEndStates.put(__entry.getKey(), __entry.getValue());
                                    __nodeQueue.add(__entry.getKey());
                                }
                            }
                            else
                            {
                                boolean __endsVisited = true;
                                for (AbstractEndNode __forwardEnd : __merge.forwardEnds())
                                {
                                    if (__forwardEnd != __current && !__blockEndStates.containsKey(__forwardEnd))
                                    {
                                        __endsVisited = false;
                                        break;
                                    }
                                }
                                if (__endsVisited)
                                {
                                    ArrayList<StateT> __states = new ArrayList<>(__merge.forwardEndCount());
                                    for (int __i = 0; __i < __merge.forwardEndCount(); __i++)
                                    {
                                        AbstractEndNode __forwardEnd = __merge.forwardEndAt(__i);
                                        StateT __other = __forwardEnd == __current ? __state : __blockEndStates.removeKey(__forwardEnd);
                                        __states.add(__other);
                                    }
                                    __state = __closure.merge(__merge, __states);
                                    __current = __closure.continueIteration(__state) ? __merge : null;
                                    continue;
                                }
                                else
                                {
                                    __blockEndStates.put(__current, __state);
                                }
                            }
                        }
                    }
                    else
                    {
                        FixedNode __firstSuccessor = (FixedNode) __successors.next();
                        if (!__successors.hasNext())
                        {
                            __current = __firstSuccessor;
                            continue;
                        }
                        else
                        {
                            do
                            {
                                AbstractBeginNode __successor = (AbstractBeginNode) __successors.next();
                                StateT __successorState = __closure.afterSplit(__successor, __state);
                                if (__closure.continueIteration(__successorState))
                                {
                                    __blockEndStates.put(__successor, __successorState);
                                    __nodeQueue.add(__successor);
                                }
                            } while (__successors.hasNext());

                            __state = __closure.afterSplit((AbstractBeginNode) __firstSuccessor, __state);
                            __current = __closure.continueIteration(__state) ? __firstSuccessor : null;
                            continue;
                        }
                    }
                }
            }

            // get next queued block
            if (__nodeQueue.isEmpty())
            {
                return __blockEndStates;
            }
            else
            {
                __current = __nodeQueue.removeFirst();
                __state = __blockEndStates.removeKey(__current);
            }
        } while (true);
    }
}
