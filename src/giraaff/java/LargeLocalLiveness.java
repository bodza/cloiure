package giraaff.java;

import java.util.BitSet;

import giraaff.java.BciBlockMapping.BciBlock;

// @class LargeLocalLiveness
public final class LargeLocalLiveness extends LocalLiveness
{
    // @field
    private BitSet[] localsLiveIn;
    // @field
    private BitSet[] localsLiveOut;
    // @field
    private BitSet[] localsLiveGen;
    // @field
    private BitSet[] localsLiveKill;
    // @field
    private BitSet[] localsChangedInLoop;

    // @cons
    public LargeLocalLiveness(BciBlock[] __blocks, int __maxLocals, int __loopCount)
    {
        super(__blocks);
        int __blocksSize = __blocks.length;
        localsLiveIn = new BitSet[__blocksSize];
        localsLiveOut = new BitSet[__blocksSize];
        localsLiveGen = new BitSet[__blocksSize];
        localsLiveKill = new BitSet[__blocksSize];
        for (int __i = 0; __i < __blocksSize; __i++)
        {
            localsLiveIn[__i] = new BitSet(__maxLocals);
            localsLiveOut[__i] = new BitSet(__maxLocals);
            localsLiveGen[__i] = new BitSet(__maxLocals);
            localsLiveKill[__i] = new BitSet(__maxLocals);
        }
        localsChangedInLoop = new BitSet[__loopCount];
        for (int __i = 0; __i < __loopCount; ++__i)
        {
            localsChangedInLoop[__i] = new BitSet(__maxLocals);
        }
    }

    @Override
    protected int liveOutCardinality(int __blockID)
    {
        return localsLiveOut[__blockID].cardinality();
    }

    @Override
    protected void propagateLiveness(int __blockID, int __successorID)
    {
        localsLiveOut[__blockID].or(localsLiveIn[__successorID]);
    }

    @Override
    protected void updateLiveness(int __blockID)
    {
        BitSet __liveIn = localsLiveIn[__blockID];
        __liveIn.clear();
        __liveIn.or(localsLiveOut[__blockID]);
        __liveIn.andNot(localsLiveKill[__blockID]);
        __liveIn.or(localsLiveGen[__blockID]);
    }

    @Override
    protected void loadOne(int __blockID, int __local)
    {
        if (!localsLiveKill[__blockID].get(__local))
        {
            localsLiveGen[__blockID].set(__local);
        }
    }

    @Override
    protected void storeOne(int __blockID, int __local)
    {
        if (!localsLiveGen[__blockID].get(__local))
        {
            localsLiveKill[__blockID].set(__local);
        }

        BciBlock __block = blocks[__blockID];
        long __tmp = __block.loops;
        int __pos = 0;
        while (__tmp != 0)
        {
            if ((__tmp & 1L) == 1L)
            {
                this.localsChangedInLoop[__pos].set(__local);
            }
            __tmp >>>= 1;
            ++__pos;
        }
    }

    @Override
    public boolean localIsLiveIn(BciBlock __block, int __local)
    {
        return __block.getId() >= Integer.MAX_VALUE ? true : localsLiveIn[__block.getId()].get(__local);
    }

    @Override
    public boolean localIsLiveOut(BciBlock __block, int __local)
    {
        return __block.getId() >= Integer.MAX_VALUE ? true : localsLiveOut[__block.getId()].get(__local);
    }

    @Override
    public boolean localIsChangedInLoop(int __loopId, int __local)
    {
        return localsChangedInLoop[__loopId].get(__local);
    }
}
