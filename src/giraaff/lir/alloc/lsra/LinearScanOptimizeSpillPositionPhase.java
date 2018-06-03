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

// @class LinearScanOptimizeSpillPositionPhase
public final class LinearScanOptimizeSpillPositionPhase extends LinearScanAllocationPhase
{
    // @field
    private final LinearScan allocator;

    // @cons
    LinearScanOptimizeSpillPositionPhase(LinearScan __allocator)
    {
        super();
        this.allocator = __allocator;
    }

    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationContext __context)
    {
        optimizeSpillPosition(__lirGenRes);
    }

    private void optimizeSpillPosition(LIRGenerationResult __res)
    {
        LIRInsertionBuffer[] __insertionBuffers = new LIRInsertionBuffer[this.allocator.getLIR().linearScanOrder().length];
        for (Interval __interval : this.allocator.intervals())
        {
            optimizeInterval(__insertionBuffers, __interval, __res);
        }
        for (LIRInsertionBuffer __insertionBuffer : __insertionBuffers)
        {
            if (__insertionBuffer != null)
            {
                __insertionBuffer.finish();
            }
        }
    }

    private void optimizeInterval(LIRInsertionBuffer[] __insertionBuffers, Interval __interval, LIRGenerationResult __res)
    {
        if (__interval == null || !__interval.isSplitParent() || __interval.spillState() != SpillState.SpillInDominator)
        {
            return;
        }
        AbstractBlockBase<?> __defBlock = this.allocator.blockForId(__interval.spillDefinitionPos());
        AbstractBlockBase<?> __spillBlock = null;
        Interval __firstSpillChild = null;
        for (Interval __splitChild : __interval.getSplitChildren())
        {
            if (LIRValueUtil.isStackSlotValue(__splitChild.location()))
            {
                if (__firstSpillChild == null || __splitChild.from() < __firstSpillChild.from())
                {
                    __firstSpillChild = __splitChild;
                }
                // iterate all blocks where the interval has use positions
                for (AbstractBlockBase<?> __splitBlock : blocksForInterval(__splitChild))
                {
                    if (AbstractControlFlowGraph.dominates(__defBlock, __splitBlock))
                    {
                        if (__spillBlock == null)
                        {
                            __spillBlock = __splitBlock;
                        }
                        else
                        {
                            __spillBlock = AbstractControlFlowGraph.commonDominator(__spillBlock, __splitBlock);
                        }
                    }
                }
            }
        }
        if (__spillBlock == null)
        {
            // no spill interval
            __interval.setSpillState(SpillState.StoreAtDefinition);
            return;
        }
        // move out of loops
        if (__defBlock.getLoopDepth() < __spillBlock.getLoopDepth())
        {
            __spillBlock = moveSpillOutOfLoop(__defBlock, __spillBlock);
        }

        /*
         * The spill block is the begin of the first split child (aka the value is on the stack).
         *
         * The problem is that if spill block has more than one predecessor, the values at the
         * end of the predecessors might differ. Therefore, we would need a spill move in all
         * predecessors. To avoid this we spill in the dominator.
         */
        if (!__defBlock.equals(__spillBlock) && __spillBlock.equals(this.allocator.blockForId(__firstSpillChild.from())))
        {
            AbstractBlockBase<?> __dom = __spillBlock.getDominator();
            __spillBlock = __dom;
        }
        if (__defBlock.equals(__spillBlock))
        {
            // definition is the best choice
            __interval.setSpillState(SpillState.StoreAtDefinition);
            return;
        }

        if (__defBlock.probability() <= __spillBlock.probability())
        {
            // better spill block has the same probability -> do nothing
            __interval.setSpillState(SpillState.StoreAtDefinition);
            return;
        }

        LIRInsertionBuffer __insertionBuffer = __insertionBuffers[__spillBlock.getId()];
        if (__insertionBuffer == null)
        {
            __insertionBuffer = new LIRInsertionBuffer();
            __insertionBuffers[__spillBlock.getId()] = __insertionBuffer;
            __insertionBuffer.init(this.allocator.getLIR().getLIRforBlock(__spillBlock));
        }
        int __spillOpId = this.allocator.getFirstLirInstructionId(__spillBlock);
        // insert spill move
        AllocatableValue __fromLocation = __interval.getSplitChildAtOpId(__spillOpId, OperandMode.DEF, this.allocator).location();
        AllocatableValue __toLocation = LinearScan.canonicalSpillOpr(__interval);
        LIRInstruction __move = this.allocator.getSpillMoveFactory().createMove(__toLocation, __fromLocation);
        __move.setId(LinearScan.DOMINATOR_SPILL_MOVE_ID);
        // We can use the insertion buffer directly because we always insert at position 1.
        __insertionBuffer.append(1, __move);

        __interval.setSpillDefinitionPos(__spillOpId);
    }

    /**
     * Iterate over all {@link AbstractBlockBase blocks} of an interval.
     */
    // @class LinearScanOptimizeSpillPositionPhase.IntervalBlockIterator
    // @closure
    private final class IntervalBlockIterator implements Iterator<AbstractBlockBase<?>>
    {
        // @field
        Range range;
        // @field
        AbstractBlockBase<?> block;

        // @cons
        IntervalBlockIterator(Interval __interval)
        {
            super();
            range = __interval.first();
            block = LinearScanOptimizeSpillPositionPhase.this.allocator.blockForId(range.from);
        }

        @Override
        public AbstractBlockBase<?> next()
        {
            AbstractBlockBase<?> __currentBlock = block;
            int __nextBlockIndex = block.getLinearScanNumber() + 1;
            if (__nextBlockIndex < LinearScanOptimizeSpillPositionPhase.this.allocator.sortedBlocks().length)
            {
                block = LinearScanOptimizeSpillPositionPhase.this.allocator.sortedBlocks()[__nextBlockIndex];
                if (range.to <= LinearScanOptimizeSpillPositionPhase.this.allocator.getFirstLirInstructionId(block))
                {
                    range = range.next;
                    if (range.isEndMarker())
                    {
                        block = null;
                    }
                    else
                    {
                        block = LinearScanOptimizeSpillPositionPhase.this.allocator.blockForId(range.from);
                    }
                }
            }
            else
            {
                block = null;
            }
            return __currentBlock;
        }

        @Override
        public boolean hasNext()
        {
            return block != null;
        }
    }

    private Iterable<AbstractBlockBase<?>> blocksForInterval(Interval __interval)
    {
        // @closure
        return new Iterable<AbstractBlockBase<?>>()
        {
            @Override
            public Iterator<AbstractBlockBase<?>> iterator()
            {
                return new IntervalBlockIterator(__interval);
            }
        };
    }

    private static AbstractBlockBase<?> moveSpillOutOfLoop(AbstractBlockBase<?> __defBlock, AbstractBlockBase<?> __spillBlock)
    {
        int __defLoopDepth = __defBlock.getLoopDepth();
        for (AbstractBlockBase<?> __block = __spillBlock.getDominator(); !__defBlock.equals(__block); __block = __block.getDominator())
        {
            if (__block.getLoopDepth() <= __defLoopDepth)
            {
                return __block;
            }
        }
        return __defBlock;
    }
}
