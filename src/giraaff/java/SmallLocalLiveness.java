package giraaff.java;

import giraaff.java.BciBlockMapping;

// @class SmallLocalLiveness
public final class SmallLocalLiveness extends LocalLiveness
{
    // local n is represented by the bit accessible as (1 << n)

    // @field
    private final long[] ___localsLiveIn;
    // @field
    private final long[] ___localsLiveOut;
    // @field
    private final long[] ___localsLiveGen;
    // @field
    private final long[] ___localsLiveKill;
    // @field
    private final long[] ___localsChangedInLoop;
    // @field
    private final int ___maxLocals;

    // @cons SmallLocalLiveness
    public SmallLocalLiveness(BciBlockMapping.BciBlock[] __blocks, int __maxLocals, int __loopCount)
    {
        super(__blocks);
        this.___maxLocals = __maxLocals;
        int __blockSize = __blocks.length;
        this.___localsLiveIn = new long[__blockSize];
        this.___localsLiveOut = new long[__blockSize];
        this.___localsLiveGen = new long[__blockSize];
        this.___localsLiveKill = new long[__blockSize];
        this.___localsChangedInLoop = new long[__loopCount];
    }

    @Override
    protected int liveOutCardinality(int __blockID)
    {
        return Long.bitCount(this.___localsLiveOut[__blockID]);
    }

    @Override
    protected void propagateLiveness(int __blockID, int __successorID)
    {
        this.___localsLiveOut[__blockID] |= this.___localsLiveIn[__successorID];
    }

    @Override
    protected void updateLiveness(int __blockID)
    {
        this.___localsLiveIn[__blockID] = (this.___localsLiveOut[__blockID] & ~this.___localsLiveKill[__blockID]) | this.___localsLiveGen[__blockID];
    }

    @Override
    protected void loadOne(int __blockID, int __local)
    {
        long __bit = 1L << __local;
        if ((this.___localsLiveKill[__blockID] & __bit) == 0L)
        {
            this.___localsLiveGen[__blockID] |= __bit;
        }
    }

    @Override
    protected void storeOne(int __blockID, int __local)
    {
        long __bit = 1L << __local;
        if ((this.___localsLiveGen[__blockID] & __bit) == 0L)
        {
            this.___localsLiveKill[__blockID] |= __bit;
        }

        BciBlockMapping.BciBlock __block = this.___blocks[__blockID];
        long __tmp = __block.___loops;
        int __pos = 0;
        while (__tmp != 0)
        {
            if ((__tmp & 1L) == 1L)
            {
                this.___localsChangedInLoop[__pos] |= __bit;
            }
            __tmp >>>= 1;
            ++__pos;
        }
    }

    @Override
    public boolean localIsLiveIn(BciBlockMapping.BciBlock __block, int __local)
    {
        int __blockID = __block.getId();
        return __blockID >= Integer.MAX_VALUE ? false : (this.___localsLiveIn[__blockID] & (1L << __local)) != 0L;
    }

    @Override
    public boolean localIsLiveOut(BciBlockMapping.BciBlock __block, int __local)
    {
        int __blockID = __block.getId();
        return __blockID >= Integer.MAX_VALUE ? false : (this.___localsLiveOut[__blockID] & (1L << __local)) != 0L;
    }

    @Override
    public boolean localIsChangedInLoop(int __loopId, int __local)
    {
        return (this.___localsChangedInLoop[__loopId] & (1L << __local)) != 0L;
    }
}
