package giraaff.phases.util;

import giraaff.nodes.AbstractMergeNode;

/**
 * This class implements a worklist for dealing with blocks. The worklist can operate either as a
 * stack (i.e. first-in / last-out), or as a sorted list, where blocks can be sorted by a supplied
 * number. The latter usage lends itself naturally to iterative dataflow analysis problems.
 */
// @class BlockWorkList
public final class BlockWorkList
{
    // @field
    AbstractMergeNode[] workList;
    // @field
    int[] workListNumbers;
    // @field
    int workListIndex;

    /**
     * Adds a block to this list in an unsorted fashion, like a stack.
     *
     * @param block the block to add
     */
    public void add(AbstractMergeNode __block)
    {
        if (workList == null)
        {
            // worklist not allocated yet
            allocate();
        }
        else if (workListIndex == workList.length)
        {
            // need to grow the worklist
            grow();
        }
        // put the block at the end of the array
        workList[workListIndex++] = __block;
    }

    /**
     * Adds a block to this list, sorted by the supplied number. The block with the lowest number is
     * returned upon subsequent removes.
     *
     * @param block the block to add
     * @param number the number used to sort the block
     */
    public void addSorted(AbstractMergeNode __block, int __number)
    {
        if (workList == null)
        {
            // worklist not allocated yet
            allocate();
        }
        else if (workListIndex == workList.length)
        {
            // need to grow the worklist
            grow();
        }
        // put the block at the end of the array
        workList[workListIndex] = __block;
        workListNumbers[workListIndex] = __number;
        workListIndex++;
        int __i = workListIndex - 2;
        // push block towards the beginning of the array
        for ( ; __i >= 0; __i--)
        {
            int __n = workListNumbers[__i];
            if (__n >= __number)
            {
                break; // already in the right position
            }
            workList[__i + 1] = workList[__i]; // bubble b down by one
            workList[__i] = __block;           // and overwrite its place with block
            workListNumbers[__i + 1] = __n;    // bubble n down by one
            workListNumbers[__i] = __number;   // and overwrite its place with number
        }
    }

    /**
     * Removes the next block from this work list. If the blocks have been added in a sorted order,
     * then the block with the lowest number is returned. Otherwise, the last block added is returned.
     *
     * @return the next block in the list
     */
    public AbstractMergeNode removeFromWorkList()
    {
        if (workListIndex != 0)
        {
            return workList[--workListIndex];
        }
        return null;
    }

    /**
     * Checks whether the list is empty.
     *
     * @return {@code true} if this list is empty
     */
    public boolean isEmpty()
    {
        return workListIndex == 0;
    }

    private void allocate()
    {
        workList = new AbstractMergeNode[5];
        workListNumbers = new int[5];
    }

    private void grow()
    {
        int __prevLength = workList.length;
        AbstractMergeNode[] __nworkList = new AbstractMergeNode[__prevLength * 3];
        System.arraycopy(workList, 0, __nworkList, 0, __prevLength);
        workList = __nworkList;

        int[] __nworkListNumbers = new int[__prevLength * 3];
        System.arraycopy(workListNumbers, 0, __nworkListNumbers, 0, __prevLength);
        workListNumbers = __nworkListNumbers;
    }
}
