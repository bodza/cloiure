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
    // @class ReentrantNodeIterator.LoopInfo
    public static final class LoopInfo<StateT>
    {
        public final EconomicMap<LoopEndNode, StateT> endStates;
        public final EconomicMap<LoopExitNode, StateT> exitStates;

        // @cons
        public LoopInfo(int endCount, int exitCount)
        {
            super();
            endStates = EconomicMap.create(Equivalence.IDENTITY, endCount);
            exitStates = EconomicMap.create(Equivalence.IDENTITY, exitCount);
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
        protected boolean continueIteration(StateT currentState)
        {
            return true;
        }
    }

    // @cons
    private ReentrantNodeIterator()
    {
        super();
        // no instances allowed
    }

    public static <StateT> LoopInfo<StateT> processLoop(NodeIteratorClosure<StateT> closure, LoopBeginNode loop, StateT initialState)
    {
        EconomicMap<FixedNode, StateT> blockEndStates = apply(closure, loop, initialState, loop);

        LoopInfo<StateT> info = new LoopInfo<>(loop.loopEnds().count(), loop.loopExits().count());
        for (LoopEndNode end : loop.loopEnds())
        {
            if (blockEndStates.containsKey(end))
            {
                info.endStates.put(end, blockEndStates.get(end));
            }
        }
        for (LoopExitNode exit : loop.loopExits())
        {
            if (blockEndStates.containsKey(exit))
            {
                info.exitStates.put(exit, blockEndStates.get(exit));
            }
        }
        return info;
    }

    public static <StateT> void apply(NodeIteratorClosure<StateT> closure, FixedNode start, StateT initialState)
    {
        apply(closure, start, initialState, null);
    }

    private static <StateT> EconomicMap<FixedNode, StateT> apply(NodeIteratorClosure<StateT> closure, FixedNode start, StateT initialState, LoopBeginNode boundary)
    {
        Deque<AbstractBeginNode> nodeQueue = new ArrayDeque<>();
        EconomicMap<FixedNode, StateT> blockEndStates = EconomicMap.create(Equivalence.IDENTITY);

        StateT state = initialState;
        FixedNode current = start;
        do
        {
            while (current instanceof FixedWithNextNode)
            {
                if (boundary != null && current instanceof LoopExitNode && ((LoopExitNode) current).loopBegin() == boundary)
                {
                    blockEndStates.put(current, state);
                    current = null;
                }
                else
                {
                    FixedNode next = ((FixedWithNextNode) current).next();
                    state = closure.processNode(current, state);
                    current = closure.continueIteration(state) ? next : null;
                }
            }

            if (current != null)
            {
                state = closure.processNode(current, state);

                if (closure.continueIteration(state))
                {
                    Iterator<Node> successors = current.successors().iterator();
                    if (!successors.hasNext())
                    {
                        if (current instanceof LoopEndNode)
                        {
                            blockEndStates.put(current, state);
                        }
                        else if (current instanceof EndNode)
                        {
                            // add the end node and see if the merge is ready for processing
                            AbstractMergeNode merge = ((EndNode) current).merge();
                            if (merge instanceof LoopBeginNode)
                            {
                                EconomicMap<LoopExitNode, StateT> loopExitState = closure.processLoop((LoopBeginNode) merge, state);
                                MapCursor<LoopExitNode, StateT> entry = loopExitState.getEntries();
                                while (entry.advance())
                                {
                                    blockEndStates.put(entry.getKey(), entry.getValue());
                                    nodeQueue.add(entry.getKey());
                                }
                            }
                            else
                            {
                                boolean endsVisited = true;
                                for (AbstractEndNode forwardEnd : merge.forwardEnds())
                                {
                                    if (forwardEnd != current && !blockEndStates.containsKey(forwardEnd))
                                    {
                                        endsVisited = false;
                                        break;
                                    }
                                }
                                if (endsVisited)
                                {
                                    ArrayList<StateT> states = new ArrayList<>(merge.forwardEndCount());
                                    for (int i = 0; i < merge.forwardEndCount(); i++)
                                    {
                                        AbstractEndNode forwardEnd = merge.forwardEndAt(i);
                                        StateT other = forwardEnd == current ? state : blockEndStates.removeKey(forwardEnd);
                                        states.add(other);
                                    }
                                    state = closure.merge(merge, states);
                                    current = closure.continueIteration(state) ? merge : null;
                                    continue;
                                }
                                else
                                {
                                    blockEndStates.put(current, state);
                                }
                            }
                        }
                    }
                    else
                    {
                        FixedNode firstSuccessor = (FixedNode) successors.next();
                        if (!successors.hasNext())
                        {
                            current = firstSuccessor;
                            continue;
                        }
                        else
                        {
                            do
                            {
                                AbstractBeginNode successor = (AbstractBeginNode) successors.next();
                                StateT successorState = closure.afterSplit(successor, state);
                                if (closure.continueIteration(successorState))
                                {
                                    blockEndStates.put(successor, successorState);
                                    nodeQueue.add(successor);
                                }
                            } while (successors.hasNext());

                            state = closure.afterSplit((AbstractBeginNode) firstSuccessor, state);
                            current = closure.continueIteration(state) ? firstSuccessor : null;
                            continue;
                        }
                    }
                }
            }

            // get next queued block
            if (nodeQueue.isEmpty())
            {
                return blockEndStates;
            }
            else
            {
                current = nodeQueue.removeFirst();
                state = blockEndStates.removeKey(current);
            }
        } while (true);
    }
}
