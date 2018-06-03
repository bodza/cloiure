package giraaff.java;

import giraaff.java.BciBlockMapping.BciBlock;

// @class SmallLocalLiveness
public final class SmallLocalLiveness extends LocalLiveness
{
    // local n is represented by the bit accessible as (1 << n)

    // @field
    private final long[] localsLiveIn;
    // @field
    private final long[] localsLiveOut;
    // @field
    private final long[] localsLiveGen;
    // @field
    private final long[] localsLiveKill;
    // @field
    private final long[] localsChangedInLoop;
    // @field
    private final int maxLocals;

    // @cons
    public SmallLocalLiveness(BciBlock[] __blocks, int __maxLocals, int __loopCount)
    {
        super(__blocks);
        this.maxLocals = __maxLocals;
        int __blockSize = __blocks.length;
        localsLiveIn = new long[__blockSize];
        localsLiveOut = new long[__blockSize];
        localsLiveGen = new long[__blockSize];
        localsLiveKill = new long[__blockSize];
        localsChangedInLoop = new long[__loopCount];
    }

    @Override
    protected int liveOutCardinality(int __blockID)
    {
        return Long.bitCount(localsLiveOut[__blockID]);
    }

    @Override
    protected void propagateLiveness(int __blockID, int __successorID)
    {
        localsLiveOut[__blockID] |= localsLiveIn[__successorID];
    }

    @Override
    protected void updateLiveness(int __blockID)
    {
        localsLiveIn[__blockID] = (localsLiveOut[__blockID] & ~localsLiveKill[__blockID]) | localsLiveGen[__blockID];
    }

    @Override
    protected void loadOne(int __blockID, int __local)
    {
        long __bit = 1L << __local;
        if ((localsLiveKill[__blockID] & __bit) == 0L)
        {
            localsLiveGen[__blockID] |= __bit;
        }
    }

    @Override
    protected void storeOne(int __blockID, int __local)
    {
        long __bit = 1L << __local;
        if ((localsLiveGen[__blockID] & __bit) == 0L)
        {
            localsLiveKill[__blockID] |= __bit;
        }

        BciBlock __block = blocks[__blockID];
        long __tmp = __block.loops;
        int __pos = 0;
        while (__tmp != 0)
        {
            if ((__tmp & 1L) == 1L)
            {
                this.localsChangedInLoop[__pos] |= __bit;
            }
            __tmp >>>= 1;
            ++__pos;
        }
    }

    @Override
    public boolean localIsLiveIn(BciBlock __block, int __local)
    {
        int __blockID = __block.getId();
        return __blockID >= Integer.MAX_VALUE ? false : (localsLiveIn[__blockID] & (1L << __local)) != 0L;
    }

    @Override
    public boolean localIsLiveOut(BciBlock __block, int __local)
    {
        int __blockID = __block.getId();
        return __blockID >= Integer.MAX_VALUE ? false : (localsLiveOut[__blockID] & (1L << __local)) != 0L;
    }

    @Override
    public boolean localIsChangedInLoop(int __loopId, int __local)
    {
        return (localsChangedInLoop[__loopId] & (1L << __local)) != 0L;
    }
}
