package giraaff.java;

import giraaff.bytecode.BytecodeStream;
import giraaff.bytecode.Bytecodes;
import giraaff.java.BciBlockMapping.BciBlock;

/**
 * Encapsulates the liveness calculation, so that subclasses for locals &le; 64 and locals &gt; 64
 * can be implemented.
 */
public abstract class LocalLiveness
{
    protected final BciBlock[] blocks;

    public static LocalLiveness compute(BytecodeStream stream, BciBlock[] blocks, int maxLocals, int loopCount)
    {
        LocalLiveness liveness = maxLocals <= 64 ? new SmallLocalLiveness(blocks, maxLocals, loopCount) : new LargeLocalLiveness(blocks, maxLocals, loopCount);
        liveness.computeLiveness(stream);
        return liveness;
    }

    protected LocalLiveness(BciBlock[] blocks)
    {
        this.blocks = blocks;
    }

    void computeLiveness(BytecodeStream stream)
    {
        for (BciBlock block : blocks)
        {
            computeLocalLiveness(stream, block);
        }

        boolean changed;
        int iteration = 0;
        do
        {
            changed = false;
            for (int i = blocks.length - 1; i >= 0; i--)
            {
                BciBlock block = blocks[i];
                int blockID = block.getId();

                boolean blockChanged = (iteration == 0);
                if (block.getSuccessorCount() > 0)
                {
                    int oldCardinality = liveOutCardinality(blockID);
                    for (BciBlock sux : block.getSuccessors())
                    {
                        propagateLiveness(blockID, sux.getId());
                    }
                    blockChanged |= (oldCardinality != liveOutCardinality(blockID));
                }

                if (blockChanged)
                {
                    updateLiveness(blockID);
                }
                changed |= blockChanged;
            }
            iteration++;
        } while (changed);
    }

    /**
     * Returns whether the local is live at the beginning of the given block.
     */
    public abstract boolean localIsLiveIn(BciBlock block, int local);

    /**
     * Returns whether the local is set in the given loop.
     */
    public abstract boolean localIsChangedInLoop(int loopId, int local);

    /**
     * Returns whether the local is live at the end of the given block.
     */
    public abstract boolean localIsLiveOut(BciBlock block, int local);

    /**
     * Returns a string representation of the liveIn values of the given block.
     */
    protected abstract String debugLiveIn(int blockID);

    /**
     * Returns a string representation of the liveOut values of the given block.
     */
    protected abstract String debugLiveOut(int blockID);

    /**
     * Returns a string representation of the liveGen values of the given block.
     */
    protected abstract String debugLiveGen(int blockID);

    /**
     * Returns a string representation of the liveKill values of the given block.
     */
    protected abstract String debugLiveKill(int blockID);

    /**
     * Returns the number of live locals at the end of the given block.
     */
    protected abstract int liveOutCardinality(int blockID);

    /**
     * Adds all locals the are in the liveIn of the successor to the liveOut of the block.
     */
    protected abstract void propagateLiveness(int blockID, int successorID);

    /**
     * Calculates a new liveIn for the given block from liveOut, liveKill and liveGen.
     */
    protected abstract void updateLiveness(int blockID);

    /**
     * Adds the local to liveGen if it wasn't already killed in this block.
     */
    protected abstract void loadOne(int blockID, int local);

    /**
     * Add this local to liveKill if it wasn't already generated in this block.
     */
    protected abstract void storeOne(int blockID, int local);

    private void computeLocalLiveness(BytecodeStream stream, BciBlock block)
    {
        if (block.isExceptionDispatch())
        {
            return;
        }
        int blockID = block.getId();
        int localIndex;
        stream.setBCI(block.startBci);
        while (stream.currentBCI() <= block.endBci)
        {
            switch (stream.currentBC())
            {
                case Bytecodes.LLOAD:
                case Bytecodes.DLOAD:
                    loadTwo(blockID, stream.readLocalIndex());
                    break;
                case Bytecodes.LLOAD_0:
                case Bytecodes.DLOAD_0:
                    loadTwo(blockID, 0);
                    break;
                case Bytecodes.LLOAD_1:
                case Bytecodes.DLOAD_1:
                    loadTwo(blockID, 1);
                    break;
                case Bytecodes.LLOAD_2:
                case Bytecodes.DLOAD_2:
                    loadTwo(blockID, 2);
                    break;
                case Bytecodes.LLOAD_3:
                case Bytecodes.DLOAD_3:
                    loadTwo(blockID, 3);
                    break;
                case Bytecodes.IINC:
                    localIndex = stream.readLocalIndex();
                    loadOne(blockID, localIndex);
                    storeOne(blockID, localIndex);
                    break;
                case Bytecodes.ILOAD:
                case Bytecodes.FLOAD:
                case Bytecodes.ALOAD:
                case Bytecodes.RET:
                    loadOne(blockID, stream.readLocalIndex());
                    break;
                case Bytecodes.ILOAD_0:
                case Bytecodes.FLOAD_0:
                case Bytecodes.ALOAD_0:
                    loadOne(blockID, 0);
                    break;
                case Bytecodes.ILOAD_1:
                case Bytecodes.FLOAD_1:
                case Bytecodes.ALOAD_1:
                    loadOne(blockID, 1);
                    break;
                case Bytecodes.ILOAD_2:
                case Bytecodes.FLOAD_2:
                case Bytecodes.ALOAD_2:
                    loadOne(blockID, 2);
                    break;
                case Bytecodes.ILOAD_3:
                case Bytecodes.FLOAD_3:
                case Bytecodes.ALOAD_3:
                    loadOne(blockID, 3);
                    break;

                case Bytecodes.LSTORE:
                case Bytecodes.DSTORE:
                    storeTwo(blockID, stream.readLocalIndex());
                    break;
                case Bytecodes.LSTORE_0:
                case Bytecodes.DSTORE_0:
                    storeTwo(blockID, 0);
                    break;
                case Bytecodes.LSTORE_1:
                case Bytecodes.DSTORE_1:
                    storeTwo(blockID, 1);
                    break;
                case Bytecodes.LSTORE_2:
                case Bytecodes.DSTORE_2:
                    storeTwo(blockID, 2);
                    break;
                case Bytecodes.LSTORE_3:
                case Bytecodes.DSTORE_3:
                    storeTwo(blockID, 3);
                    break;
                case Bytecodes.ISTORE:
                case Bytecodes.FSTORE:
                case Bytecodes.ASTORE:
                    storeOne(blockID, stream.readLocalIndex());
                    break;
                case Bytecodes.ISTORE_0:
                case Bytecodes.FSTORE_0:
                case Bytecodes.ASTORE_0:
                    storeOne(blockID, 0);
                    break;
                case Bytecodes.ISTORE_1:
                case Bytecodes.FSTORE_1:
                case Bytecodes.ASTORE_1:
                    storeOne(blockID, 1);
                    break;
                case Bytecodes.ISTORE_2:
                case Bytecodes.FSTORE_2:
                case Bytecodes.ASTORE_2:
                    storeOne(blockID, 2);
                    break;
                case Bytecodes.ISTORE_3:
                case Bytecodes.FSTORE_3:
                case Bytecodes.ASTORE_3:
                    storeOne(blockID, 3);
                    break;
            }
            stream.next();
        }
    }

    private void loadTwo(int blockID, int local)
    {
        loadOne(blockID, local);
        loadOne(blockID, local + 1);
    }

    private void storeTwo(int blockID, int local)
    {
        storeOne(blockID, local);
        storeOne(blockID, local + 1);
    }
}
