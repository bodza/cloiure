package giraaff.lir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

///
// A buffer to enqueue updates to a list. This avoids frequent re-sizing of the list and copying of
// list elements when insertions are done at multiple positions of the list. Additionally, it
// ensures that the list is not modified while it is, e.g. iterated, and instead only modified once
// after the iteration is done.
//
// The buffer uses internal data structures to store the enqueued updates. To avoid allocations, a
// buffer can be re-used. Call the methods in the following order: {@link #init}, {@link #append},
// {@link #append}, ..., {@link #finish()}, {@link #init}, ...
//
// Note: This class does not depend on LIRInstruction, so we could make it a generic utility class.
///
// @class LIRInsertionBuffer
public final class LIRInsertionBuffer
{
    ///
    // The lir list where ops of this buffer should be inserted later (null when uninitialized).
    ///
    // @field
    private List<LIRInstruction> ___lir;

    ///
    // List of insertion points. index and count are stored alternately: indexAndCount[i * 2]: the
    // index into lir list where "count" ops should be inserted indexAndCount[i * 2 + 1]: the number
    // of ops to be inserted at index
    ///
    // @field
    private int[] ___indexAndCount;
    // @field
    private int ___indexAndCountSize;

    ///
    // The LIROps to be inserted.
    ///
    // @field
    private final List<LIRInstruction> ___ops;

    // @cons
    public LIRInsertionBuffer()
    {
        super();
        this.___indexAndCount = new int[8];
        this.___ops = new ArrayList<>(4);
    }

    ///
    // Initialize this buffer. This method must be called before using {@link #append}.
    ///
    public void init(List<LIRInstruction> __newLir)
    {
        this.___lir = __newLir;
    }

    public boolean initialized()
    {
        return this.___lir != null;
    }

    public List<LIRInstruction> lirList()
    {
        return this.___lir;
    }

    ///
    // Enqueue a new instruction that will be appended to the instruction list when
    // {@link #finish()} is called. The new instruction is added <b>before</b> the existing
    // instruction with the given index. This method can only be called with increasing values of
    // index, e.g. once an instruction was appended with index 4, subsequent instructions can only
    // be appended with index 4 or higher.
    ///
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
        this.___ops.add(__op);
    }

    ///
    // Append all enqueued instructions to the instruction list. After that, {@link #init(List)} can
    // be called again to re-use this buffer.
    ///
    public void finish()
    {
        if (this.___ops.size() > 0)
        {
            int __n = this.___lir.size();
            // increase size of instructions list
            for (int __i = 0; __i < this.___ops.size(); __i++)
            {
                this.___lir.add(null);
            }
            // insert ops from buffer into instructions list
            int __opIndex = this.___ops.size() - 1;
            int __ipIndex = numberOfInsertionPoints() - 1;
            int __fromIndex = __n - 1;
            int __toIndex = this.___lir.size() - 1;
            while (__ipIndex >= 0)
            {
                int __index = indexAt(__ipIndex);
                // make room after insertion point
                while (__fromIndex >= __index)
                {
                    this.___lir.set(__toIndex--, this.___lir.get(__fromIndex--));
                }
                // insert ops from buffer
                for (int __i = countAt(__ipIndex); __i > 0; __i--)
                {
                    this.___lir.set(__toIndex--, this.___ops.get(__opIndex--));
                }
                __ipIndex--;
            }
            this.___indexAndCountSize = 0;
            this.___ops.clear();
        }
        this.___lir = null;
    }

    private void appendNew(int __index, int __count)
    {
        int __oldSize = this.___indexAndCountSize;
        int __newSize = __oldSize + 2;
        if (__newSize > this.___indexAndCount.length)
        {
            this.___indexAndCount = Arrays.copyOf(this.___indexAndCount, __newSize * 2);
        }
        this.___indexAndCount[__oldSize] = __index;
        this.___indexAndCount[__oldSize + 1] = __count;
        this.___indexAndCountSize = __newSize;
    }

    private void setCountAt(int __i, int __value)
    {
        this.___indexAndCount[(__i << 1) + 1] = __value;
    }

    private int numberOfInsertionPoints()
    {
        return this.___indexAndCountSize >> 1;
    }

    private int indexAt(int __i)
    {
        return this.___indexAndCount[(__i << 1)];
    }

    private int countAt(int __i)
    {
        return this.___indexAndCount[(__i << 1) + 1];
    }
}
