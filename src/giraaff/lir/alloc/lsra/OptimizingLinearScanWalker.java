package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.alloc.lsra.Interval.RegisterBinding;
import giraaff.lir.alloc.lsra.Interval.RegisterPriority;
import giraaff.lir.alloc.lsra.Interval.State;

// @class OptimizingLinearScanWalker
public final class OptimizingLinearScanWalker extends LinearScanWalker
{
    // @cons
    OptimizingLinearScanWalker(LinearScan allocator, Interval unhandledFixedFirst, Interval unhandledAnyFirst)
    {
        super(allocator, unhandledFixedFirst, unhandledAnyFirst);
    }

    @Override
    protected void handleSpillSlot(Interval interval)
    {
        if (interval.canMaterialize())
        {
            return;
        }
        unhandledLists.addToListSortedByStartAndUsePositions(RegisterBinding.Stack, interval);
    }

    @Override
    void walk()
    {
        for (AbstractBlockBase<?> block : allocator.sortedBlocks())
        {
            optimizeBlock(block);
        }
        super.walk();
    }

    private void optimizeBlock(AbstractBlockBase<?> block)
    {
        if (block.getPredecessorCount() == 1)
        {
            int nextBlock = allocator.getFirstLirInstructionId(block);
            walkTo(nextBlock);

            boolean changed = true;
            // we need to do this because the active lists might change
            loop: while (changed)
            {
                changed = false;
                for (Interval active = activeLists.get(RegisterBinding.Any); !active.isEndMarker(); active = active.next)
                {
                    if (optimize(nextBlock, block, active, RegisterBinding.Any))
                    {
                        changed = true;
                        break loop;
                    }
                }
                for (Interval active = activeLists.get(RegisterBinding.Stack); !active.isEndMarker(); active = active.next)
                {
                    if (optimize(nextBlock, block, active, RegisterBinding.Stack))
                    {
                        changed = true;
                        break loop;
                    }
                }
            }
        }
    }

    private boolean optimize(int currentPos, AbstractBlockBase<?> currentBlock, Interval currentInterval, RegisterBinding binding)
    {
        if (!currentInterval.isSplitChild())
        {
            // interval is not a split child -> no need for optimization
            return false;
        }

        if (currentInterval.from() == currentPos)
        {
            // the interval starts at the current position so no need for splitting
            return false;
        }

        // get current location
        AllocatableValue currentLocation = currentInterval.location();

        // get predecessor stuff
        AbstractBlockBase<?> predecessorBlock = currentBlock.getPredecessors()[0];
        int predEndId = allocator.getLastLirInstructionId(predecessorBlock);
        Interval predecessorInterval = currentInterval.getIntervalCoveringOpId(predEndId);
        AllocatableValue predecessorLocation = predecessorInterval.location();

        // END initialize and sanity checks

        if (currentLocation.equals(predecessorLocation))
        {
            // locations are already equal -> nothing to optimize
            return false;
        }

        if (!LIRValueUtil.isStackSlotValue(predecessorLocation) && !ValueUtil.isRegister(predecessorLocation))
        {
            // value is materialized -> no need for optimization
            return false;
        }

        // split current interval at current position

        Interval splitPart = currentInterval.split(currentPos, allocator);
        activeLists.remove(binding, currentInterval);

        // the currentSplitChild is needed later when moves are inserted for reloading
        splitPart.makeCurrentSplitChild();

        if (GraalOptions.lsraOptSplitOnly)
        {
            // just add the split interval to the unhandled list
            unhandledLists.addToListSortedByStartAndUsePositions(RegisterBinding.Any, splitPart);
        }
        else
        {
            if (ValueUtil.isRegister(predecessorLocation))
            {
                splitRegisterInterval(splitPart, ValueUtil.asRegister(predecessorLocation));
            }
            else
            {
                splitPart.assignLocation(predecessorLocation);
                // activate interval
                activeLists.addToListSortedByCurrentFromPositions(RegisterBinding.Stack, splitPart);
                splitPart.state = State.Active;

                splitStackInterval(splitPart);
            }
        }
        return true;
    }

    private void splitRegisterInterval(Interval interval, Register reg)
    {
        // collect current usage of registers
        initVarsForAlloc(interval);
        initUseLists(false);
        spillExcludeActiveFixed();
        // spillBlockUnhandledFixed(cur);
        spillBlockInactiveFixed(interval);
        spillCollectActiveAny(RegisterPriority.LiveAtLoopEnd);
        spillCollectInactiveAny(interval);

        // the register must be free at least until this position
        boolean needSplit = blockPos[reg.number] <= interval.to();

        int splitPos = blockPos[reg.number];

        interval.assignLocation(reg.asValue(interval.kind()));
        if (needSplit)
        {
            // register not available for full interval : so split it
            splitWhenPartialRegisterAvailable(interval, splitPos);
        }

        // perform splitting and spilling for all affected intervals
        splitAndSpillIntersectingIntervals(reg);

        // activate interval
        activeLists.addToListSortedByCurrentFromPositions(RegisterBinding.Any, interval);
        interval.state = State.Active;
    }
}
