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
    OptimizingLinearScanWalker(LinearScan __allocator, Interval __unhandledFixedFirst, Interval __unhandledAnyFirst)
    {
        super(__allocator, __unhandledFixedFirst, __unhandledAnyFirst);
    }

    @Override
    protected void handleSpillSlot(Interval __interval)
    {
        if (__interval.canMaterialize())
        {
            return;
        }
        unhandledLists.addToListSortedByStartAndUsePositions(RegisterBinding.Stack, __interval);
    }

    @Override
    void walk()
    {
        for (AbstractBlockBase<?> __block : allocator.sortedBlocks())
        {
            optimizeBlock(__block);
        }
        super.walk();
    }

    private void optimizeBlock(AbstractBlockBase<?> __block)
    {
        if (__block.getPredecessorCount() == 1)
        {
            int __nextBlock = allocator.getFirstLirInstructionId(__block);
            walkTo(__nextBlock);

            boolean __changed = true;
            // we need to do this because the active lists might change
            loop: while (__changed)
            {
                __changed = false;
                for (Interval __active = activeLists.get(RegisterBinding.Any); !__active.isEndMarker(); __active = __active.next)
                {
                    if (optimize(__nextBlock, __block, __active, RegisterBinding.Any))
                    {
                        __changed = true;
                        break loop;
                    }
                }
                for (Interval __active = activeLists.get(RegisterBinding.Stack); !__active.isEndMarker(); __active = __active.next)
                {
                    if (optimize(__nextBlock, __block, __active, RegisterBinding.Stack))
                    {
                        __changed = true;
                        break loop;
                    }
                }
            }
        }
    }

    private boolean optimize(int __currentPos, AbstractBlockBase<?> __currentBlock, Interval __currentInterval, RegisterBinding __binding)
    {
        if (!__currentInterval.isSplitChild())
        {
            // interval is not a split child -> no need for optimization
            return false;
        }

        if (__currentInterval.from() == __currentPos)
        {
            // the interval starts at the current position so no need for splitting
            return false;
        }

        // get current location
        AllocatableValue __currentLocation = __currentInterval.location();

        // get predecessor stuff
        AbstractBlockBase<?> __predecessorBlock = __currentBlock.getPredecessors()[0];
        int __predEndId = allocator.getLastLirInstructionId(__predecessorBlock);
        Interval __predecessorInterval = __currentInterval.getIntervalCoveringOpId(__predEndId);
        AllocatableValue __predecessorLocation = __predecessorInterval.location();

        // END initialize and sanity checks

        if (__currentLocation.equals(__predecessorLocation))
        {
            // locations are already equal -> nothing to optimize
            return false;
        }

        if (!LIRValueUtil.isStackSlotValue(__predecessorLocation) && !ValueUtil.isRegister(__predecessorLocation))
        {
            // value is materialized -> no need for optimization
            return false;
        }

        // split current interval at current position

        Interval __splitPart = __currentInterval.split(__currentPos, allocator);
        activeLists.remove(__binding, __currentInterval);

        // the currentSplitChild is needed later when moves are inserted for reloading
        __splitPart.makeCurrentSplitChild();

        if (GraalOptions.lsraOptSplitOnly)
        {
            // just add the split interval to the unhandled list
            unhandledLists.addToListSortedByStartAndUsePositions(RegisterBinding.Any, __splitPart);
        }
        else
        {
            if (ValueUtil.isRegister(__predecessorLocation))
            {
                splitRegisterInterval(__splitPart, ValueUtil.asRegister(__predecessorLocation));
            }
            else
            {
                __splitPart.assignLocation(__predecessorLocation);
                // activate interval
                activeLists.addToListSortedByCurrentFromPositions(RegisterBinding.Stack, __splitPart);
                __splitPart.state = State.Active;

                splitStackInterval(__splitPart);
            }
        }
        return true;
    }

    private void splitRegisterInterval(Interval __interval, Register __reg)
    {
        // collect current usage of registers
        initVarsForAlloc(__interval);
        initUseLists(false);
        spillExcludeActiveFixed();
        // spillBlockUnhandledFixed(cur);
        spillBlockInactiveFixed(__interval);
        spillCollectActiveAny(RegisterPriority.LiveAtLoopEnd);
        spillCollectInactiveAny(__interval);

        // the register must be free at least until this position
        boolean __needSplit = blockPos[__reg.number] <= __interval.to();

        int __splitPos = blockPos[__reg.number];

        __interval.assignLocation(__reg.asValue(__interval.kind()));
        if (__needSplit)
        {
            // register not available for full interval : so split it
            splitWhenPartialRegisterAvailable(__interval, __splitPos);
        }

        // perform splitting and spilling for all affected intervals
        splitAndSpillIntersectingIntervals(__reg);

        // activate interval
        activeLists.addToListSortedByCurrentFromPositions(RegisterBinding.Any, __interval);
        __interval.state = State.Active;
    }
}
