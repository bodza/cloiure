package graalvm.compiler.lir.alloc.lsra;

import static graalvm.compiler.core.common.cfg.AbstractControlFlowGraph.commonDominator;
import static graalvm.compiler.core.common.cfg.AbstractControlFlowGraph.dominates;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;

import java.util.Iterator;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.alloc.lsra.Interval.SpillState;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

public final class LinearScanOptimizeSpillPositionPhase extends LinearScanAllocationPhase
{
    private static final CounterKey betterSpillPos = DebugContext.counter("BetterSpillPosition");
    private static final CounterKey betterSpillPosWithLowerProbability = DebugContext.counter("BetterSpillPositionWithLowerProbability");

    private final LinearScan allocator;
    private DebugContext debug;

    LinearScanOptimizeSpillPositionPhase(LinearScan allocator)
    {
        this.allocator = allocator;
        this.debug = allocator.getDebug();
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        optimizeSpillPosition(lirGenRes);
        allocator.printIntervals("After optimize spill position");
    }

    @SuppressWarnings("try")
    private void optimizeSpillPosition(LIRGenerationResult res)
    {
        try (Indent indent0 = debug.logAndIndent("OptimizeSpillPositions"))
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
                    assert insertionBuffer.initialized() : "Insertion buffer is nonnull but not initialized!";
                    insertionBuffer.finish();
                }
            }
        }
    }

    @SuppressWarnings("try")
    private void optimizeInterval(LIRInsertionBuffer[] insertionBuffers, Interval interval, LIRGenerationResult res)
    {
        if (interval == null || !interval.isSplitParent() || interval.spillState() != SpillState.SpillInDominator)
        {
            return;
        }
        AbstractBlockBase<?> defBlock = allocator.blockForId(interval.spillDefinitionPos());
        AbstractBlockBase<?> spillBlock = null;
        Interval firstSpillChild = null;
        try (Indent indent = debug.logAndIndent("interval %s (%s)", interval, defBlock))
        {
            for (Interval splitChild : interval.getSplitChildren())
            {
                if (isStackSlotValue(splitChild.location()))
                {
                    if (firstSpillChild == null || splitChild.from() < firstSpillChild.from())
                    {
                        firstSpillChild = splitChild;
                    }
                    else
                    {
                        assert firstSpillChild.from() < splitChild.from();
                    }
                    // iterate all blocks where the interval has use positions
                    for (AbstractBlockBase<?> splitBlock : blocksForInterval(splitChild))
                    {
                        if (dominates(defBlock, splitBlock))
                        {
                            debug.log("Split interval %s, block %s", splitChild, splitBlock);
                            if (spillBlock == null)
                            {
                                spillBlock = splitBlock;
                            }
                            else
                            {
                                spillBlock = commonDominator(spillBlock, splitBlock);
                                assert spillBlock != null;
                            }
                        }
                    }
                }
            }
            if (spillBlock == null)
            {
                debug.log("not spill interval found");
                // no spill interval
                interval.setSpillState(SpillState.StoreAtDefinition);
                return;
            }
            debug.log(DebugContext.VERBOSE_LEVEL, "Spill block candidate (initial): %s", spillBlock);
            // move out of loops
            if (defBlock.getLoopDepth() < spillBlock.getLoopDepth())
            {
                spillBlock = moveSpillOutOfLoop(defBlock, spillBlock);
            }
            debug.log(DebugContext.VERBOSE_LEVEL, "Spill block candidate (after loop optimizaton): %s", spillBlock);

            /*
             * The spill block is the begin of the first split child (aka the value is on the
             * stack).
             *
             * The problem is that if spill block has more than one predecessor, the values at the
             * end of the predecessors might differ. Therefore, we would need a spill move in all
             * predecessors. To avoid this we spill in the dominator.
             */
            assert firstSpillChild != null;
            if (!defBlock.equals(spillBlock) && spillBlock.equals(allocator.blockForId(firstSpillChild.from())))
            {
                AbstractBlockBase<?> dom = spillBlock.getDominator();
                if (debug.isLogEnabled())
                {
                    debug.log("Spill block (%s) is the beginning of a spill child -> use dominator (%s)", spillBlock, dom);
                }
                spillBlock = dom;
            }
            if (defBlock.equals(spillBlock))
            {
                debug.log(DebugContext.VERBOSE_LEVEL, "Definition is the best choice: %s", defBlock);
                // definition is the best choice
                interval.setSpillState(SpillState.StoreAtDefinition);
                return;
            }
            assert dominates(defBlock, spillBlock);
            betterSpillPos.increment(debug);
            if (debug.isLogEnabled())
            {
                debug.log("Better spill position found (Block %s)", spillBlock);
            }

            if (defBlock.probability() <= spillBlock.probability())
            {
                debug.log(DebugContext.VERBOSE_LEVEL, "Definition has lower probability %s (%f) is lower than spill block %s (%f)", defBlock, defBlock.probability(), spillBlock, spillBlock.probability());
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
            debug.log(DebugContext.VERBOSE_LEVEL, "Insert spill move %s", move);
            move.setId(LinearScan.DOMINATOR_SPILL_MOVE_ID);
            /*
             * We can use the insertion buffer directly because we always insert at position 1.
             */
            insertionBuffer.append(1, move);

            betterSpillPosWithLowerProbability.increment(debug);
            interval.setSpillDefinitionPos(spillOpId);
        }
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
            assert block != null : "spill block not dominated by definition block?";
            if (block.getLoopDepth() <= defLoopDepth)
            {
                assert block.getLoopDepth() == defLoopDepth : "Cannot spill an interval outside of the loop where it is defined!";
                return block;
            }
        }
        return defBlock;
    }
}
