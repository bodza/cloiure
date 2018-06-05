package giraaff.java;

import giraaff.bytecode.BytecodeStream;
import giraaff.bytecode.Bytecodes;
import giraaff.java.BciBlockMapping.BciBlock;

///
// Encapsulates the liveness calculation, so that subclasses for locals <= 64 and locals > 64 can be implemented.
///
// @class LocalLiveness
public abstract class LocalLiveness
{
    // @field
    protected final BciBlock[] ___blocks;

    public static LocalLiveness compute(BytecodeStream __stream, BciBlock[] __blocks, int __maxLocals, int __loopCount)
    {
        LocalLiveness __liveness = __maxLocals <= 64 ? new SmallLocalLiveness(__blocks, __maxLocals, __loopCount) : new LargeLocalLiveness(__blocks, __maxLocals, __loopCount);
        __liveness.computeLiveness(__stream);
        return __liveness;
    }

    // @cons
    protected LocalLiveness(BciBlock[] __blocks)
    {
        super();
        this.___blocks = __blocks;
    }

    void computeLiveness(BytecodeStream __stream)
    {
        for (BciBlock __block : this.___blocks)
        {
            computeLocalLiveness(__stream, __block);
        }

        boolean __changed;
        int __iteration = 0;
        do
        {
            __changed = false;
            for (int __i = this.___blocks.length - 1; __i >= 0; __i--)
            {
                BciBlock __block = this.___blocks[__i];
                int __blockID = __block.getId();

                boolean __blockChanged = (__iteration == 0);
                if (__block.getSuccessorCount() > 0)
                {
                    int __oldCardinality = liveOutCardinality(__blockID);
                    for (BciBlock __sux : __block.getSuccessors())
                    {
                        propagateLiveness(__blockID, __sux.getId());
                    }
                    __blockChanged |= (__oldCardinality != liveOutCardinality(__blockID));
                }

                if (__blockChanged)
                {
                    updateLiveness(__blockID);
                }
                __changed |= __blockChanged;
            }
            __iteration++;
        } while (__changed);
    }

    ///
    // Returns whether the local is live at the beginning of the given block.
    ///
    public abstract boolean localIsLiveIn(BciBlock __block, int __local);

    ///
    // Returns whether the local is set in the given loop.
    ///
    public abstract boolean localIsChangedInLoop(int __loopId, int __local);

    ///
    // Returns whether the local is live at the end of the given block.
    ///
    public abstract boolean localIsLiveOut(BciBlock __block, int __local);

    ///
    // Returns the number of live locals at the end of the given block.
    ///
    protected abstract int liveOutCardinality(int __blockID);

    ///
    // Adds all locals the are in the liveIn of the successor to the liveOut of the block.
    ///
    protected abstract void propagateLiveness(int __blockID, int __successorID);

    ///
    // Calculates a new liveIn for the given block from liveOut, liveKill and liveGen.
    ///
    protected abstract void updateLiveness(int __blockID);

    ///
    // Adds the local to liveGen if it wasn't already killed in this block.
    ///
    protected abstract void loadOne(int __blockID, int __local);

    ///
    // Add this local to liveKill if it wasn't already generated in this block.
    ///
    protected abstract void storeOne(int __blockID, int __local);

    private void computeLocalLiveness(BytecodeStream __stream, BciBlock __block)
    {
        if (__block.isExceptionDispatch())
        {
            return;
        }
        int __blockID = __block.getId();
        int __localIndex;
        __stream.setBCI(__block.___startBci);
        while (__stream.currentBCI() <= __block.___endBci)
        {
            switch (__stream.currentBC())
            {
                case Bytecodes.LLOAD:
                {
                    loadTwo(__blockID, __stream.readLocalIndex());
                    break;
                }
                case Bytecodes.LLOAD_0:
                {
                    loadTwo(__blockID, 0);
                    break;
                }
                case Bytecodes.LLOAD_1:
                {
                    loadTwo(__blockID, 1);
                    break;
                }
                case Bytecodes.LLOAD_2:
                {
                    loadTwo(__blockID, 2);
                    break;
                }
                case Bytecodes.LLOAD_3:
                {
                    loadTwo(__blockID, 3);
                    break;
                }
                case Bytecodes.IINC:
                {
                    __localIndex = __stream.readLocalIndex();
                    loadOne(__blockID, __localIndex);
                    storeOne(__blockID, __localIndex);
                    break;
                }
                case Bytecodes.ILOAD:
                case Bytecodes.ALOAD:
                case Bytecodes.RET:
                {
                    loadOne(__blockID, __stream.readLocalIndex());
                    break;
                }
                case Bytecodes.ILOAD_0:
                case Bytecodes.ALOAD_0:
                {
                    loadOne(__blockID, 0);
                    break;
                }
                case Bytecodes.ILOAD_1:
                case Bytecodes.ALOAD_1:
                {
                    loadOne(__blockID, 1);
                    break;
                }
                case Bytecodes.ILOAD_2:
                case Bytecodes.ALOAD_2:
                {
                    loadOne(__blockID, 2);
                    break;
                }
                case Bytecodes.ILOAD_3:
                case Bytecodes.ALOAD_3:
                {
                    loadOne(__blockID, 3);
                    break;
                }

                case Bytecodes.LSTORE:
                {
                    storeTwo(__blockID, __stream.readLocalIndex());
                    break;
                }
                case Bytecodes.LSTORE_0:
                {
                    storeTwo(__blockID, 0);
                    break;
                }
                case Bytecodes.LSTORE_1:
                {
                    storeTwo(__blockID, 1);
                    break;
                }
                case Bytecodes.LSTORE_2:
                {
                    storeTwo(__blockID, 2);
                    break;
                }
                case Bytecodes.LSTORE_3:
                {
                    storeTwo(__blockID, 3);
                    break;
                }
                case Bytecodes.ISTORE:
                case Bytecodes.ASTORE:
                {
                    storeOne(__blockID, __stream.readLocalIndex());
                    break;
                }
                case Bytecodes.ISTORE_0:
                case Bytecodes.ASTORE_0:
                {
                    storeOne(__blockID, 0);
                    break;
                }
                case Bytecodes.ISTORE_1:
                case Bytecodes.ASTORE_1:
                {
                    storeOne(__blockID, 1);
                    break;
                }
                case Bytecodes.ISTORE_2:
                case Bytecodes.ASTORE_2:
                {
                    storeOne(__blockID, 2);
                    break;
                }
                case Bytecodes.ISTORE_3:
                case Bytecodes.ASTORE_3:
                {
                    storeOne(__blockID, 3);
                    break;
                }
            }
            __stream.next();
        }
    }

    private void loadTwo(int __blockID, int __local)
    {
        loadOne(__blockID, __local);
        loadOne(__blockID, __local + 1);
    }

    private void storeTwo(int __blockID, int __local)
    {
        storeOne(__blockID, __local);
        storeOne(__blockID, __local + 1);
    }
}
