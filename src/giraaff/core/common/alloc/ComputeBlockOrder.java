package giraaff.core.common.alloc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.Loop;

/**
 * Computes an ordering of the block that can be used by the linear scan register allocator and the
 * machine code generator. The machine code generation order will start with the first block and
 * produce a straight sequence always following the most likely successor. Then it will continue
 * with the most likely path that was left out during this process. The process iteratively
 * continues until all blocks are scheduled. Additionally, it is guaranteed that all blocks of a
 * loop are scheduled before any block following the loop is scheduled.
 *
 * The machine code generator order includes reordering of loop headers such that the backward jump
 * is a conditional jump if there is only one loop end block. Additionally, the target of loop
 * backward jumps are always marked as aligned. Aligning the target of conditional jumps does not
 * bring a measurable benefit and is therefore avoided to keep the code size small.
 *
 * The linear scan register allocator order has an additional mechanism that prevents merge nodes
 * from being scheduled if there is at least one highly likely predecessor still unscheduled. This
 * increases the probability that the merge node and the corresponding predecessor are more closely
 * together in the schedule thus decreasing the probability for inserted phi moves. Also, the
 * algorithm sets the linear scan order number of the block that corresponds to its index in the
 * linear scan order.
 */
// @class ComputeBlockOrder
public final class ComputeBlockOrder
{
    /**
     * The initial capacities of the worklists used for iteratively finding the block order.
     */
    // @def
    private static final int INITIAL_WORKLIST_CAPACITY = 10;

    /**
     * Divisor used for degrading the probability of the current path versus unscheduled paths at
     * a merge node when calculating the linear scan order. A high value means that predecessors
     * of merge nodes are more likely to be scheduled before the merge node.
     */
    // @def
    private static final int PENALTY_VERSUS_UNSCHEDULED = 10;

    /**
     * Computes the block order used for the linear scan register allocator.
     *
     * @return sorted list of blocks
     */
    public static <T extends AbstractBlockBase<T>> AbstractBlockBase<?>[] computeLinearScanOrder(int __blockCount, T __startBlock)
    {
        List<T> __order = new ArrayList<>();
        BitSet __visitedBlocks = new BitSet(__blockCount);
        computeLinearScanOrder(__order, initializeWorklist(__startBlock, __visitedBlocks), __visitedBlocks);
        return __order.toArray(new AbstractBlockBase<?>[0]);
    }

    /**
     * Computes the block order used for code emission.
     *
     * @return sorted list of blocks
     */
    public static <T extends AbstractBlockBase<T>> AbstractBlockBase<?>[] computeCodeEmittingOrder(int __blockCount, T __startBlock)
    {
        List<T> __order = new ArrayList<>();
        BitSet __visitedBlocks = new BitSet(__blockCount);
        computeCodeEmittingOrder(__order, initializeWorklist(__startBlock, __visitedBlocks), __visitedBlocks);
        return __order.toArray(new AbstractBlockBase<?>[0]);
    }

    /**
     * Iteratively adds paths to the code emission block order.
     */
    private static <T extends AbstractBlockBase<T>> void computeCodeEmittingOrder(List<T> __order, PriorityQueue<T> __worklist, BitSet __visitedBlocks)
    {
        while (!__worklist.isEmpty())
        {
            addPathToCodeEmittingOrder(__worklist.poll(), __order, __worklist, __visitedBlocks);
        }
    }

    /**
     * Iteratively adds paths to the linear scan block order.
     */
    private static <T extends AbstractBlockBase<T>> void computeLinearScanOrder(List<T> __order, PriorityQueue<T> __worklist, BitSet __visitedBlocks)
    {
        while (!__worklist.isEmpty())
        {
            T __nextImportantPath = __worklist.poll();
            do
            {
                __nextImportantPath = addPathToLinearScanOrder(__nextImportantPath, __order, __worklist, __visitedBlocks);
            } while (__nextImportantPath != null);
        }
    }

    /**
     * Initializes the priority queue used for the work list of blocks and adds the start block.
     */
    private static <T extends AbstractBlockBase<T>> PriorityQueue<T> initializeWorklist(T __startBlock, BitSet __visitedBlocks)
    {
        PriorityQueue<T> __result = new PriorityQueue<>(INITIAL_WORKLIST_CAPACITY, new BlockOrderComparator<>());
        __result.add(__startBlock);
        __visitedBlocks.set(__startBlock.getId());
        return __result;
    }

    /**
     * Add a linear path to the linear scan order greedily following the most likely successor.
     */
    private static <T extends AbstractBlockBase<T>> T addPathToLinearScanOrder(T __block, List<T> __order, PriorityQueue<T> __worklist, BitSet __visitedBlocks)
    {
        __block.setLinearScanNumber(__order.size());
        __order.add(__block);
        T __mostLikelySuccessor = findAndMarkMostLikelySuccessor(__block, __visitedBlocks);
        enqueueSuccessors(__block, __worklist, __visitedBlocks);
        if (__mostLikelySuccessor != null)
        {
            if (!__mostLikelySuccessor.isLoopHeader() && __mostLikelySuccessor.getPredecessorCount() > 1)
            {
                // We are at a merge. Check probabilities of predecessors that are not yet scheduled.
                double __unscheduledSum = 0.0;
                for (T __pred : __mostLikelySuccessor.getPredecessors())
                {
                    if (__pred.getLinearScanNumber() == -1)
                    {
                        __unscheduledSum += __pred.probability();
                    }
                }

                if (__unscheduledSum > __block.probability() / PENALTY_VERSUS_UNSCHEDULED)
                {
                    // Add this merge only after at least one additional predecessor gets scheduled.
                    __visitedBlocks.clear(__mostLikelySuccessor.getId());
                    return null;
                }
            }
            return __mostLikelySuccessor;
        }
        return null;
    }

    /**
     * Add a linear path to the code emission order greedily following the most likely successor.
     */
    private static <T extends AbstractBlockBase<T>> void addPathToCodeEmittingOrder(T __initialBlock, List<T> __order, PriorityQueue<T> __worklist, BitSet __visitedBlocks)
    {
        T __block = __initialBlock;
        while (__block != null)
        {
            // Skip loop headers if there is only a single loop end block to make
            // the backward jump be a conditional jump.
            if (!skipLoopHeader(__block))
            {
                // Align unskipped loop headers as they are the target of the backward jump.
                if (__block.isLoopHeader())
                {
                    __block.setAlign(true);
                }
                addBlock(__block, __order);
            }

            Loop<T> __loop = __block.getLoop();
            if (__block.isLoopEnd() && skipLoopHeader(__loop.getHeader()))
            {
                // This is the only loop end of a skipped loop header.
                // Add the header immediately afterwards.
                addBlock(__loop.getHeader(), __order);

                // Make sure the loop successors of the loop header are aligned,
                // as they are the target of the backward jump.
                for (T __successor : __loop.getHeader().getSuccessors())
                {
                    if (__successor.getLoopDepth() == __block.getLoopDepth())
                    {
                        __successor.setAlign(true);
                    }
                }
            }

            T __mostLikelySuccessor = findAndMarkMostLikelySuccessor(__block, __visitedBlocks);
            enqueueSuccessors(__block, __worklist, __visitedBlocks);
            __block = __mostLikelySuccessor;
        }
    }

    /**
     * Adds a block to the ordering.
     */
    private static <T extends AbstractBlockBase<T>> void addBlock(T __header, List<T> __order)
    {
        __order.add(__header);
    }

    /**
     * Find the highest likely unvisited successor block of a given block.
     */
    private static <T extends AbstractBlockBase<T>> T findAndMarkMostLikelySuccessor(T __block, BitSet __visitedBlocks)
    {
        T __result = null;
        for (T __successor : __block.getSuccessors())
        {
            if (!__visitedBlocks.get(__successor.getId()) && __successor.getLoopDepth() >= __block.getLoopDepth() && (__result == null || __successor.probability() >= __result.probability()))
            {
                __result = __successor;
            }
        }
        if (__result != null)
        {
            __visitedBlocks.set(__result.getId());
        }
        return __result;
    }

    /**
     * Add successor blocks into the given work list if they are not already marked as visited.
     */
    private static <T extends AbstractBlockBase<T>> void enqueueSuccessors(T __block, PriorityQueue<T> __worklist, BitSet __visitedBlocks)
    {
        for (T __successor : __block.getSuccessors())
        {
            if (!__visitedBlocks.get(__successor.getId()))
            {
                __visitedBlocks.set(__successor.getId());
                __worklist.add(__successor);
            }
        }
    }

    /**
     * Skip the loop header block if the loop consists of more than one block and it has only a
     * single loop end block.
     */
    private static <T extends AbstractBlockBase<T>> boolean skipLoopHeader(AbstractBlockBase<T> __block)
    {
        return (__block.isLoopHeader() && !__block.isLoopEnd() && __block.getLoop().numBackedges() == 1);
    }

    /**
     * Comparator for sorting blocks based on loop depth and probability.
     */
    // @class ComputeBlockOrder.BlockOrderComparator
    private static final class BlockOrderComparator<T extends AbstractBlockBase<T>> implements Comparator<T>
    {
        // @def
        private static final double EPSILON = 1E-6;

        @Override
        public int compare(T __a, T __b)
        {
            // Loop blocks before any loop exit block. The only exception are
            // blocks that are (almost) impossible to reach.
            if (__a.probability() > EPSILON && __b.probability() > EPSILON)
            {
                int __diff = __b.getLoopDepth() - __a.getLoopDepth();
                if (__diff != 0)
                {
                    return __diff;
                }
            }

            // Blocks with high probability before blocks with low probability.
            if (__a.probability() > __b.probability())
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
    }
}
