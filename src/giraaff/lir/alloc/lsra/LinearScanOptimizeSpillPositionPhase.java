package giraaff.lir.alloc.lsra;

import java.util.Iterator;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.AbstractControlFlowGraph;
import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.alloc.lsra.Interval;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;

// @class LinearScanOptimizeSpillPositionPhase
public final class LinearScanOptimizeSpillPositionPhase extends LinearScanAllocationPhase
{
    // @field
    private final LinearScan ___allocator;

    // @cons LinearScanOptimizeSpillPositionPhase
    LinearScanOptimizeSpillPositionPhase(LinearScan __allocator)
    {
        super();
        this.___allocator = __allocator;
    }

    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationPhase.AllocationContext __context)
    {
        optimizeSpillPosition(__lirGenRes);
    }

    private void optimizeSpillPosition(LIRGenerationResult __res)
    {
        LIRInsertionBuffer[] __insertionBuffers = new LIRInsertionBuffer[this.___allocator.getLIR().linearScanOrder().length];
        for (Interval __interval : this.___allocator.intervals())
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
        if (__interval == null || !__interval.isSplitParent() || __interval.spillState() != Interval.SpillState.SpillInDominator)
        {
            return;
        }
        AbstractBlockBase<?> __defBlock = this.___allocator.blockForId(__interval.spillDefinitionPos());
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
            __interval.setSpillState(Interval.SpillState.StoreAtDefinition);
            return;
        }
        // move out of loops
        if (__defBlock.getLoopDepth() < __spillBlock.getLoopDepth())
        {
            __spillBlock = moveSpillOutOfLoop(__defBlock, __spillBlock);
        }

        // The spill block is the begin of the first split child (aka the value is on the stack).
        //
        // The problem is that if spill block has more than one predecessor, the values at the
        // end of the predecessors might differ. Therefore, we would need a spill move in all
        // predecessors. To avoid this we spill in the dominator.
        if (!__defBlock.equals(__spillBlock) && __spillBlock.equals(this.___allocator.blockForId(__firstSpillChild.from())))
        {
            AbstractBlockBase<?> __dom = __spillBlock.getDominator();
            __spillBlock = __dom;
        }
        if (__defBlock.equals(__spillBlock))
        {
            // definition is the best choice
            __interval.setSpillState(Interval.SpillState.StoreAtDefinition);
            return;
        }

        if (__defBlock.probability() <= __spillBlock.probability())
        {
            // better spill block has the same probability -> do nothing
            __interval.setSpillState(Interval.SpillState.StoreAtDefinition);
            return;
        }

        LIRInsertionBuffer __insertionBuffer = __insertionBuffers[__spillBlock.getId()];
        if (__insertionBuffer == null)
        {
            __insertionBuffer = new LIRInsertionBuffer();
            __insertionBuffers[__spillBlock.getId()] = __insertionBuffer;
            __insertionBuffer.init(this.___allocator.getLIR().getLIRforBlock(__spillBlock));
        }
        int __spillOpId = this.___allocator.getFirstLirInstructionId(__spillBlock);
        // insert spill move
        AllocatableValue __fromLocation = __interval.getSplitChildAtOpId(__spillOpId, LIRInstruction.OperandMode.DEF, this.___allocator).location();
        AllocatableValue __toLocation = LinearScan.canonicalSpillOpr(__interval);
        LIRInstruction __move = this.___allocator.getSpillMoveFactory().createMove(__toLocation, __fromLocation);
        __move.setId(LinearScan.DOMINATOR_SPILL_MOVE_ID);
        // We can use the insertion buffer directly because we always insert at position 1.
        __insertionBuffer.append(1, __move);

        __interval.setSpillDefinitionPos(__spillOpId);
    }

    ///
    // Iterate over all {@link AbstractBlockBase blocks} of an interval.
    ///
    // @class LinearScanOptimizeSpillPositionPhase.IntervalBlockIterator
    // @closure
    private final class IntervalBlockIterator implements Iterator<AbstractBlockBase<?>>
    {
        // @field
        Range ___range;
        // @field
        AbstractBlockBase<?> ___block;

        // @cons LinearScanOptimizeSpillPositionPhase.IntervalBlockIterator
        IntervalBlockIterator(Interval __interval)
        {
            super();
            this.___range = __interval.first();
            this.___block = LinearScanOptimizeSpillPositionPhase.this.___allocator.blockForId(this.___range.___from);
        }

        @Override
        public AbstractBlockBase<?> next()
        {
            AbstractBlockBase<?> __currentBlock = this.___block;
            int __nextBlockIndex = this.___block.getLinearScanNumber() + 1;
            if (__nextBlockIndex < LinearScanOptimizeSpillPositionPhase.this.___allocator.sortedBlocks().length)
            {
                this.___block = LinearScanOptimizeSpillPositionPhase.this.___allocator.sortedBlocks()[__nextBlockIndex];
                if (this.___range.___to <= LinearScanOptimizeSpillPositionPhase.this.___allocator.getFirstLirInstructionId(this.___block))
                {
                    this.___range = this.___range.___next;
                    if (this.___range.isEndMarker())
                    {
                        this.___block = null;
                    }
                    else
                    {
                        this.___block = LinearScanOptimizeSpillPositionPhase.this.___allocator.blockForId(this.___range.___from);
                    }
                }
            }
            else
            {
                this.___block = null;
            }
            return __currentBlock;
        }

        @Override
        public boolean hasNext()
        {
            return this.___block != null;
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
                return new LinearScanOptimizeSpillPositionPhase.IntervalBlockIterator(__interval);
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
