package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.alloc.lsra.Interval;

// @class OptimizingLinearScanWalker
public final class OptimizingLinearScanWalker extends LinearScanWalker
{
    // @cons OptimizingLinearScanWalker
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
        this.___unhandledLists.addToListSortedByStartAndUsePositions(Interval.RegisterBinding.Stack, __interval);
    }

    @Override
    void walk()
    {
        for (AbstractBlockBase<?> __block : this.___allocator.sortedBlocks())
        {
            optimizeBlock(__block);
        }
        super.walk();
    }

    private void optimizeBlock(AbstractBlockBase<?> __block)
    {
        if (__block.getPredecessorCount() == 1)
        {
            int __nextBlock = this.___allocator.getFirstLirInstructionId(__block);
            walkTo(__nextBlock);

            boolean __changed = true;
            // we need to do this because the active lists might change
            loop: while (__changed)
            {
                __changed = false;
                for (Interval __active = this.___activeLists.get(Interval.RegisterBinding.Any); !__active.isEndMarker(); __active = __active.___next)
                {
                    if (optimize(__nextBlock, __block, __active, Interval.RegisterBinding.Any))
                    {
                        __changed = true;
                        break loop;
                    }
                }
                for (Interval __active = this.___activeLists.get(Interval.RegisterBinding.Stack); !__active.isEndMarker(); __active = __active.___next)
                {
                    if (optimize(__nextBlock, __block, __active, Interval.RegisterBinding.Stack))
                    {
                        __changed = true;
                        break loop;
                    }
                }
            }
        }
    }

    private boolean optimize(int __currentPos, AbstractBlockBase<?> __currentBlock, Interval __currentInterval, Interval.RegisterBinding __binding)
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
        int __predEndId = this.___allocator.getLastLirInstructionId(__predecessorBlock);
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

        Interval __splitPart = __currentInterval.split(__currentPos, this.___allocator);
        this.___activeLists.remove(__binding, __currentInterval);

        // the currentSplitChild is needed later when moves are inserted for reloading
        __splitPart.makeCurrentSplitChild();

        if (GraalOptions.lsraOptSplitOnly)
        {
            // just add the split interval to the unhandled list
            this.___unhandledLists.addToListSortedByStartAndUsePositions(Interval.RegisterBinding.Any, __splitPart);
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
                this.___activeLists.addToListSortedByCurrentFromPositions(Interval.RegisterBinding.Stack, __splitPart);
                __splitPart.___state = Interval.IntervalState.Active;

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
        spillCollectActiveAny(Interval.RegisterPriority.LiveAtLoopEnd);
        spillCollectInactiveAny(__interval);

        // the register must be free at least until this position
        boolean __needSplit = this.___blockPos[__reg.number] <= __interval.to();

        int __splitPos = this.___blockPos[__reg.number];

        __interval.assignLocation(__reg.asValue(__interval.kind()));
        if (__needSplit)
        {
            // register not available for full interval : so split it
            splitWhenPartialRegisterAvailable(__interval, __splitPos);
        }

        // perform splitting and spilling for all affected intervals
        splitAndSpillIntersectingIntervals(__reg);

        // activate interval
        this.___activeLists.addToListSortedByCurrentFromPositions(Interval.RegisterBinding.Any, __interval);
        __interval.___state = Interval.IntervalState.Active;
    }
}
