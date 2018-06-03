package giraaff.java;

import java.util.BitSet;

import giraaff.java.BciBlockMapping.BciBlock;

// @class LargeLocalLiveness
public final class LargeLocalLiveness extends LocalLiveness
{
    // @field
    private BitSet[] ___localsLiveIn;
    // @field
    private BitSet[] ___localsLiveOut;
    // @field
    private BitSet[] ___localsLiveGen;
    // @field
    private BitSet[] ___localsLiveKill;
    // @field
    private BitSet[] ___localsChangedInLoop;

    // @cons
    public LargeLocalLiveness(BciBlock[] __blocks, int __maxLocals, int __loopCount)
    {
        super(__blocks);
        int __blocksSize = __blocks.length;
        this.___localsLiveIn = new BitSet[__blocksSize];
        this.___localsLiveOut = new BitSet[__blocksSize];
        this.___localsLiveGen = new BitSet[__blocksSize];
        this.___localsLiveKill = new BitSet[__blocksSize];
        for (int __i = 0; __i < __blocksSize; __i++)
        {
            this.___localsLiveIn[__i] = new BitSet(__maxLocals);
            this.___localsLiveOut[__i] = new BitSet(__maxLocals);
            this.___localsLiveGen[__i] = new BitSet(__maxLocals);
            this.___localsLiveKill[__i] = new BitSet(__maxLocals);
        }
        this.___localsChangedInLoop = new BitSet[__loopCount];
        for (int __i = 0; __i < __loopCount; ++__i)
        {
            this.___localsChangedInLoop[__i] = new BitSet(__maxLocals);
        }
    }

    @Override
    protected int liveOutCardinality(int __blockID)
    {
        return this.___localsLiveOut[__blockID].cardinality();
    }

    @Override
    protected void propagateLiveness(int __blockID, int __successorID)
    {
        this.___localsLiveOut[__blockID].or(this.___localsLiveIn[__successorID]);
    }

    @Override
    protected void updateLiveness(int __blockID)
    {
        BitSet __liveIn = this.___localsLiveIn[__blockID];
        __liveIn.clear();
        __liveIn.or(this.___localsLiveOut[__blockID]);
        __liveIn.andNot(this.___localsLiveKill[__blockID]);
        __liveIn.or(this.___localsLiveGen[__blockID]);
    }

    @Override
    protected void loadOne(int __blockID, int __local)
    {
        if (!this.___localsLiveKill[__blockID].get(__local))
        {
            this.___localsLiveGen[__blockID].set(__local);
        }
    }

    @Override
    protected void storeOne(int __blockID, int __local)
    {
        if (!this.___localsLiveGen[__blockID].get(__local))
        {
            this.___localsLiveKill[__blockID].set(__local);
        }

        BciBlock __block = this.___blocks[__blockID];
        long __tmp = __block.___loops;
        int __pos = 0;
        while (__tmp != 0)
        {
            if ((__tmp & 1L) == 1L)
            {
                this.___localsChangedInLoop[__pos].set(__local);
            }
            __tmp >>>= 1;
            ++__pos;
        }
    }

    @Override
    public boolean localIsLiveIn(BciBlock __block, int __local)
    {
        return __block.getId() >= Integer.MAX_VALUE ? true : this.___localsLiveIn[__block.getId()].get(__local);
    }

    @Override
    public boolean localIsLiveOut(BciBlock __block, int __local)
    {
        return __block.getId() >= Integer.MAX_VALUE ? true : this.___localsLiveOut[__block.getId()].get(__local);
    }

    @Override
    public boolean localIsChangedInLoop(int __loopId, int __local)
    {
        return this.___localsChangedInLoop[__loopId].get(__local);
    }
}
