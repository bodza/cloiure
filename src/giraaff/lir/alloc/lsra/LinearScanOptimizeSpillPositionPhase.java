package giraaff.lir.alloc.lsra;

import java.util.Iterator;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.alloc.lsra.Interval.SpillState;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;

public final class LinearScanOptimizeSpillPositionPhase extends LinearScanAllocationPhase
{
    private final LinearScan allocator;

    LinearScanOptimizeSpillPositionPhase(LinearScan allocator)
    {
        this.allocator = allocator;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        optimizeSpillPosition(lirGenRes);
    }

    private void optimizeSpillPosition(LIRGenerationResult res)
    {
        LIRInsertionBuffer[] insertionBuffers = new LIRInsertionBuffer[allocator.getLIR().linearScanOrder().length];
        for (Interval interval : allocator.intervals())
        {
            optimizeInterval(insertionBuffers, interval, res);
        }
        for (LIRInsertionBuffer insertionBuffer : insertionBuffers)
        {
            if (insertionBuffer != null)
            {
                insertionBuffer.finish();
            }
        }
    }

    private void optimizeInterval(LIRInsertionBuffer[] insertionBuffers, Interval interval, LIRGenerationResult res)
    {
        if (interval == null || !interval.isSplitParent() || interval.spillState() != SpillState.SpillInDominator)
        {
            return;
        }
        AbstractBlockBase<?> defBlock = allocator.blockForId(interval.spillDefinitionPos());
        AbstractBlockBase<?> spillBlock = null;
        Interval firstSpillChild = null;
        for (Interval splitChild : interval.getSplitChildren())
        {
            if (LIRValueUtil.isStackSlotValue(splitChild.location()))
            {
                if (firstSpillChild == null || splitChild.from() < firstSpillChild.from())
                {
                    firstSpillChild = splitChild;
                }
                // iterate all blocks where the interval has use positions
                for (AbstractBlockBase<?> splitBlock : blocksForInterval(splitChild))
                {
                    if (AbstractControlFlowGraph.dominates(defBlock, splitBlock))
                    {
                        if (spillBlock == null)
                        {
                            spillBlock = splitBlock;
                        }
                        else
                        {
                            spillBlock = AbstractControlFlowGraph.commonDominator(spillBlock, splitBlock);
                        }
                    }
                }
            }
        }
        if (spillBlock == null)
        {
            // no spill interval
            interval.setSpillState(SpillState.StoreAtDefinition);
            return;
        }
        // move out of loops
        if (defBlock.getLoopDepth() < spillBlock.getLoopDepth())
        {
            spillBlock = moveSpillOutOfLoop(defBlock, spillBlock);
        }

        /*
         * The spill block is the begin of the first split child (aka the value is on the stack).
         *
         * The problem is that if spill block has more than one predecessor, the values at the
         * end of the predecessors might differ. Therefore, we would need a spill move in all
         * predecessors. To avoid this we spill in the dominator.
         */
        if (!defBlock.equals(spillBlock) && spillBlock.equals(allocator.blockForId(firstSpillChild.from())))
        {
            AbstractBlockBase<?> dom = spillBlock.getDominator();
            spillBlock = dom;
        }
        if (defBlock.equals(spillBlock))
        {
            // definition is the best choice
            interval.setSpillState(SpillState.StoreAtDefinition);
            return;
        }

        if (defBlock.probability() <= spillBlock.probability())
        {
            // better spill block has the same probability -> do nothing
            interval.setSpillState(SpillState.StoreAtDefinition);
            return;
        }

        LIRInsertionBuffer insertionBuffer = insertionBuffers[spillBlock.getId()];
        if (insertionBuffer == null)
        {
            insertionBuffer = new LIRInsertionBuffer();
            insertionBuffers[spillBlock.getId()] = insertionBuffer;
            insertionBuffer.init(allocator.getLIR().getLIRforBlock(spillBlock));
        }
        int spillOpId = allocator.getFirstLirInstructionId(spillBlock);
        // insert spill move
        AllocatableValue fromLocation = interval.getSplitChildAtOpId(spillOpId, OperandMode.DEF, allocator).location();
        AllocatableValue toLocation = LinearScan.canonicalSpillOpr(interval);
        LIRInstruction move = allocator.getSpillMoveFactory().createMove(toLocation, fromLocation);
        move.setComment(res, "LSRAOptimizeSpillPos: optimize spill pos");
        move.setId(LinearScan.DOMINATOR_SPILL_MOVE_ID);
        // We can use the insertion buffer directly because we always insert at position 1.
        insertionBuffer.append(1, move);

        interval.setSpillDefinitionPos(spillOpId);
    }

    /**
     * Iterate over all {@link AbstractBlockBase blocks} of an interval.
     */
    private class IntervalBlockIterator implements Iterator<AbstractBlockBase<?>>
    {
        Range range;
        AbstractBlockBase<?> block;

        IntervalBlockIterator(Interval interval)
        {
            range = interval.first();
            block = allocator.blockForId(range.from);
        }

        @Override
        public AbstractBlockBase<?> next()
        {
            AbstractBlockBase<?> currentBlock = block;
            int nextBlockIndex = block.getLinearScanNumber() + 1;
            if (nextBlockIndex < allocator.sortedBlocks().length)
            {
                block = allocator.sortedBlocks()[nextBlockIndex];
                if (range.to <= allocator.getFirstLirInstructionId(block))
                {
                    range = range.next;
                    if (range.isEndMarker())
                    {
                        block = null;
                    }
                    else
                    {
                        block = allocator.blockForId(range.from);
                    }
                }
            }
            else
            {
                block = null;
            }
            return currentBlock;
        }

        @Override
        public boolean hasNext()
        {
            return block != null;
        }
    }

    private Iterable<AbstractBlockBase<?>> blocksForInterval(Interval interval)
    {
        return new Iterable<AbstractBlockBase<?>>()
        {
            @Override
            public Iterator<AbstractBlockBase<?>> iterator()
            {
                return new IntervalBlockIterator(interval);
            }
        };
    }

    private static AbstractBlockBase<?> moveSpillOutOfLoop(AbstractBlockBase<?> defBlock, AbstractBlockBase<?> spillBlock)
    {
        int defLoopDepth = defBlock.getLoopDepth();
        for (AbstractBlockBase<?> block = spillBlock.getDominator(); !defBlock.equals(block); block = block.getDominator())
        {
            if (block.getLoopDepth() <= defLoopDepth)
            {
                return block;
            }
        }
        return defBlock;
    }
}