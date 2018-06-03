package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.alloc.RegisterAllocationConfig.AllocatableRegisters;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.util.Util;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.StandardOp.ValueMoveOp;
import giraaff.lir.alloc.lsra.Interval.RegisterBinding;
import giraaff.lir.alloc.lsra.Interval.RegisterPriority;
import giraaff.lir.alloc.lsra.Interval.SpillState;
import giraaff.lir.alloc.lsra.Interval.State;
import giraaff.util.GraalError;

// @class LinearScanWalker
class LinearScanWalker extends IntervalWalker
{
    // @field
    protected Register[] availableRegs;

    // @field
    protected final int[] usePos;
    // @field
    protected final int[] blockPos;

    // @field
    protected List<Interval>[] spillIntervals;

    // @field
    private MoveResolver moveResolver; // for ordering spill moves

    // @field
    private int minReg;

    // @field
    private int maxReg;

    /**
     * Only 10% of the lists in {@link #spillIntervals} are actually used. But when they are used,
     * they can grow quite long. The maximum length observed was 45 (all numbers taken from a
     * bootstrap run of Graal). Therefore, we initialize {@link #spillIntervals} with this marker
     * value, and allocate a "real" list only on demand in {@link #setUsePos}.
     */
    // @def
    private static final List<Interval> EMPTY_LIST = Collections.emptyList();

    // accessors mapped to same functions in class LinearScan
    int blockCount()
    {
        return allocator.blockCount();
    }

    AbstractBlockBase<?> blockAt(int __idx)
    {
        return allocator.blockAt(__idx);
    }

    AbstractBlockBase<?> blockOfOpWithId(int __opId)
    {
        return allocator.blockForId(__opId);
    }

    // @cons
    LinearScanWalker(LinearScan __allocator, Interval __unhandledFixedFirst, Interval __unhandledAnyFirst)
    {
        super(__allocator, __unhandledFixedFirst, __unhandledAnyFirst);

        moveResolver = __allocator.createMoveResolver();
        spillIntervals = Util.uncheckedCast(new List<?>[__allocator.getRegisters().size()]);
        for (int __i = 0; __i < __allocator.getRegisters().size(); __i++)
        {
            spillIntervals[__i] = EMPTY_LIST;
        }
        usePos = new int[__allocator.getRegisters().size()];
        blockPos = new int[__allocator.getRegisters().size()];
    }

    void initUseLists(boolean __onlyProcessUsePos)
    {
        for (Register __register : availableRegs)
        {
            int __i = __register.number;
            usePos[__i] = Integer.MAX_VALUE;

            if (!__onlyProcessUsePos)
            {
                blockPos[__i] = Integer.MAX_VALUE;
                spillIntervals[__i].clear();
            }
        }
    }

    int maxRegisterNumber()
    {
        return maxReg;
    }

    int minRegisterNumber()
    {
        return minReg;
    }

    boolean isRegisterInRange(int __reg)
    {
        return __reg >= minRegisterNumber() && __reg <= maxRegisterNumber();
    }

    void excludeFromUse(Interval __i)
    {
        Value __location = __i.location();
        int __i1 = ValueUtil.asRegister(__location).number;
        if (isRegisterInRange(__i1))
        {
            usePos[__i1] = 0;
        }
    }

    void setUsePos(Interval __interval, int __usePos, boolean __onlyProcessUsePos)
    {
        if (__usePos != -1)
        {
            int __i = ValueUtil.asRegister(__interval.location()).number;
            if (isRegisterInRange(__i))
            {
                if (this.usePos[__i] > __usePos)
                {
                    this.usePos[__i] = __usePos;
                }
                if (!__onlyProcessUsePos)
                {
                    List<Interval> __list = spillIntervals[__i];
                    if (__list == EMPTY_LIST)
                    {
                        __list = new ArrayList<>(2);
                        spillIntervals[__i] = __list;
                    }
                    __list.add(__interval);
                }
            }
        }
    }

    void setBlockPos(Interval __i, int __blockPos)
    {
        if (__blockPos != -1)
        {
            int __reg = ValueUtil.asRegister(__i.location()).number;
            if (isRegisterInRange(__reg))
            {
                if (this.blockPos[__reg] > __blockPos)
                {
                    this.blockPos[__reg] = __blockPos;
                }
                if (usePos[__reg] > __blockPos)
                {
                    usePos[__reg] = __blockPos;
                }
            }
        }
    }

    void freeExcludeActiveFixed()
    {
        Interval __interval = activeLists.get(RegisterBinding.Fixed);
        while (!__interval.isEndMarker())
        {
            excludeFromUse(__interval);
            __interval = __interval.next;
        }
    }

    void freeExcludeActiveAny()
    {
        Interval __interval = activeLists.get(RegisterBinding.Any);
        while (!__interval.isEndMarker())
        {
            excludeFromUse(__interval);
            __interval = __interval.next;
        }
    }

    void freeCollectInactiveFixed(Interval __current)
    {
        Interval __interval = inactiveLists.get(RegisterBinding.Fixed);
        while (!__interval.isEndMarker())
        {
            if (__current.to() <= __interval.currentFrom())
            {
                setUsePos(__interval, __interval.currentFrom(), true);
            }
            else
            {
                setUsePos(__interval, __interval.currentIntersectsAt(__current), true);
            }
            __interval = __interval.next;
        }
    }

    void freeCollectInactiveAny(Interval __current)
    {
        Interval __interval = inactiveLists.get(RegisterBinding.Any);
        while (!__interval.isEndMarker())
        {
            setUsePos(__interval, __interval.currentIntersectsAt(__current), true);
            __interval = __interval.next;
        }
    }

    void freeCollectUnhandled(RegisterBinding __kind, Interval __current)
    {
        Interval __interval = unhandledLists.get(__kind);
        while (!__interval.isEndMarker())
        {
            setUsePos(__interval, __interval.intersectsAt(__current), true);
            if (__kind == RegisterBinding.Fixed && __current.to() <= __interval.from())
            {
                setUsePos(__interval, __interval.from(), true);
            }
            __interval = __interval.next;
        }
    }

    void spillExcludeActiveFixed()
    {
        Interval __interval = activeLists.get(RegisterBinding.Fixed);
        while (!__interval.isEndMarker())
        {
            excludeFromUse(__interval);
            __interval = __interval.next;
        }
    }

    void spillBlockUnhandledFixed(Interval __current)
    {
        Interval __interval = unhandledLists.get(RegisterBinding.Fixed);
        while (!__interval.isEndMarker())
        {
            setBlockPos(__interval, __interval.intersectsAt(__current));
            __interval = __interval.next;
        }
    }

    void spillBlockInactiveFixed(Interval __current)
    {
        Interval __interval = inactiveLists.get(RegisterBinding.Fixed);
        while (!__interval.isEndMarker())
        {
            if (__current.to() > __interval.currentFrom())
            {
                setBlockPos(__interval, __interval.currentIntersectsAt(__current));
            }

            __interval = __interval.next;
        }
    }

    void spillCollectActiveAny(RegisterPriority __registerPriority)
    {
        Interval __interval = activeLists.get(RegisterBinding.Any);
        while (!__interval.isEndMarker())
        {
            setUsePos(__interval, Math.min(__interval.nextUsage(__registerPriority, currentPosition), __interval.to()), false);
            __interval = __interval.next;
        }
    }

    void spillCollectInactiveAny(Interval __current)
    {
        Interval __interval = inactiveLists.get(RegisterBinding.Any);
        while (!__interval.isEndMarker())
        {
            if (__interval.currentIntersects(__current))
            {
                setUsePos(__interval, Math.min(__interval.nextUsage(RegisterPriority.LiveAtLoopEnd, currentPosition), __interval.to()), false);
            }
            __interval = __interval.next;
        }
    }

    void insertMove(int __operandId, Interval __srcIt, Interval __dstIt)
    {
        // Output all moves here. When source and target are equal, the move is optimized
        // away later in assignRegNums.
        int __opId = (__operandId + 1) & ~1;
        AbstractBlockBase<?> __opBlock = allocator.blockForId(__opId);

        // Calculate index of instruction inside instruction list of current block.
        // The minimal index (for a block with no spill moves) can be calculated, because
        // the numbering of instructions is known.
        // When the block already contains spill moves, the index must be increased until
        // the correct index is reached.
        ArrayList<LIRInstruction> __instructions = allocator.getLIR().getLIRforBlock(__opBlock);
        int __index = (__opId - __instructions.get(0).id()) >> 1;

        while (__instructions.get(__index).id() != __opId)
        {
            __index++;
        }

        // insert new instruction before instruction at position index
        moveResolver.moveInsertPosition(__instructions, __index);
        moveResolver.addMapping(__srcIt, __dstIt);
    }

    int findOptimalSplitPos(AbstractBlockBase<?> __minBlock, AbstractBlockBase<?> __maxBlock, int __maxSplitPos)
    {
        int __fromBlockNr = __minBlock.getLinearScanNumber();
        int __toBlockNr = __maxBlock.getLinearScanNumber();

        // Try to split at end of maxBlock. If this would be after maxSplitPos, then use the begin of maxBlock.
        int __optimalSplitPos = allocator.getLastLirInstructionId(__maxBlock) + 2;
        if (__optimalSplitPos > __maxSplitPos)
        {
            __optimalSplitPos = allocator.getFirstLirInstructionId(__maxBlock);
        }

        int __minLoopDepth = __maxBlock.getLoopDepth();
        for (int __i = __toBlockNr - 1; __minLoopDepth > 0 && __i >= __fromBlockNr; __i--)
        {
            AbstractBlockBase<?> __cur = blockAt(__i);

            if (__cur.getLoopDepth() < __minLoopDepth)
            {
                // block with lower loop-depth found . split at the end of this block
                __minLoopDepth = __cur.getLoopDepth();
                __optimalSplitPos = allocator.getLastLirInstructionId(__cur) + 2;
            }
        }

        return __optimalSplitPos;
    }

    int findOptimalSplitPos(Interval __interval, int __minSplitPos, int __maxSplitPos, boolean __doLoopOptimization)
    {
        int __optimalSplitPos = -1;
        if (__minSplitPos == __maxSplitPos)
        {
            // trivial case, no optimization of split position possible
            __optimalSplitPos = __minSplitPos;
        }
        else
        {
            // Reason for using minSplitPos - 1: when the minimal split pos is exactly at the beginning of a block,
            // then minSplitPos is also a possible split position.
            // Use the block before as minBlock, because then minBlock.lastLirInstructionId() + 2 == minSplitPos.
            AbstractBlockBase<?> __minBlock = allocator.blockForId(__minSplitPos - 1);

            // Reason for using maxSplitPos - 1: otherwise there would be an assert on failure when an interval
            // ends at the end of the last block of the method
            // (in this case, maxSplitPos == allocator().maxLirOpId() + 2, and there is no block at this opId).
            AbstractBlockBase<?> __maxBlock = allocator.blockForId(__maxSplitPos - 1);

            if (__minBlock == __maxBlock)
            {
                // split position cannot be moved to block boundary : so split as late as possible
                __optimalSplitPos = __maxSplitPos;
            }
            else
            {
                if (__interval.hasHoleBetween(__maxSplitPos - 1, __maxSplitPos) && !allocator.isBlockBegin(__maxSplitPos))
                {
                    // Do not move split position if the interval has a hole before maxSplitPos.
                    // Intervals resulting from Phi-functions have more than one definition (marked
                    // as mustHaveRegister) with a hole before each definition. When the register
                    // is needed for the second definition, an earlier reloading is unnecessary.
                    __optimalSplitPos = __maxSplitPos;
                }
                else
                {
                    // seach optimal block boundary between minSplitPos and maxSplitPos
                    if (__doLoopOptimization)
                    {
                        // Loop optimization: if a loop-end marker is found between min- and max-position,
                        // then split before this loop.
                        int __loopEndPos = __interval.nextUsageExact(RegisterPriority.LiveAtLoopEnd, allocator.getLastLirInstructionId(__minBlock) + 2);

                        if (__loopEndPos < __maxSplitPos)
                        {
                            // Loop-end marker found between min- and max-position. If it is not the end marker
                            // for the same loop as the min-position, move the max-position to this loop block.
                            // Desired result: uses tagged as shouldHaveRegister inside a loop cause a reloading
                            // of the interval (normally, only mustHaveRegister causes a reloading).
                            AbstractBlockBase<?> __loopBlock = allocator.blockForId(__loopEndPos);

                            int __maxSpillPos = allocator.getLastLirInstructionId(__loopBlock) + 2;
                            __optimalSplitPos = findOptimalSplitPos(__minBlock, __loopBlock, __maxSpillPos);
                            if (__optimalSplitPos == __maxSpillPos)
                            {
                                __optimalSplitPos = -1;
                            }
                        }
                    }

                    if (__optimalSplitPos == -1)
                    {
                        // not calculated by loop optimization
                        __optimalSplitPos = findOptimalSplitPos(__minBlock, __maxBlock, __maxSplitPos);
                    }
                }
            }
        }

        return __optimalSplitPos;
    }

    // split an interval at the optimal position between minSplitPos and
    // maxSplitPos in two parts:
    // 1) the left part has already a location assigned
    // 2) the right part is sorted into to the unhandled-list
    void splitBeforeUsage(Interval __interval, int __minSplitPos, int __maxSplitPos)
    {
        int __optimalSplitPos = findOptimalSplitPos(__interval, __minSplitPos, __maxSplitPos, true);

        if (__optimalSplitPos == __interval.to() && __interval.nextUsage(RegisterPriority.MustHaveRegister, __minSplitPos) == Integer.MAX_VALUE)
        {
            // the split position would be just before the end of the interval
            // . no split at all necessary
            return;
        }

        // must calculate this before the actual split is performed and before split position is
        // moved to odd opId
        boolean __moveNecessary = !allocator.isBlockBegin(__optimalSplitPos) && !__interval.hasHoleBetween(__optimalSplitPos - 1, __optimalSplitPos);

        if (!allocator.isBlockBegin(__optimalSplitPos))
        {
            // move position before actual instruction (odd opId)
            __optimalSplitPos = (__optimalSplitPos - 1) | 1;
        }

        Interval __splitPart = __interval.split(__optimalSplitPos, allocator);

        __splitPart.setInsertMoveWhenActivated(__moveNecessary);

        unhandledLists.addToListSortedByStartAndUsePositions(RegisterBinding.Any, __splitPart);
    }

    // split an interval at the optimal position between minSplitPos and
    // maxSplitPos in two parts:
    // 1) the left part has already a location assigned
    // 2) the right part is always on the stack and therefore ignored in further processing
    void splitForSpilling(Interval __interval)
    {
        // calculate allowed range of splitting position
        int __maxSplitPos = currentPosition;
        int __previousUsage = __interval.previousUsage(RegisterPriority.ShouldHaveRegister, __maxSplitPos);
        if (__previousUsage == currentPosition)
        {
            /*
             * If there is a usage with ShouldHaveRegister priority at the current position fall
             * back to MustHaveRegister priority. This only happens if register priority was
             * downgraded to MustHaveRegister in #allocLockedRegister.
             */
            __previousUsage = __interval.previousUsage(RegisterPriority.MustHaveRegister, __maxSplitPos);
        }
        int __minSplitPos = Math.max(__previousUsage + 1, __interval.from());

        if (__minSplitPos == __interval.from())
        {
            // the whole interval is never used, so spill it entirely to memory

            allocator.assignSpillSlot(__interval);
            handleSpillSlot(__interval);
            changeSpillState(__interval, __minSplitPos);

            // Also kick parent intervals out of register to memory when they have no use position.
            // This avoids short interval in register surrounded by intervals in memory.
            // Avoid useless moves from memory to register and back.
            Interval __parent = __interval;
            while (__parent != null && __parent.isSplitChild())
            {
                __parent = __parent.getSplitChildBeforeOpId(__parent.from());

                if (ValueUtil.isRegister(__parent.location()))
                {
                    if (__parent.firstUsage(RegisterPriority.ShouldHaveRegister) == Integer.MAX_VALUE)
                    {
                        // parent is never used, so kick it out of its assigned register
                        allocator.assignSpillSlot(__parent);
                        handleSpillSlot(__parent);
                    }
                    else
                    {
                        // do not go further back because the register is actually used by
                        // the interval
                        __parent = null;
                    }
                }
            }
        }
        else
        {
            // search optimal split pos, split interval and spill only the right hand part
            int __optimalSplitPos = findOptimalSplitPos(__interval, __minSplitPos, __maxSplitPos, false);

            if (!allocator.isBlockBegin(__optimalSplitPos))
            {
                // move position before actual instruction (odd opId)
                __optimalSplitPos = (__optimalSplitPos - 1) | 1;
            }

            Interval __spilledPart = __interval.split(__optimalSplitPos, allocator);
            allocator.assignSpillSlot(__spilledPart);
            handleSpillSlot(__spilledPart);
            changeSpillState(__spilledPart, __optimalSplitPos);

            if (!allocator.isBlockBegin(__optimalSplitPos))
            {
                insertMove(__optimalSplitPos, __interval, __spilledPart);
            }

            // the currentSplitChild is needed later when moves are inserted for reloading
            __spilledPart.makeCurrentSplitChild();
        }
    }

    // called during register allocation
    private void changeSpillState(Interval __interval, int __spillPos)
    {
        switch (__interval.spillState())
        {
            case NoSpillStore:
            {
                int __defLoopDepth = allocator.blockForId(__interval.spillDefinitionPos()).getLoopDepth();
                int __spillLoopDepth = allocator.blockForId(__spillPos).getLoopDepth();

                if (__defLoopDepth < __spillLoopDepth)
                {
                    /*
                     * The loop depth of the spilling position is higher then the loop depth at the
                     * definition of the interval. Move write to memory out of loop.
                     */
                    if (GraalOptions.lirOptLSRAOptimizeSpillPosition)
                    {
                        // find best spill position in dominator the tree
                        __interval.setSpillState(SpillState.SpillInDominator);
                    }
                    else
                    {
                        // store at definition of the interval
                        __interval.setSpillState(SpillState.StoreAtDefinition);
                    }
                }
                else
                {
                    /*
                     * The interval is currently spilled only once, so for now there is no reason to
                     * store the interval at the definition.
                     */
                    __interval.setSpillState(SpillState.OneSpillStore);
                }
                break;
            }

            case OneSpillStore:
            {
                int __defLoopDepth = allocator.blockForId(__interval.spillDefinitionPos()).getLoopDepth();
                int __spillLoopDepth = allocator.blockForId(__spillPos).getLoopDepth();

                if (__defLoopDepth <= __spillLoopDepth)
                {
                    if (GraalOptions.lirOptLSRAOptimizeSpillPosition)
                    {
                        // the interval is spilled more then once
                        __interval.setSpillState(SpillState.SpillInDominator);
                    }
                    else
                    {
                        // It is better to store it to memory at the definition.
                        __interval.setSpillState(SpillState.StoreAtDefinition);
                    }
                }
                break;
            }

            case SpillInDominator:
            case StoreAtDefinition:
            case StartInMemory:
            case NoOptimization:
            case NoDefinitionFound:
                // nothing to do
                break;

            default:
                throw GraalError.shouldNotReachHere("other states not allowed at this time");
        }
    }

    /**
     * This is called for every interval that is assigned to a stack slot.
     */
    protected void handleSpillSlot(Interval __interval)
    {
        // Do nothing. Stack slots are not processed in this implementation.
    }

    void splitStackInterval(Interval __interval)
    {
        int __minSplitPos = currentPosition + 1;
        int __maxSplitPos = Math.min(__interval.firstUsage(RegisterPriority.ShouldHaveRegister), __interval.to());

        splitBeforeUsage(__interval, __minSplitPos, __maxSplitPos);
    }

    void splitWhenPartialRegisterAvailable(Interval __interval, int __registerAvailableUntil)
    {
        int __minSplitPos = Math.max(__interval.previousUsage(RegisterPriority.ShouldHaveRegister, __registerAvailableUntil), __interval.from() + 1);
        splitBeforeUsage(__interval, __minSplitPos, __registerAvailableUntil);
    }

    void splitAndSpillInterval(Interval __interval)
    {
        int __currentPos = currentPosition;
        if (__interval.state == State.Inactive)
        {
            // The interval is currently inactive, so no spill slot is needed for now.
            // When the split part is activated, the interval has a new chance to get a register,
            // so in the best case no stack slot is necessary.
            splitBeforeUsage(__interval, __currentPos + 1, __currentPos + 1);
        }
        else
        {
            // Search the position where the interval must have a register and split at the optimal position before.
            // The new created part is added to the unhandled list and will get a register when it is activated.
            int __minSplitPos = __currentPos + 1;
            int __maxSplitPos = Math.min(__interval.nextUsage(RegisterPriority.MustHaveRegister, __minSplitPos), __interval.to());

            splitBeforeUsage(__interval, __minSplitPos, __maxSplitPos);

            splitForSpilling(__interval);
        }
    }

    boolean allocFreeRegister(Interval __interval)
    {
        initUseLists(true);
        freeExcludeActiveFixed();
        freeExcludeActiveAny();
        freeCollectInactiveFixed(__interval);
        freeCollectInactiveAny(__interval);
        // freeCollectUnhandled(fixedKind, cur);

        // usePos contains the start of the next interval that has this register assigned
        // (either as a fixed register or a normal allocated register in the past)
        // only intervals overlapping with cur are processed, non-overlapping invervals can be
        // ignored safely

        Register __hint = null;
        Interval __locationHint = __interval.locationHint(true);
        if (__locationHint != null && __locationHint.location() != null && ValueUtil.isRegister(__locationHint.location()))
        {
            __hint = ValueUtil.asRegister(__locationHint.location());
        }

        // the register must be free at least until this position
        int __regNeededUntil = __interval.from() + 1;
        int __intervalTo = __interval.to();

        boolean __needSplit = false;
        int __splitPos = -1;

        Register __reg = null;
        Register __minFullReg = null;
        Register __maxPartialReg = null;

        for (Register __availableReg : availableRegs)
        {
            int __number = __availableReg.number;
            if (usePos[__number] >= __intervalTo)
            {
                // this register is free for the full interval
                if (__minFullReg == null || __availableReg.equals(__hint) || (usePos[__number] < usePos[__minFullReg.number] && !__minFullReg.equals(__hint)))
                {
                    __minFullReg = __availableReg;
                }
            }
            else if (usePos[__number] > __regNeededUntil)
            {
                // this register is at least free until regNeededUntil
                if (__maxPartialReg == null || __availableReg.equals(__hint) || (usePos[__number] > usePos[__maxPartialReg.number] && !__maxPartialReg.equals(__hint)))
                {
                    __maxPartialReg = __availableReg;
                }
            }
        }

        if (__minFullReg != null)
        {
            __reg = __minFullReg;
        }
        else if (__maxPartialReg != null)
        {
            __needSplit = true;
            __reg = __maxPartialReg;
        }
        else
        {
            return false;
        }

        __splitPos = usePos[__reg.number];
        __interval.assignLocation(__reg.asValue(__interval.kind()));

        if (__needSplit)
        {
            // register not available for full interval, so split it
            splitWhenPartialRegisterAvailable(__interval, __splitPos);
        }
        // only return true if interval is completely assigned
        return true;
    }

    void splitAndSpillIntersectingIntervals(Register __reg)
    {
        for (int __i = 0; __i < spillIntervals[__reg.number].size(); __i++)
        {
            Interval __interval = spillIntervals[__reg.number].get(__i);
            removeFromList(__interval);
            splitAndSpillInterval(__interval);
        }
    }

    // split an Interval and spill it to memory so that cur can be placed in a register
    void allocLockedRegister(Interval __interval)
    {
        // the register must be free at least until this position
        int __firstUsage = __interval.firstUsage(RegisterPriority.MustHaveRegister);
        int __firstShouldHaveUsage = __interval.firstUsage(RegisterPriority.ShouldHaveRegister);
        int __regNeededUntil = Math.min(__firstUsage, __interval.from() + 1);
        int __intervalTo = __interval.to();

        Register __reg;
        Register __ignore;
        /*
         * In the common case we don't spill registers that have _any_ use position that is
         * closer than the next use of the current interval, but if we can't spill the current
         * interval we weaken this strategy and also allow spilling of intervals that have a
         * non-mandatory requirements (no MustHaveRegister use position).
         */
        for (RegisterPriority __registerPriority = RegisterPriority.LiveAtLoopEnd; true; __registerPriority = RegisterPriority.MustHaveRegister)
        {
            // collect current usage of registers
            initUseLists(false);
            spillExcludeActiveFixed();
            // spillBlockUnhandledFixed(cur);
            spillBlockInactiveFixed(__interval);
            spillCollectActiveAny(__registerPriority);
            spillCollectInactiveAny(__interval);

            __reg = null;
            __ignore = __interval.location() != null && ValueUtil.isRegister(__interval.location()) ? ValueUtil.asRegister(__interval.location()) : null;

            for (Register __availableReg : availableRegs)
            {
                int __number = __availableReg.number;
                if (__availableReg.equals(__ignore))
                {
                    // this register must be ignored
                }
                else if (usePos[__number] > __regNeededUntil)
                {
                    if (__reg == null || (usePos[__number] > usePos[__reg.number]))
                    {
                        __reg = __availableReg;
                    }
                }
            }

            int __regUsePos = (__reg == null ? 0 : usePos[__reg.number]);
            if (__regUsePos <= __firstShouldHaveUsage)
            {
                if (__firstUsage <= __interval.from() + 1)
                {
                    if (__registerPriority.equals(RegisterPriority.LiveAtLoopEnd))
                    {
                        /*
                         * Tool of last resort: we can not spill the current interval so we try
                         * to spill an active interval that has a usage but do not require a register.
                         */
                        continue;
                    }
                    // assign a reasonable register and do a bailout in product mode to avoid errors
                    allocator.assignSpillSlot(__interval);
                    throw new /*OutOfRegistersException*/ BailoutException("linear scan: no register found");
                }

                splitAndSpillInterval(__interval);
                return;
            }
            break;
        }

        boolean __needSplit = blockPos[__reg.number] <= __intervalTo;

        int __splitPos = blockPos[__reg.number];

        __interval.assignLocation(__reg.asValue(__interval.kind()));
        if (__needSplit)
        {
            // register not available for full interval : so split it
            splitWhenPartialRegisterAvailable(__interval, __splitPos);
        }

        // perform splitting and spilling for all affected intervals
        splitAndSpillIntersectingIntervals(__reg);
        return;
    }

    boolean noAllocationPossible(Interval __interval)
    {
        if (allocator.callKillsRegisters())
        {
            // fast calculation of intervals that can never get a register because the
            // the next instruction is a call that blocks all registers
            // note: this only works if a call kills all registers

            // check if this interval is the result of a split operation (an interval got a register until this position)
            int __pos = __interval.from();
            if (CodeUtil.isOdd(__pos))
            {
                // the current instruction is a call that blocks all registers
                if (__pos < allocator.maxOpId() && allocator.hasCall(__pos + 1) && __interval.to() > __pos + 1)
                {
                    return true;
                }
            }
        }
        return false;
    }

    void initVarsForAlloc(Interval __interval)
    {
        AllocatableRegisters __allocatableRegisters = allocator.getRegisterAllocationConfig().getAllocatableRegisters(__interval.kind().getPlatformKind());
        availableRegs = __allocatableRegisters.allocatableRegisters;
        minReg = __allocatableRegisters.minRegisterNumber;
        maxReg = __allocatableRegisters.maxRegisterNumber;
    }

    static boolean isMove(LIRInstruction __op, Interval __from, Interval __to)
    {
        if (ValueMoveOp.isValueMoveOp(__op))
        {
            ValueMoveOp __move = ValueMoveOp.asValueMoveOp(__op);
            if (LIRValueUtil.isVariable(__move.getInput()) && LIRValueUtil.isVariable(__move.getResult()))
            {
                return __move.getInput() != null && __move.getInput().equals(__from.operand) && __move.getResult() != null && __move.getResult().equals(__to.operand);
            }
        }
        return false;
    }

    // optimization (especially for phi functions of nested loops):
    // assign same spill slot to non-intersecting intervals
    void combineSpilledIntervals(Interval __interval)
    {
        if (__interval.isSplitChild())
        {
            // optimization is only suitable for split parents
            return;
        }

        Interval __registerHint = __interval.locationHint(false);
        if (__registerHint == null)
        {
            // cur is not the target of a move : otherwise registerHint would be set
            return;
        }

        if (__interval.spillState() != SpillState.NoOptimization || __registerHint.spillState() != SpillState.NoOptimization)
        {
            // combining the stack slots for intervals where spill move optimization is applied
            // is not benefitial and would cause problems
            return;
        }

        int __beginPos = __interval.from();
        int __endPos = __interval.to();
        if (__endPos > allocator.maxOpId() || CodeUtil.isOdd(__beginPos) || CodeUtil.isOdd(__endPos))
        {
            // safety check that lirOpWithId is allowed
            return;
        }

        if (!isMove(allocator.instructionForId(__beginPos), __registerHint, __interval) || !isMove(allocator.instructionForId(__endPos), __interval, __registerHint))
        {
            // cur and registerHint are not connected with two moves
            return;
        }

        Interval __beginHint = __registerHint.getSplitChildAtOpId(__beginPos, LIRInstruction.OperandMode.USE, allocator);
        Interval __endHint = __registerHint.getSplitChildAtOpId(__endPos, LIRInstruction.OperandMode.DEF, allocator);
        if (__beginHint == __endHint || __beginHint.to() != __beginPos || __endHint.from() != __endPos)
        {
            // registerHint must be split : otherwise the re-writing of use positions does not work
            return;
        }

        if (ValueUtil.isRegister(__beginHint.location()))
        {
            // registerHint is not spilled at beginPos : so it would not be benefitial to
            // immediately spill cur
            return;
        }

        // modify intervals such that cur gets the same stack slot as registerHint
        // delete use positions to prevent the intervals to get a register at beginning
        __interval.setSpillSlot(__registerHint.spillSlot());
        __interval.removeFirstUsePos();
        __endHint.removeFirstUsePos();
    }

    // allocate a physical register or memory location to an interval
    @Override
    protected boolean activateCurrent(Interval __interval)
    {
        boolean __result = true;
        final Value __operand = __interval.operand;
        if (__interval.location() != null && LIRValueUtil.isStackSlotValue(__interval.location()))
        {
            // activating an interval that has a stack slot assigned . split it at first use
            // position
            // used for method parameters
            splitStackInterval(__interval);
            __result = false;
        }
        else
        {
            if (__interval.location() == null)
            {
                // interval has not assigned register . normal allocation
                // (this is the normal case for most intervals)

                // assign same spill slot to non-intersecting intervals
                combineSpilledIntervals(__interval);

                initVarsForAlloc(__interval);
                if (noAllocationPossible(__interval) || !allocFreeRegister(__interval))
                {
                    // no empty register available.
                    // split and spill another interval so that this interval gets a register
                    allocLockedRegister(__interval);
                }

                // spilled intervals need not be move to active-list
                if (!ValueUtil.isRegister(__interval.location()))
                {
                    __result = false;
                }
            }
        }

        // load spilled values that become active from stack slot to register
        if (__interval.insertMoveWhenActivated())
        {
            insertMove(__interval.from(), __interval.currentSplitChild(), __interval);
        }
        __interval.makeCurrentSplitChild();

        return __result; // true = interval is moved to active list
    }

    public void finishAllocation()
    {
        // must be called when all intervals are allocated
        moveResolver.resolveAndAppendMoves();
    }
}
