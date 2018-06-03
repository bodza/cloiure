package giraaff.phases.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.cfg.Loop;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.cfg.Block;

// @class ReentrantBlockIterator
public final class ReentrantBlockIterator
{
    // @cons
    private ReentrantBlockIterator()
    {
        super();
    }

    // @class ReentrantBlockIterator.LoopInfo
    public static final class LoopInfo<StateT>
    {
        // @field
        public final List<StateT> ___endStates;
        // @field
        public final List<StateT> ___exitStates;

        // @cons
        public LoopInfo(int __endCount, int __exitCount)
        {
            super();
            this.___endStates = new ArrayList<>(__endCount);
            this.___exitStates = new ArrayList<>(__exitCount);
        }
    }

    // @class ReentrantBlockIterator.BlockIteratorClosure
    public abstract static class BlockIteratorClosure<StateT>
    {
        protected abstract StateT getInitialState();

        protected abstract StateT processBlock(Block __block, StateT __currentState);

        protected abstract StateT merge(Block __merge, List<StateT> __states);

        protected abstract StateT cloneState(StateT __oldState);

        protected abstract List<StateT> processLoop(Loop<Block> __loop, StateT __initialState);
    }

    public static <StateT> LoopInfo<StateT> processLoop(BlockIteratorClosure<StateT> __closure, Loop<Block> __loop, StateT __initialState)
    {
        EconomicMap<FixedNode, StateT> __blockEndStates = apply(__closure, __loop.getHeader(), __initialState, __block -> !(__block.getLoop() == __loop || __block.isLoopHeader()));

        Block[] __predecessors = __loop.getHeader().getPredecessors();
        LoopInfo<StateT> __info = new LoopInfo<>(__predecessors.length - 1, __loop.getExits().size());
        for (int __i = 1; __i < __predecessors.length; __i++)
        {
            StateT __endState = __blockEndStates.get(__predecessors[__i].getEndNode());
            // make sure all end states are unique objects
            __info.___endStates.add(__closure.cloneState(__endState));
        }
        for (Block __loopExit : __loop.getExits())
        {
            StateT __exitState = __blockEndStates.get(__loopExit.getBeginNode());
            // make sure all exit states are unique objects
            __info.___exitStates.add(__closure.cloneState(__exitState));
        }
        return __info;
    }

    public static <StateT> void apply(BlockIteratorClosure<StateT> __closure, Block __start)
    {
        apply(__closure, __start, __closure.getInitialState(), null);
    }

    public static <StateT> EconomicMap<FixedNode, StateT> apply(BlockIteratorClosure<StateT> __closure, Block __start, StateT __initialState, Predicate<Block> __stopAtBlock)
    {
        Deque<Block> __blockQueue = new ArrayDeque<>();
        // States are stored on EndNodes before merges, and on BeginNodes after ControlSplitNodes.
        EconomicMap<FixedNode, StateT> __states = EconomicMap.create(Equivalence.IDENTITY);

        StateT __state = __initialState;
        Block __current = __start;

        StructuredGraph __graph = __start.getBeginNode().graph();
        while (true)
        {
            Block __next = null;
            if (__stopAtBlock != null && __stopAtBlock.test(__current))
            {
                __states.put(__current.getBeginNode(), __state);
            }
            else
            {
                __state = __closure.processBlock(__current, __state);

                Block[] __successors = __current.getSuccessors();
                if (__successors.length == 0)
                {
                    // nothing to do...
                }
                else if (__successors.length == 1)
                {
                    Block __successor = __successors[0];
                    if (__successor.isLoopHeader())
                    {
                        if (__current.isLoopEnd())
                        {
                            // nothing to do... loop ends only lead to loop begins we've already visited
                            __states.put(__current.getEndNode(), __state);
                        }
                        else
                        {
                            recurseIntoLoop(__closure, __blockQueue, __states, __state, __successor);
                        }
                    }
                    else if (__current.getEndNode() instanceof AbstractEndNode)
                    {
                        AbstractEndNode __end = (AbstractEndNode) __current.getEndNode();

                        // add the end node and see if the merge is ready for processing
                        AbstractMergeNode __merge = __end.merge();
                        if (allEndsVisited(__states, __current, __merge))
                        {
                            ArrayList<StateT> __mergedStates = mergeStates(__states, __state, __current, __successor, __merge);
                            __state = __closure.merge(__successor, __mergedStates);
                            __next = __successor;
                        }
                        else
                        {
                            __states.put(__end, __state);
                        }
                    }
                    else
                    {
                        __next = __successor;
                    }
                }
                else
                {
                    __next = processMultipleSuccessors(__closure, __blockQueue, __states, __state, __successors);
                }
            }

            // get next queued block
            if (__next != null)
            {
                __current = __next;
            }
            else if (__blockQueue.isEmpty())
            {
                return __states;
            }
            else
            {
                __current = __blockQueue.removeFirst();
                __state = __states.removeKey(__current.getBeginNode());
            }
        }
    }

    private static <StateT> boolean allEndsVisited(EconomicMap<FixedNode, StateT> __states, Block __current, AbstractMergeNode __merge)
    {
        for (AbstractEndNode __forwardEnd : __merge.forwardEnds())
        {
            if (__forwardEnd != __current.getEndNode() && !__states.containsKey(__forwardEnd))
            {
                return false;
            }
        }
        return true;
    }

    private static <StateT> Block processMultipleSuccessors(BlockIteratorClosure<StateT> __closure, Deque<Block> __blockQueue, EconomicMap<FixedNode, StateT> __states, StateT __state, Block[] __successors)
    {
        for (int __i = 1; __i < __successors.length; __i++)
        {
            Block __successor = __successors[__i];
            __blockQueue.addFirst(__successor);
            __states.put(__successor.getBeginNode(), __closure.cloneState(__state));
        }
        return __successors[0];
    }

    private static <StateT> ArrayList<StateT> mergeStates(EconomicMap<FixedNode, StateT> __states, StateT __state, Block __current, Block __successor, AbstractMergeNode __merge)
    {
        ArrayList<StateT> __mergedStates = new ArrayList<>(__merge.forwardEndCount());
        for (Block __predecessor : __successor.getPredecessors())
        {
            StateT __endState = __predecessor == __current ? __state : __states.removeKey(__predecessor.getEndNode());
            __mergedStates.add(__endState);
        }
        return __mergedStates;
    }

    private static <StateT> void recurseIntoLoop(BlockIteratorClosure<StateT> __closure, Deque<Block> __blockQueue, EconomicMap<FixedNode, StateT> __states, StateT __state, Block __successor)
    {
        // recurse into the loop
        Loop<Block> __loop = __successor.getLoop();
        LoopBeginNode __loopBegin = (LoopBeginNode) __loop.getHeader().getBeginNode();

        List<StateT> __exitStates = __closure.processLoop(__loop, __state);

        int __i = 0;
        for (Block __exit : __loop.getExits())
        {
            __states.put(__exit.getBeginNode(), __exitStates.get(__i++));
            __blockQueue.addFirst(__exit);
        }
    }
}
