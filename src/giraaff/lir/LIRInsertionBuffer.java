package giraaff.lir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A buffer to enqueue updates to a list. This avoids frequent re-sizing of the list and copying of
 * list elements when insertions are done at multiple positions of the list. Additionally, it
 * ensures that the list is not modified while it is, e.g. iterated, and instead only modified once
 * after the iteration is done.
 *
 * The buffer uses internal data structures to store the enqueued updates. To avoid allocations, a
 * buffer can be re-used. Call the methods in the following order: {@link #init}, {@link #append},
 * {@link #append}, ..., {@link #finish()}, {@link #init}, ...
 *
 * Note: This class does not depend on LIRInstruction, so we could make it a generic utility class.
 */
// @class LIRInsertionBuffer
public final class LIRInsertionBuffer
{
    /**
     * The lir list where ops of this buffer should be inserted later (null when uninitialized).
     */
    // @field
    private List<LIRInstruction> lir;

    /**
     * List of insertion points. index and count are stored alternately: indexAndCount[i * 2]: the
     * index into lir list where "count" ops should be inserted indexAndCount[i * 2 + 1]: the number
     * of ops to be inserted at index
     */
    // @field
    private int[] indexAndCount;
    // @field
    private int indexAndCountSize;

    /**
     * The LIROps to be inserted.
     */
    // @field
    private final List<LIRInstruction> ops;

    // @cons
    public LIRInsertionBuffer()
    {
        super();
        indexAndCount = new int[8];
        ops = new ArrayList<>(4);
    }

    /**
     * Initialize this buffer. This method must be called before using {@link #append}.
     */
    public void init(List<LIRInstruction> __newLir)
    {
        this.lir = __newLir;
    }

    public boolean initialized()
    {
        return lir != null;
    }

    public List<LIRInstruction> lirList()
    {
        return lir;
    }

    /**
     * Enqueue a new instruction that will be appended to the instruction list when
     * {@link #finish()} is called. The new instruction is added <b>before</b> the existing
     * instruction with the given index. This method can only be called with increasing values of
     * index, e.g. once an instruction was appended with index 4, subsequent instructions can only
     * be appended with index 4 or higher.
     */
    public void append(int __index, LIRInstruction __op)
    {
        int __i = numberOfInsertionPoints() - 1;
        if (__i < 0 || indexAt(__i) < __index)
        {
            appendNew(__index, 1);
        }
        else
        {
            setCountAt(__i, countAt(__i) + 1);
        }
        ops.add(__op);
    }

    /**
     * Append all enqueued instructions to the instruction list. After that, {@link #init(List)} can
     * be called again to re-use this buffer.
     */
    public void finish()
    {
        if (ops.size() > 0)
        {
            int __n = lir.size();
            // increase size of instructions list
            for (int __i = 0; __i < ops.size(); __i++)
            {
                lir.add(null);
            }
            // insert ops from buffer into instructions list
            int __opIndex = ops.size() - 1;
            int __ipIndex = numberOfInsertionPoints() - 1;
            int __fromIndex = __n - 1;
            int __toIndex = lir.size() - 1;
            while (__ipIndex >= 0)
            {
                int __index = indexAt(__ipIndex);
                // make room after insertion point
                while (__fromIndex >= __index)
                {
                    lir.set(__toIndex--, lir.get(__fromIndex--));
                }
                // insert ops from buffer
                for (int __i = countAt(__ipIndex); __i > 0; __i--)
                {
                    lir.set(__toIndex--, ops.get(__opIndex--));
                }
                __ipIndex--;
            }
            indexAndCountSize = 0;
            ops.clear();
        }
        lir = null;
    }

    private void appendNew(int __index, int __count)
    {
        int __oldSize = indexAndCountSize;
        int __newSize = __oldSize + 2;
        if (__newSize > this.indexAndCount.length)
        {
            indexAndCount = Arrays.copyOf(indexAndCount, __newSize * 2);
        }
        indexAndCount[__oldSize] = __index;
        indexAndCount[__oldSize + 1] = __count;
        this.indexAndCountSize = __newSize;
    }

    private void setCountAt(int __i, int __value)
    {
        indexAndCount[(__i << 1) + 1] = __value;
    }

    private int numberOfInsertionPoints()
    {
        return indexAndCountSize >> 1;
    }

    private int indexAt(int __i)
    {
        return indexAndCount[(__i << 1)];
    }

    private int countAt(int __i)
    {
        return indexAndCount[(__i << 1) + 1];
    }
}
