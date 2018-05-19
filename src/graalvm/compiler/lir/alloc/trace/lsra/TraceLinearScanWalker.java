package graalvm.compiler.lir.alloc.trace.lsra;

import static jdk.vm.ci.code.CodeUtil.isOdd;
import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig.AllocatableRegisters;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.core.common.util.Util;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRValueUtil;
import graalvm.compiler.lir.StandardOp.BlockEndOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.StandardOp.ValueMoveOp;
import graalvm.compiler.lir.alloc.OutOfRegistersException;
import graalvm.compiler.lir.alloc.trace.lsra.TraceInterval.RegisterPriority;
import graalvm.compiler.lir.alloc.trace.lsra.TraceInterval.SpillState;
import graalvm.compiler.lir.alloc.trace.lsra.TraceInterval.State;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.ssa.SSAUtil;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.Value;

/**
 */
final class TraceLinearScanWalker
{
    /**
     * Adds an interval to a list sorted by {@linkplain FixedInterval#currentFrom() current from}
     * positions.
     *
     * @param list the list
     * @param interval the interval to add
     * @return the new head of the list
     */
    private static FixedInterval addToListSortedByCurrentFromPositions(FixedInterval list, FixedInterval interval)
    {
        FixedInterval prev = null;
        FixedInterval cur = list;
        while (cur.currentFrom() < interval.currentFrom())
        {
            prev = cur;
            cur = cur.next;
        }
        FixedInterval result = list;
        if (prev == null)
        {
            // add to head of list
            result = interval;
        }
        else
        {
            // add before 'cur'
            prev.next = interval;
        }
        interval.next = cur;
        return result;
    }

    /**
     * Adds an interval to a list sorted by {@linkplain TraceInterval#from() current from}
     * positions.
     *
     * @param list the list
     * @param interval the interval to add
     * @return the new head of the list
     */
    private static TraceInterval addToListSortedByFromPositions(TraceInterval list, TraceInterval interval)
    {
        TraceInterval prev = null;
        TraceInterval cur = list;
        while (cur.from() < interval.from())
        {
            prev = cur;
            cur = cur.next;
        }
        TraceInterval result = list;
        if (prev == null)
        {
            // add to head of list
            result = interval;
        }
        else
        {
            // add before 'cur'
            prev.next = interval;
        }
        interval.next = cur;
        return result;
    }

    /**
     * Adds an interval to a list sorted by {@linkplain TraceInterval#from() start} positions and
     * {@linkplain TraceInterval#firstUsage(RegisterPriority) first usage} positions.
     *
     * @param list the list
     * @param interval the interval to add
     * @return the new head of the list
     */
    private static TraceInterval addToListSortedByStartAndUsePositions(TraceInterval list, TraceInterval interval)
    {
        TraceInterval newHead = list;
        TraceInterval prev = null;
        TraceInterval cur = newHead;
        while (cur.from() < interval.from() || (cur.from() == interval.from() && cur.firstUsage(RegisterPriority.None) < interval.firstUsage(RegisterPriority.None)))
        {
            prev = cur;
            cur = cur.next;
        }
        if (prev == null)
        {
            newHead = interval;
        }
        else
        {
            prev.next = interval;
        }
        interval.next = cur;
        return newHead;
    }

    /**
     * Removes an interval from a list.
     *
     * @param list the list
     * @param interval the interval to remove
     * @return the new head of the list
     */
    private static TraceInterval removeAny(TraceInterval list, TraceInterval interval)
    {
        TraceInterval newHead = list;
        TraceInterval prev = null;
        TraceInterval cur = newHead;
        while (cur != interval)
        {
            prev = cur;
            cur = cur.next;
        }
        if (prev == null)
        {
            newHead = cur.next;
        }
        else
        {
            prev.next = cur.next;
        }
        return newHead;
    }

    private Register[] availableRegs;

    private final int[] usePos;
    private final int[] blockPos;
    private final BitSet isInMemory;

    private final ArrayList<TraceInterval>[] spillIntervals;

    private TraceLocalMoveResolver moveResolver; // for ordering spill moves

    private int minReg;

    private int maxReg;

    private final TraceLinearScan allocator;

    /**
     * Sorted list of intervals, not live before the current position.
     */
    private TraceInterval unhandledAnyList;

    /**
     * Sorted list of intervals, live at the current position.
     */
    private TraceInterval activeAnyList;

    private FixedInterval activeFixedList;

    /**
     * Sorted list of intervals in a life time hole at the current position.
     */
    private FixedInterval inactiveFixedList;

    /**
     * The current position (intercept point through the intervals).
     */
    private int currentPosition;

    /**
     * Only 10% of the lists in {@link #spillIntervals} are actually used. But when they are used,
     * they can grow quite long. The maximum length observed was 45 (all numbers taken from a
     * bootstrap run of Graal). Therefore, we initialize {@link #spillIntervals} with this marker
     * value, and allocate a "real" list only on demand in {@link #setUsePos}.
     */
    private static final ArrayList<TraceInterval> EMPTY_LIST = new ArrayList<>(0);

    // accessors mapped to same functions in class LinearScan
    private int blockCount()
    {
        return allocator.blockCount();
    }

    private AbstractBlockBase<?> blockAt(int idx)
    {
        return allocator.blockAt(idx);
    }

    @SuppressWarnings("unused")
    private AbstractBlockBase<?> blockOfOpWithId(int opId)
    {
        return allocator.blockForId(opId);
    }

    TraceLinearScanWalker(TraceLinearScan allocator, FixedInterval unhandledFixedFirst, TraceInterval unhandledAnyFirst)
    {
        this.allocator = allocator;

        unhandledAnyList = unhandledAnyFirst;
        activeAnyList = TraceInterval.EndMarker;
        activeFixedList = FixedInterval.EndMarker;
        // we don't need a separate unhandled list for fixed.
        inactiveFixedList = unhandledFixedFirst;
        currentPosition = -1;

        moveResolver = allocator.createMoveResolver();
        int numRegs = allocator.getRegisters().size();
        spillIntervals = Util.uncheckedCast(new ArrayList<?>[numRegs]);
        for (int i = 0; i < numRegs; i++)
        {
            spillIntervals[i] = EMPTY_LIST;
        }
        usePos = new int[numRegs];
        blockPos = new int[numRegs];
        isInMemory = new BitSet(numRegs);
    }

    private void initUseLists(boolean onlyProcessUsePos)
    {
        for (Register register : availableRegs)
        {
            int i = register.number;
            usePos[i] = Integer.MAX_VALUE;

            if (!onlyProcessUsePos)
            {
                blockPos[i] = Integer.MAX_VALUE;
                spillIntervals[i].clear();
                isInMemory.clear(i);
            }
        }
    }

    private int maxRegisterNumber()
    {
        return maxReg;
    }

    private int minRegisterNumber()
    {
        return minReg;
    }

    private boolean isRegisterInRange(int reg)
    {
        return reg >= minRegisterNumber() && reg <= maxRegisterNumber();
    }

    private void excludeFromUse(IntervalHint i)
    {
        Value location = i.location();
        int i1 = asRegister(location).number;
        if (isRegisterInRange(i1))
        {
            usePos[i1] = 0;
        }
    }

    private void setUsePos(TraceInterval interval, int usePos, boolean onlyProcessUsePos)
    {
        if (usePos != -1)
        {
            int i = asRegister(interval.location()).number;
            if (isRegisterInRange(i))
            {
                if (this.usePos[i] > usePos)
                {
                    this.usePos[i] = usePos;
                }
                if (!onlyProcessUsePos)
                {
                    ArrayList<TraceInterval> list = spillIntervals[i];
                    if (list == EMPTY_LIST)
                    {
                        list = new ArrayList<>(2);
                        spillIntervals[i] = list;
                    }
                    list.add(interval);
                    // set is in memory flag
                    if (interval.inMemoryAt(currentPosition))
                    {
                        isInMemory.set(i);
                    }
                }
            }
        }
    }

    private void setUsePos(FixedInterval interval, int usePos, boolean onlyProcessUsePos)
    {
        if (usePos != -1)
        {
            int i = asRegister(interval.location()).number;
            if (isRegisterInRange(i))
            {
                if (this.usePos[i] > usePos)
                {
                    this.usePos[i] = usePos;
                }
            }
        }
    }

    private void setBlockPos(IntervalHint i, int blockPos)
    {
        if (blockPos != -1)
        {
            int reg = asRegister(i.location()).number;
            if (isRegisterInRange(reg))
            {
                if (this.blockPos[reg] > blockPos)
                {
                    this.blockPos[reg] = blockPos;
                }
                if (usePos[reg] > blockPos)
                {
                    usePos[reg] = blockPos;
                }
            }
        }
    }

    private void freeExcludeActiveFixed()
    {
        FixedInterval interval = activeFixedList;
        while (interval != FixedInterval.EndMarker)
        {
            excludeFromUse(interval);
            interval = interval.next;
        }
    }

    private void freeExcludeActiveAny()
    {
        TraceInterval interval = activeAnyList;
        while (interval != TraceInterval.EndMarker)
        {
            excludeFromUse(interval);
            interval = interval.next;
        }
    }

    private void freeCollectInactiveFixed(TraceInterval current)
    {
        FixedInterval interval = inactiveFixedList;
        while (interval != FixedInterval.EndMarker)
        {
            if (current.to() <= interval.from())
            {
                setUsePos(interval, interval.from(), true);
            }
            else
            {
                setUsePos(interval, interval.currentIntersectsAt(current), true);
            }
            interval = interval.next;
        }
    }

    private void spillExcludeActiveFixed()
    {
        FixedInterval interval = activeFixedList;
        while (interval != FixedInterval.EndMarker)
        {
            excludeFromUse(interval);
            interval = interval.next;
        }
    }

    private void spillBlockInactiveFixed(TraceInterval current)
    {
        FixedInterval interval = inactiveFixedList;
        while (interval != FixedInterval.EndMarker)
        {
            if (current.to() > interval.currentFrom())
            {
                setBlockPos(interval, interval.currentIntersectsAt(current));
            }

            interval = interval.next;
        }
    }

    private void spillCollectActiveAny(RegisterPriority registerPriority)
    {
        TraceInterval interval = activeAnyList;
        while (interval != TraceInterval.EndMarker)
        {
            setUsePos(interval, Math.min(interval.nextUsage(registerPriority, currentPosition), interval.to()), false);
            interval = interval.next;
        }
    }

    @SuppressWarnings("unused")
    private int insertIdAtBasicBlockBoundary(int opId)
    {
        AbstractBlockBase<?> toBlock = allocator.blockForId(opId);
        AbstractBlockBase<?> fromBlock = allocator.blockForId(opId - 2);

        if (fromBlock.getSuccessorCount() == 1)
        {
            // insert move in predecessor
            return opId - 2;
        }
        // insert move in successor
        return opId + 2;
    }

    private void insertMove(int operandId, TraceInterval srcIt, TraceInterval dstIt)
    {
        // output all moves here. When source and target are equal, the move is
        // optimized away later in assignRegNums

        int opId = (operandId + 1) & ~1;
        AbstractBlockBase<?> opBlock = allocator.blockForId(opId);

        // calculate index of instruction inside instruction list of current block
        // the minimal index (for a block with no spill moves) can be calculated because the
        // numbering of instructions is known.
        // When the block already contains spill moves, the index must be increased until the
        // correct index is reached.
        ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(opBlock);
        int index = (opId - instructions.get(0).id()) >> 1;

        while (instructions.get(index).id() != opId)
        {
            index++;
        }

        // insert new instruction before instruction at position index
        moveResolver.moveInsertPosition(instructions, index);
        moveResolver.addMapping(srcIt, dstIt);
    }

    private int findOptimalSplitPos(AbstractBlockBase<?> minBlock, AbstractBlockBase<?> maxBlock, int maxSplitPos)
    {
        int fromBlockNr = minBlock.getLinearScanNumber();
        int toBlockNr = maxBlock.getLinearScanNumber();

        // Try to split at end of maxBlock. If this would be after
        // maxSplitPos, then use the begin of maxBlock
        int optimalSplitPos = allocator.getLastLirInstructionId(maxBlock) + 2;
        if (optimalSplitPos > maxSplitPos)
        {
            optimalSplitPos = allocator.getFirstLirInstructionId(maxBlock);
        }

        // minimal block probability
        double minProbability = maxBlock.probability();
        for (int i = toBlockNr - 1; i >= fromBlockNr; i--)
        {
            AbstractBlockBase<?> cur = blockAt(i);

            if (cur.probability() < minProbability)
            {
                // Block with lower probability found. Split at the end of this block.
                minProbability = cur.probability();
                optimalSplitPos = allocator.getLastLirInstructionId(cur) + 2;
            }
        }

        return optimalSplitPos;
    }

    @SuppressWarnings({"unused"})
    private int findOptimalSplitPos(TraceInterval interval, int minSplitPos, int maxSplitPos, boolean doLoopOptimization)
    {
        int optimalSplitPos = findOptimalSplitPos0(minSplitPos, maxSplitPos);
        return optimalSplitPos;
    }

    private int findOptimalSplitPos0(int minSplitPos, int maxSplitPos)
    {
        if (minSplitPos == maxSplitPos)
        {
            // trivial case, no optimization of split position possible
            return minSplitPos;
        }

        // reason for using minSplitPos - 1: when the minimal split pos is exactly at the
        // beginning of a block, then minSplitPos is also a possible split position.
        // Use the block before as minBlock, because then minBlock.lastLirInstructionId() + 2 ==
        // minSplitPos
        AbstractBlockBase<?> minBlock = allocator.blockForId(minSplitPos - 1);

        // reason for using maxSplitPos - 1: otherwise there would be an assert on failure
        // when an interval ends at the end of the last block of the method
        // (in this case, maxSplitPos == allocator().maxLirOpId() + 2, and there is no
        // block at this opId)
        AbstractBlockBase<?> maxBlock = allocator.blockForId(maxSplitPos - 1);

        if (minBlock == maxBlock)
        {
            // split position cannot be moved to block boundary : so split as late as possible
            return maxSplitPos;
        }
        // seach optimal block boundary between minSplitPos and maxSplitPos

        return findOptimalSplitPos(minBlock, maxBlock, maxSplitPos);
    }

    // split an interval at the optimal position between minSplitPos and
    // maxSplitPos in two parts:
    // 1) the left part has already a location assigned
    // 2) the right part is sorted into to the unhandled-list
    private void splitBeforeUsage(TraceInterval interval, int minSplitPos, int maxSplitPos)
    {
        final int optimalSplitPos = findOptimalSplitPos(interval, minSplitPos, maxSplitPos, true);

        if (optimalSplitPos == interval.to() && interval.nextUsage(RegisterPriority.MustHaveRegister, minSplitPos) == Integer.MAX_VALUE)
        {
            // the split position would be just before the end of the interval
            // . no split at all necessary
            return;
        }
        // must calculate this before the actual split is performed and before split position is
        // moved to odd opId
        final int optimalSplitPosFinal;
        boolean blockBegin = allocator.isBlockBegin(optimalSplitPos);
        boolean moveNecessary = !blockBegin;
        if (blockBegin)
        {
            optimalSplitPosFinal = optimalSplitPos;
        }
        else
        {
            // move position before actual instruction (odd opId)
            optimalSplitPosFinal = (optimalSplitPos - 1) | 1;
        }

        // TODO (je) duplicate code. try to fold
        if (optimalSplitPosFinal == interval.to() && interval.nextUsage(RegisterPriority.MustHaveRegister, minSplitPos) == Integer.MAX_VALUE)
        {
            // the split position would be just before the end of the interval
            // . no split at all necessary
            return;
        }
        TraceInterval splitPart = interval.split(optimalSplitPosFinal, allocator);

        splitPart.setInsertMoveWhenActivated(moveNecessary);

        unhandledAnyList = TraceLinearScanWalker.addToListSortedByStartAndUsePositions(unhandledAnyList, splitPart);
    }

    // split an interval at the optimal position between minSplitPos and
    // maxSplitPos in two parts:
    // 1) the left part has already a location assigned
    // 2) the right part is always on the stack and therefore ignored in further processing
    private void splitForSpilling(TraceInterval interval)
    {
        // calculate allowed range of splitting position
        int maxSplitPos = currentPosition;
        int previousUsage = interval.previousUsage(RegisterPriority.ShouldHaveRegister, maxSplitPos);
        if (previousUsage == currentPosition)
        {
            /*
             * If there is a usage with ShouldHaveRegister priority at the current position fall
             * back to MustHaveRegister priority. This only happens if register priority was
             * downgraded to MustHaveRegister in #allocLockedRegister.
             */
            previousUsage = interval.previousUsage(RegisterPriority.MustHaveRegister, maxSplitPos);
        }
        int minSplitPos = Math.max(previousUsage + 1, interval.from());

        if (minSplitPos == interval.from())
        {
            // the whole interval is never used, so spill it entirely to memory

            allocator.assignSpillSlot(interval);
            handleSpillSlot(interval);
            changeSpillState(interval, minSplitPos);

            // Also kick parent intervals out of register to memory when they have no use
            // position. This avoids short interval in register surrounded by intervals in
            // memory . avoid useless moves from memory to register and back
            TraceInterval parent = interval;
            while (parent != null && parent.isSplitChild())
            {
                parent = parent.getSplitChildBeforeOpId(parent.from());

                if (isRegister(parent.location()))
                {
                    if (parent.firstUsage(RegisterPriority.ShouldHaveRegister) == Integer.MAX_VALUE)
                    {
                        // parent is never used, so kick it out of its assigned register
                        allocator.assignSpillSlot(parent);
                        handleSpillSlot(parent);
                    }
                    else
                    {
                        // do not go further back because the register is actually used by
                        // the interval
                        parent = null;
                    }
                }
            }
        }
        else
        {
            // search optimal split pos, split interval and spill only the right hand part
            int optimalSplitPos = findOptimalSplitPos(interval, minSplitPos, maxSplitPos, false);

            if (!allocator.isBlockBegin(optimalSplitPos))
            {
                // move position before actual instruction (odd opId)
                optimalSplitPos = (optimalSplitPos - 1) | 1;
            }

            TraceInterval spilledPart = interval.split(optimalSplitPos, allocator);
            allocator.assignSpillSlot(spilledPart);
            handleSpillSlot(spilledPart);
            changeSpillState(spilledPart, optimalSplitPos);

            if (!allocator.isBlockBegin(optimalSplitPos))
            {
                insertMove(optimalSplitPos, interval, spilledPart);
            }

            // the currentSplitChild is needed later when moves are inserted for reloading
            spilledPart.makeCurrentSplitChild();
        }
    }

    /**
     * Change spill state of an interval.
     *
     * Note: called during register allocation.
     *
     * @param spillPos position of the spill
     */
    private void changeSpillState(TraceInterval interval, int spillPos)
    {
        if (TraceLinearScanPhase.Options.LIROptTraceRAEliminateSpillMoves.getValue(allocator.getOptions()))
        {
            switch (interval.spillState())
            {
                case NoSpillStore:
                    final int minSpillPos = calculateMinSpillPos(interval.spillDefinitionPos(), spillPos);
                    final int maxSpillPos = calculateMaxSpillPos(minSpillPos, spillPos);

                    final int optimalSpillPos = findOptimalSpillPos(minSpillPos, maxSpillPos);

                    /* Cannot spill at block begin since it interferes with move resolution. */

                    interval.setSpillDefinitionPos(optimalSpillPos);
                    interval.setSpillState(SpillState.SpillStore);
                    break;
                case SpillStore:
                case StartInMemory:
                case NoOptimization:
                case NoDefinitionFound:
                    // nothing to do
                    break;

                default:
                    throw GraalError.shouldNotReachHere("other states not allowed at this time");
            }
        }
        else
        {
            interval.setSpillState(SpillState.NoOptimization);
        }
    }

    private int calculateMinSpillPos(int spillDefinitionPos, int spillPos)
    {
        int spillDefinitionPosEven = spillDefinitionPos & ~1;
        if (spillDefinitionPosEven == 0 || !allocator.isBlockBegin(spillDefinitionPosEven) || spillDefinitionPos == spillPos)
        {
            return spillDefinitionPos;
        }
        if (SSAUtil.isMerge(allocator.blockForId(spillDefinitionPos)))
        {
            /* Spill at merge are OK since there will be no resolution moves. */
            return spillDefinitionPos;
        }
        int minSpillPos = spillDefinitionPosEven + 2;
        while (allocator.isBlockEnd(minSpillPos))
        {
            // +2 is block begin, +4 is the instruction afterwards
            minSpillPos += 4;
        }
        return minSpillPos;
    }

    private int calculateMaxSpillPos(final int minSpillPos, int spillPos)
    {
        int spillPosEven = spillPos & ~1;
        if (spillPosEven == 0)
        {
            return spillPos;
        }
        if ((minSpillPos & ~1) == spillPosEven)
        {
            return spillPos;
        }
        int maxSpillPos;
        /* Move away from block end. */
        if (allocator.isBlockEnd(spillPosEven))
        {
            /* Block end. Use instruction before. */
            maxSpillPos = spillPosEven - 2;
        }
        else if (allocator.isBlockBegin(spillPosEven))
        {
            /* Block begin. Use instruction before previous block end. */
            maxSpillPos = spillPosEven - 4;
        }
        else
        {
            return spillPos;
        }

        /* Skip block begins. */
        while (allocator.isBlockBegin(maxSpillPos) && maxSpillPos > minSpillPos)
        {
            // -2 is block end, -4 is the instruction before
            maxSpillPos -= 4;
        }
        return maxSpillPos;
    }

    private boolean isNotBlockBeginOrMerge(int spillPos)
    {
        int spillPosEven = spillPos & ~1;
        return spillPosEven == 0 || !allocator.isBlockBegin(spillPosEven) || SSAUtil.isMerge(allocator.blockForId(spillPosEven));
    }

    /**
     * @param minSpillPos minimal spill position
     * @param maxSpillPos maximal spill position
     */
    private int findOptimalSpillPos(int minSpillPos, int maxSpillPos)
    {
        int optimalSpillPos = findOptimalSpillPos0(minSpillPos, maxSpillPos) & (~1);
        return optimalSpillPos;
    }

    private int findOptimalSpillPos0(int minSpillPos, int maxSpillPos)
    {
        if (minSpillPos == maxSpillPos)
        {
            // trivial case, no optimization of split position possible
            return minSpillPos;
        }

        AbstractBlockBase<?> minBlock = allocator.blockForId(minSpillPos);
        AbstractBlockBase<?> maxBlock = allocator.blockForId(maxSpillPos);

        if (minBlock == maxBlock)
        {
            // split position cannot be moved to block boundary : so split as late as possible
            return maxSpillPos;
        }
        // search optimal block boundary between minSplitPos and maxSplitPos

        // currently using the same heuristic as for splitting
        return findOptimalSpillPos(minBlock, maxBlock, maxSpillPos);
    }

    private int findOptimalSpillPos(AbstractBlockBase<?> minBlock, AbstractBlockBase<?> maxBlock, int maxSplitPos)
    {
        int fromBlockNr = minBlock.getLinearScanNumber();
        int toBlockNr = maxBlock.getLinearScanNumber();

        /*
         * Try to split at end of maxBlock. We use last instruction -2 because we want to insert the
         * move before the block end op. If this would be after maxSplitPos, then use the
         * maxSplitPos.
         */
        int optimalSplitPos = allocator.getLastLirInstructionId(maxBlock) - 2;
        if (optimalSplitPos > maxSplitPos)
        {
            optimalSplitPos = maxSplitPos;
        }

        // minimal block probability
        double minProbability = maxBlock.probability();
        for (int i = toBlockNr - 1; i >= fromBlockNr; i--)
        {
            AbstractBlockBase<?> cur = blockAt(i);

            if (cur.probability() < minProbability)
            {
                // Block with lower probability found. Split at the end of this block.
                int opIdBeforeBlockEnd = allocator.getLastLirInstructionId(cur) - 2;
                if (allocator.getLIR().getLIRforBlock(cur).size() > 2)
                {
                    minProbability = cur.probability();
                    optimalSplitPos = opIdBeforeBlockEnd;
                }
                else
                {
                    // Skip blocks with only LabelOp and BlockEndOp since they cause move ordering problems.
                }
            }
        }
        return optimalSplitPos;
    }

    /**
     * This is called for every interval that is assigned to a stack slot.
     */
    private static void handleSpillSlot(TraceInterval interval)
    {
        // Do nothing. Stack slots are not processed in this implementation.
    }

    private void splitStackInterval(TraceInterval interval)
    {
        int minSplitPos = currentPosition + 1;
        int maxSplitPos = Math.min(interval.firstUsage(RegisterPriority.ShouldHaveRegister), interval.to());

        splitBeforeUsage(interval, minSplitPos, maxSplitPos);
    }

    private void splitWhenPartialRegisterAvailable(TraceInterval interval, int registerAvailableUntil)
    {
        int minSplitPos = Math.max(interval.previousUsage(RegisterPriority.ShouldHaveRegister, registerAvailableUntil), interval.from() + 1);
        splitBeforeUsage(interval, minSplitPos, registerAvailableUntil);
    }

    private void splitAndSpillInterval(TraceInterval interval)
    {
        int currentPos = currentPosition;
        /*
         * Search the position where the interval must have a register and split at the optimal
         * position before. The new created part is added to the unhandled list and will get a
         * register when it is activated.
         */
        int minSplitPos = currentPos + 1;
        int maxSplitPos = interval.nextUsage(RegisterPriority.MustHaveRegister, minSplitPos);

        if (maxSplitPos <= interval.to())
        {
            splitBeforeUsage(interval, minSplitPos, maxSplitPos);
        }

        splitForSpilling(interval);
    }

    private boolean allocFreeRegister(TraceInterval interval)
    {
        initUseLists(true);
        freeExcludeActiveFixed();
        freeCollectInactiveFixed(interval);
        freeExcludeActiveAny();
        // freeCollectUnhandled(fixedKind, cur);

        // usePos contains the start of the next interval that has this register assigned
        // (either as a fixed register or a normal allocated register in the past)
        // only intervals overlapping with cur are processed, non-overlapping invervals can be
        // ignored safely

        Register hint = null;
        IntervalHint locationHint = interval.locationHint(true);
        if (locationHint != null && locationHint.location() != null && isRegister(locationHint.location()))
        {
            hint = asRegister(locationHint.location());
        }

        // the register must be free at least until this position
        int regNeededUntil = interval.from() + 1;
        int intervalTo = interval.to();

        boolean needSplit = false;
        int splitPos = -1;

        Register reg = null;
        Register minFullReg = null;
        Register maxPartialReg = null;

        for (Register availableReg : availableRegs)
        {
            int number = availableReg.number;
            if (usePos[number] >= intervalTo)
            {
                // this register is free for the full interval
                if (minFullReg == null || availableReg.equals(hint) || (usePos[number] < usePos[minFullReg.number] && !minFullReg.equals(hint)))
                {
                    minFullReg = availableReg;
                }
            }
            else if (usePos[number] > regNeededUntil)
            {
                // this register is at least free until regNeededUntil
                if (maxPartialReg == null || availableReg.equals(hint) || (usePos[number] > usePos[maxPartialReg.number] && !maxPartialReg.equals(hint)))
                {
                    maxPartialReg = availableReg;
                }
            }
        }

        if (minFullReg != null)
        {
            reg = minFullReg;
        }
        else if (maxPartialReg != null)
        {
            needSplit = true;
            reg = maxPartialReg;
        }
        else
        {
            return false;
        }

        splitPos = usePos[reg.number];
        interval.assignLocation(reg.asValue(allocator.getKind(interval)));

        if (needSplit)
        {
            // register not available for full interval, so split it
            splitWhenPartialRegisterAvailable(interval, splitPos);
        }
        // only return true if interval is completely assigned
        return true;
    }

    private void splitAndSpillIntersectingIntervals(Register reg)
    {
        for (int i = 0; i < spillIntervals[reg.number].size(); i++)
        {
            TraceInterval interval = spillIntervals[reg.number].get(i);
            removeFromList(interval);
            splitAndSpillInterval(interval);
        }
    }

    // Split an Interval and spill it to memory so that cur can be placed in a register
    private void allocLockedRegister(TraceInterval interval)
    {
        // the register must be free at least until this position
        int firstUsage = interval.firstUsage(RegisterPriority.MustHaveRegister);
        int firstShouldHaveUsage = interval.firstUsage(RegisterPriority.ShouldHaveRegister);
        int regNeededUntil = Math.min(firstUsage, interval.from() + 1);
        int intervalTo = interval.to();

        Register reg;
        Register ignore;
        /*
         * In the common case we don't spill registers that have _any_ use position that is
         * closer than the next use of the current interval, but if we can't spill the current
         * interval we weaken this strategy and also allow spilling of intervals that have a
         * non-mandatory requirements (no MustHaveRegister use position).
         */
        for (RegisterPriority registerPriority = RegisterPriority.LiveAtLoopEnd; true; registerPriority = RegisterPriority.MustHaveRegister)
        {
            // collect current usage of registers
            initUseLists(false);
            spillExcludeActiveFixed();
            // spillBlockUnhandledFixed(cur);
            spillBlockInactiveFixed(interval);
            spillCollectActiveAny(registerPriority);

            reg = null;
            ignore = interval.location() != null && isRegister(interval.location()) ? asRegister(interval.location()) : null;

            for (Register availableReg : availableRegs)
            {
                int number = availableReg.number;
                if (availableReg.equals(ignore))
                {
                    // this register must be ignored
                }
                else if (usePos[number] > regNeededUntil)
                {
                    /*
                     * If the use position is the same, prefer registers (active intervals)
                     * where the value is already on the stack.
                     */
                    if (reg == null || (usePos[number] > usePos[reg.number]) || (usePos[number] == usePos[reg.number] && (!isInMemory.get(reg.number) && isInMemory.get(number))))
                    {
                        reg = availableReg;
                    }
                }
            }

            int regUsePos = (reg == null ? 0 : usePos[reg.number]);
            if (regUsePos <= firstShouldHaveUsage)
            {
                /* Check if there is another interval that is already in memory. */
                if (reg == null || interval.inMemoryAt(currentPosition) || !isInMemory.get(reg.number))
                {
                    if (firstUsage <= interval.from() + 1)
                    {
                        if (registerPriority.equals(RegisterPriority.LiveAtLoopEnd))
                        {
                            /*
                             * Tool of last resort: we can not spill the current interval so we
                             * try to spill an active interval that has a usage but do not
                             * require a register.
                             */
                            continue;
                        }
                        String description = "cannot spill interval (" + interval + ") that is used in first instruction (possible reason: no register found) firstUsage=" + firstUsage + ", interval.from()=" + interval.from() + "; already used candidates: " + Arrays.toString(availableRegs);
                        /*
                         * assign a reasonable register and do a bailout in product mode to
                         * avoid errors
                         */
                        allocator.assignSpillSlot(interval);
                        throw new OutOfRegistersException("LinearScan: no register found", description);
                    }

                    splitAndSpillInterval(interval);
                    return;
                }
            }
            // common case: break out of the loop
            break;
        }

        boolean needSplit = blockPos[reg.number] <= intervalTo;

        int splitPos = blockPos[reg.number];

        interval.assignLocation(reg.asValue(allocator.getKind(interval)));
        if (needSplit)
        {
            // register not available for full interval : so split it
            splitWhenPartialRegisterAvailable(interval, splitPos);
        }

        // perform splitting and spilling for all affected intervals
        splitAndSpillIntersectingIntervals(reg);
        return;
    }

    private boolean noAllocationPossible(TraceInterval interval)
    {
        if (allocator.callKillsRegisters())
        {
            // fast calculation of intervals that can never get a register because the
            // the next instruction is a call that blocks all registers
            // Note: this only works if a call kills all registers

            // check if this interval is the result of a split operation
            // (an interval got a register until this position)
            int pos = interval.from();
            if (isOdd(pos))
            {
                // the current instruction is a call that blocks all registers
                if (pos < allocator.maxOpId() && allocator.hasCall(pos + 1) && interval.to() > pos + 1)
                {
                    // safety check that there is really no register available
                    return true;
                }
            }
        }
        return false;
    }

    private void initVarsForAlloc(TraceInterval interval)
    {
        AllocatableRegisters allocatableRegisters = allocator.getRegisterAllocationConfig().getAllocatableRegisters(allocator.getKind(interval).getPlatformKind());
        availableRegs = allocatableRegisters.allocatableRegisters;
        minReg = allocatableRegisters.minRegisterNumber;
        maxReg = allocatableRegisters.maxRegisterNumber;
    }

    private static boolean isMove(LIRInstruction op, TraceInterval from, TraceInterval to)
    {
        if (ValueMoveOp.isValueMoveOp(op))
        {
            ValueMoveOp move = ValueMoveOp.asValueMoveOp(op);
            if (isVariable(move.getInput()) && isVariable(move.getResult()))
            {
                return move.getInput() != null && LIRValueUtil.asVariable(move.getInput()).index == from.operandNumber && move.getResult() != null && LIRValueUtil.asVariable(move.getResult()).index == to.operandNumber;
            }
        }
        return false;
    }

    // optimization (especially for phi functions of nested loops):
    // assign same spill slot to non-intersecting intervals
    private void combineSpilledIntervals(TraceInterval interval)
    {
        if (interval.isSplitChild())
        {
            // optimization is only suitable for split parents
            return;
        }

        IntervalHint locationHint = interval.locationHint(false);
        if (locationHint == null || !(locationHint instanceof TraceInterval))
        {
            return;
        }
        TraceInterval registerHint = (TraceInterval) locationHint;

        if (interval.spillState() != SpillState.NoOptimization || registerHint.spillState() != SpillState.NoOptimization)
        {
            // combining the stack slots for intervals where spill move optimization is applied
            // is not benefitial and would cause problems
            return;
        }

        int beginPos = interval.from();
        int endPos = interval.to();
        if (endPos > allocator.maxOpId() || isOdd(beginPos) || isOdd(endPos))
        {
            // safety check that lirOpWithId is allowed
            return;
        }

        if (!isMove(allocator.instructionForId(beginPos), registerHint, interval) || !isMove(allocator.instructionForId(endPos), interval, registerHint))
        {
            // cur and registerHint are not connected with two moves
            return;
        }

        TraceInterval beginHint = registerHint.getSplitChildAtOpId(beginPos, LIRInstruction.OperandMode.USE);
        TraceInterval endHint = registerHint.getSplitChildAtOpId(endPos, LIRInstruction.OperandMode.DEF);
        if (beginHint == endHint || beginHint.to() != beginPos || endHint.from() != endPos)
        {
            // registerHint must be split : otherwise the re-writing of use positions does not work
            return;
        }

        if (isRegister(beginHint.location()))
        {
            // registerHint is not spilled at beginPos : so it would not be benefitial to
            // immediately spill cur
            return;
        }

        // modify intervals such that cur gets the same stack slot as registerHint
        // delete use positions to prevent the intervals to get a register at beginning
        interval.setSpillSlot(registerHint.spillSlot());
        interval.removeFirstUsePos();
        endHint.removeFirstUsePos();
    }

    // allocate a physical register or memory location to an interval
    private boolean activateCurrent(TraceInterval interval)
    {
        boolean result = true;

        if (interval.location() != null && isStackSlotValue(interval.location()))
        {
            // activating an interval that has a stack slot assigned . split it at first use
            // position
            // used for method parameters
            splitStackInterval(interval);
            result = false;
        }
        else
        {
            if (interval.location() == null)
            {
                // interval has not assigned register . normal allocation
                // (this is the normal case for most intervals)

                // assign same spill slot to non-intersecting intervals
                combineSpilledIntervals(interval);

                initVarsForAlloc(interval);
                if (noAllocationPossible(interval) || !allocFreeRegister(interval))
                {
                    // no empty register available.
                    // split and spill another interval so that this interval gets a register
                    allocLockedRegister(interval);
                }

                // spilled intervals need not be move to active-list
                if (!isRegister(interval.location()))
                {
                    result = false;
                }
            }
        }

        // load spilled values that become active from stack slot to register
        if (interval.insertMoveWhenActivated())
        {
            insertMove(interval.from(), interval.currentSplitChild(), interval);
        }
        interval.makeCurrentSplitChild();

        return result; // true = interval is moved to active list
    }

    void finishAllocation()
    {
        // must be called when all intervals are allocated
        moveResolver.resolveAndAppendMoves();
    }

    void walk()
    {
        walkTo(Integer.MAX_VALUE);
    }

    private void removeFromList(TraceInterval interval)
    {
        activeAnyList = TraceLinearScanWalker.removeAny(activeAnyList, interval);
    }

    /**
     * Walks up to {@code from} and updates the state of {@link FixedInterval fixed intervals}.
     *
     * Fixed intervals can switch back and forth between the states {@link State#Active} and
     * {@link State#Inactive} (and eventually to {@link State#Handled} but handled intervals are not
     * managed).
     */
    @SuppressWarnings("try")
    private void walkToFixed(State state, int from)
    {
        FixedInterval prevprev = null;
        FixedInterval prev = (state == State.Active) ? activeFixedList : inactiveFixedList;
        FixedInterval next = prev;
        while (next.currentFrom() <= from)
        {
            FixedInterval cur = next;
            next = cur.next;

            boolean rangeHasChanged = false;
            while (cur.currentTo() <= from)
            {
                cur.nextRange();
                rangeHasChanged = true;
            }

            // also handle move from inactive list to active list
            rangeHasChanged = rangeHasChanged || (state == State.Inactive && cur.currentFrom() <= from);

            if (rangeHasChanged)
            {
                // remove cur from list
                if (prevprev == null)
                {
                    if (state == State.Active)
                    {
                        activeFixedList = next;
                    }
                    else
                    {
                        inactiveFixedList = next;
                    }
                }
                else
                {
                    prevprev.next = next;
                }
                prev = next;
                TraceInterval.State newState;
                if (cur.currentAtEnd())
                {
                    // move to handled state (not maintained as a list)
                    newState = State.Handled;
                }
                else
                {
                    if (cur.currentFrom() <= from)
                    {
                        // sort into active list
                        activeFixedList = TraceLinearScanWalker.addToListSortedByCurrentFromPositions(activeFixedList, cur);
                        newState = State.Active;
                    }
                    else
                    {
                        // sort into inactive list
                        inactiveFixedList = TraceLinearScanWalker.addToListSortedByCurrentFromPositions(inactiveFixedList, cur);
                        newState = State.Inactive;
                    }
                    if (prev == cur)
                    {
                        prevprev = prev;
                        prev = cur.next;
                    }
                }
                intervalMoved(cur, state, newState);
            }
            else
            {
                prevprev = prev;
                prev = cur.next;
            }
        }
    }

    /**
     * Walks up to {@code from} and updates the state of {@link TraceInterval intervals}.
     *
     * Trace intervals can switch once from {@link State#Unhandled} to {@link State#Active} and then
     * to {@link State#Handled} but handled intervals are not managed.
     */
    @SuppressWarnings("try")
    private void walkToAny(int from)
    {
        TraceInterval prevprev = null;
        TraceInterval prev = activeAnyList;
        TraceInterval next = prev;
        while (next.from() <= from)
        {
            TraceInterval cur = next;
            next = cur.next;

            if (cur.to() <= from)
            {
                // remove cur from list
                if (prevprev == null)
                {
                    activeAnyList = next;
                }
                else
                {
                    prevprev.next = next;
                }
                intervalMoved(cur, State.Active, State.Handled);
            }
            else
            {
                prevprev = prev;
            }
            prev = next;
        }
    }

    /**
     * Get the next interval from {@linkplain #unhandledAnyList} which starts before or at
     * {@code toOpId}. The returned interval is removed.
     *
     * @postcondition all intervals in {@linkplain #unhandledAnyList} start after {@code toOpId}.
     *
     * @return The next interval or null if there is no {@linkplain #unhandledAnyList unhandled}
     *         interval at position {@code toOpId}.
     */
    private TraceInterval nextInterval(int toOpId)
    {
        TraceInterval any = unhandledAnyList;

        if (any != TraceInterval.EndMarker)
        {
            TraceInterval currentInterval = unhandledAnyList;
            if (toOpId < currentInterval.from())
            {
                return null;
            }

            unhandledAnyList = currentInterval.next;
            currentInterval.next = TraceInterval.EndMarker;
            return currentInterval;
        }
        return null;
    }

    /**
     * Walk up to {@code toOpId}.
     *
     * @postcondition {@link #currentPosition} is set to {@code toOpId}, {@link #activeFixedList}
     *                and {@link #inactiveFixedList} are populated.
     */
    private void walkTo(int toOpId)
    {
        for (TraceInterval currentInterval = nextInterval(toOpId); currentInterval != null; currentInterval = nextInterval(toOpId))
        {
            int opId = currentInterval.from();

            // set currentPosition prior to call of walkTo
            currentPosition = opId;

            // update unhandled stack intervals
            // updateUnhandledStackIntervals(opId);

            // call walkTo even if currentPosition == id
            walkToFixed(State.Active, opId);
            walkToFixed(State.Inactive, opId);
            walkToAny(opId);

            if (activateCurrent(currentInterval))
            {
                activeAnyList = TraceLinearScanWalker.addToListSortedByFromPositions(activeAnyList, currentInterval);
                intervalMoved(currentInterval, State.Unhandled, State.Active);
            }
        }
        // set currentPosition prior to call of walkTo
        currentPosition = toOpId;

        if (currentPosition <= allocator.maxOpId())
        {
            // update unhandled stack intervals
            // updateUnhandledStackIntervals(toOpId);

            // call walkTo if still in range
            walkToFixed(State.Active, toOpId);
            walkToFixed(State.Inactive, toOpId);
            walkToAny(toOpId);
        }
    }

    private static void intervalMoved(IntervalHint interval, State from, State to)
    {
        // intervalMoved() is called whenever an interval moves from one interval list to another.
        // In the implementation of this method it is prohibited to move the interval to any list.
    }
}
