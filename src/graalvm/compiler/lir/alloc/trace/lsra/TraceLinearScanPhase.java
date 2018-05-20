package graalvm.compiler.lir.alloc.trace.lsra;

import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isIllegal;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.alloc.trace.GlobalLivenessInfo;
import graalvm.compiler.lir.alloc.trace.TraceAllocationPhase;
import graalvm.compiler.lir.alloc.trace.TraceAllocationPhase.TraceAllocationContext;
import graalvm.compiler.lir.alloc.trace.TraceRegisterAllocationPhase;
import graalvm.compiler.lir.alloc.trace.TraceUtil;
import graalvm.compiler.lir.framemap.FrameMapBuilder;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.phases.LIRPhase;
import graalvm.compiler.options.NestedBooleanOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

/**
 * Implementation of the Linear Scan allocation approach for traces described in
 * <a href="http://dx.doi.org/10.1145/2972206.2972211">"Trace-based Register Allocation in a JIT
 * Compiler"</a> by Josef Eisl et al. It is derived from
 * <a href="http://doi.acm.org/10.1145/1064979.1064998" > "Optimized Interval Splitting in a Linear
 * Scan Register Allocator"</a> by Christian Wimmer and Hanspeter Moessenboeck.
 */
public final class TraceLinearScanPhase extends TraceAllocationPhase<TraceAllocationContext>
{
    public static class Options
    {
        @Option(help = "Enable spill position optimization", type = OptionType.Debug)
        public static final OptionKey<Boolean> LIROptTraceRAEliminateSpillMoves = new NestedBooleanOptionKey(LIRPhase.Options.LIROptimization, true);
    }

    private static final TraceLinearScanRegisterAllocationPhase TRACE_LINEAR_SCAN_REGISTER_ALLOCATION_PHASE = new TraceLinearScanRegisterAllocationPhase();
    private static final TraceLinearScanAssignLocationsPhase TRACE_LINEAR_SCAN_ASSIGN_LOCATIONS_PHASE = new TraceLinearScanAssignLocationsPhase();
    private static final TraceLinearScanEliminateSpillMovePhase TRACE_LINEAR_SCAN_ELIMINATE_SPILL_MOVE_PHASE = new TraceLinearScanEliminateSpillMovePhase();
    private static final TraceLinearScanResolveDataFlowPhase TRACE_LINEAR_SCAN_RESOLVE_DATA_FLOW_PHASE = new TraceLinearScanResolveDataFlowPhase();
    private static final TraceLinearScanLifetimeAnalysisPhase TRACE_LINEAR_SCAN_LIFETIME_ANALYSIS_PHASE = new TraceLinearScanLifetimeAnalysisPhase();

    public static final int DOMINATOR_SPILL_MOVE_ID = -2;

    private final FrameMapBuilder frameMapBuilder;
    private final RegisterAttributes[] registerAttributes;
    private final RegisterArray registers;
    private final RegisterAllocationConfig regAllocConfig;
    private final MoveFactory moveFactory;

    protected final TraceBuilderResult traceBuilderResult;

    private final boolean neverSpillConstants;

    /**
     * Maps from {@link Variable#index} to a spill stack slot. If
     * {@linkplain graalvm.compiler.lir.alloc.trace.TraceRegisterAllocationPhase.Options#TraceRACacheStackSlots
     * enabled} a {@link Variable} is always assigned to the same stack slot.
     */
    private final AllocatableValue[] cachedStackSlots;

    private final LIRGenerationResult res;
    private final GlobalLivenessInfo livenessInfo;

    public TraceLinearScanPhase(TargetDescription target, LIRGenerationResult res, MoveFactory spillMoveFactory, RegisterAllocationConfig regAllocConfig, TraceBuilderResult traceBuilderResult, boolean neverSpillConstants, AllocatableValue[] cachedStackSlots, GlobalLivenessInfo livenessInfo)
    {
        this.res = res;
        this.moveFactory = spillMoveFactory;
        this.frameMapBuilder = res.getFrameMapBuilder();
        this.registerAttributes = regAllocConfig.getRegisterConfig().getAttributesMap();
        this.regAllocConfig = regAllocConfig;

        this.registers = target.arch.getRegisters();
        this.traceBuilderResult = traceBuilderResult;
        this.neverSpillConstants = neverSpillConstants;
        this.cachedStackSlots = cachedStackSlots;
        this.livenessInfo = livenessInfo;
    }

    public static boolean isVariableOrRegister(Value value)
    {
        return isVariable(value) || isRegister(value);
    }

    abstract static class IntervalPredicate
    {
        abstract boolean apply(TraceInterval i);
    }

    static final IntervalPredicate IS_VARIABLE_INTERVAL = new IntervalPredicate()
    {
        @Override
        public boolean apply(TraceInterval i)
        {
            // all TraceIntervals are variable intervals
            return !i.preSpilledAllocated();
        }
    };
    private static final Comparator<TraceInterval> SORT_BY_FROM_COMP = new Comparator<TraceInterval>()
    {
        @Override
        public int compare(TraceInterval a, TraceInterval b)
        {
            return a.from() - b.from();
        }
    };
    private static final Comparator<TraceInterval> SORT_BY_SPILL_POS_COMP = new Comparator<TraceInterval>()
    {
        @Override
        public int compare(TraceInterval a, TraceInterval b)
        {
            return a.spillDefinitionPos() - b.spillDefinitionPos();
        }
    };

    public TraceLinearScan createAllocator(Trace trace)
    {
        return new TraceLinearScan(trace);
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, TraceAllocationContext traceContext)
    {
        createAllocator(trace).allocate(target, lirGenRes, traceContext);
    }

    private static <T extends IntervalHint> boolean isSortedByFrom(T[] intervals)
    {
        int from = -1;
        for (T interval : intervals)
        {
            if (interval == null)
            {
                continue;
            }
            from = interval.from();
        }
        return true;
    }

    private static boolean isSortedBySpillPos(TraceInterval[] intervals)
    {
        int from = -1;
        for (TraceInterval interval : intervals)
        {
            from = interval.spillDefinitionPos();
        }
        return true;
    }

    private static TraceInterval[] sortIntervalsBeforeAllocation(TraceInterval[] intervals, TraceInterval[] sortedList)
    {
        int sortedIdx = 0;
        int sortedFromMax = -1;

        // special sorting algorithm: the original interval-list is almost sorted,
        // only some intervals are swapped. So this is much faster than a complete QuickSort
        for (TraceInterval interval : intervals)
        {
            if (interval != null)
            {
                int from = interval.from();

                if (sortedFromMax <= from)
                {
                    sortedList[sortedIdx++] = interval;
                    sortedFromMax = interval.from();
                }
                else
                {
                    // the assumption that the intervals are already sorted failed,
                    // so this interval must be sorted in manually
                    int j;
                    for (j = sortedIdx - 1; j >= 0 && from < sortedList[j].from(); j--)
                    {
                        sortedList[j + 1] = sortedList[j];
                    }
                    sortedList[j + 1] = interval;
                    sortedIdx++;
                }
            }
        }
        return sortedList;
    }

    public final class TraceLinearScan
    {
        /**
         * Intervals sorted by {@link TraceInterval#from()}.
         */
        private TraceInterval[] sortedIntervals;

        private final Trace trace;

        public TraceLinearScan(Trace trace)
        {
            this.trace = trace;
            this.fixedIntervals = new FixedInterval[registers.size()];
        }

        GlobalLivenessInfo getGlobalLivenessInfo()
        {
            return livenessInfo;
        }

        /**
         * @return {@link Variable#index}
         */
        int operandNumber(Variable operand)
        {
            return operand.index;
        }

        OptionValues getOptions()
        {
            return getLIR().getOptions();
        }

        /**
         * Gets the number of operands. This value will increase by 1 for new variable.
         */
        int operandSize()
        {
            return getLIR().numVariables();
        }

        /**
         * Gets the number of registers. This value will never change.
         */
        int numRegisters()
        {
            return registers.size();
        }

        public int getFirstLirInstructionId(AbstractBlockBase<?> block)
        {
            int result = getLIR().getLIRforBlock(block).get(0).id();
            return result;
        }

        public int getLastLirInstructionId(AbstractBlockBase<?> block)
        {
            ArrayList<LIRInstruction> instructions = getLIR().getLIRforBlock(block);
            int result = instructions.get(instructions.size() - 1).id();
            return result;
        }

        /**
         * Gets an object describing the attributes of a given register according to this register
         * configuration.
         */
        public RegisterAttributes attributes(Register reg)
        {
            return registerAttributes[reg.number];
        }

        public boolean isAllocatable(RegisterValue register)
        {
            return attributes(register.getRegister()).isAllocatable();
        }

        public MoveFactory getSpillMoveFactory()
        {
            return moveFactory;
        }

        protected TraceLocalMoveResolver createMoveResolver()
        {
            TraceLocalMoveResolver moveResolver = new TraceLocalMoveResolver(this);
            return moveResolver;
        }

        void assignSpillSlot(TraceInterval interval)
        {
            /*
             * Assign the canonical spill slot of the parent (if a part of the interval is already
             * spilled) or allocate a new spill slot.
             */
            if (interval.canMaterialize())
            {
                interval.assignLocation(Value.ILLEGAL);
            }
            else if (interval.spillSlot() != null)
            {
                interval.assignLocation(interval.spillSlot());
            }
            else
            {
                AllocatableValue slot = allocateSpillSlot(interval);
                interval.setSpillSlot(slot);
                interval.assignLocation(slot);
            }
        }

        /**
         * Returns a new spill slot or a cached entry if there is already one for the
         * {@linkplain TraceInterval variable}.
         */
        private AllocatableValue allocateSpillSlot(TraceInterval interval)
        {
            int variableIndex = interval.splitParent().operandNumber;
            OptionValues options = getOptions();
            if (TraceRegisterAllocationPhase.Options.TraceRACacheStackSlots.getValue(options))
            {
                AllocatableValue cachedStackSlot = cachedStackSlots[variableIndex];
                if (cachedStackSlot != null)
                {
                    return cachedStackSlot;
                }
            }
            VirtualStackSlot slot = frameMapBuilder.allocateSpillSlot(getKind(interval));
            if (TraceRegisterAllocationPhase.Options.TraceRACacheStackSlots.getValue(options))
            {
                cachedStackSlots[variableIndex] = slot;
            }
            return slot;
        }

        // access to block list (sorted in linear scan order)
        public int blockCount()
        {
            return sortedBlocks().length;
        }

        public AbstractBlockBase<?> blockAt(int index)
        {
            return sortedBlocks()[index];
        }

        int numLoops()
        {
            return getLIR().getControlFlowGraph().getLoops().size();
        }

        boolean isBlockBegin(int opId)
        {
            return opId == 0 || blockForId(opId) != blockForId(opId - 1);
        }

        boolean isBlockEnd(int opId)
        {
            boolean isBlockBegin = isBlockBegin(opId + 2);
            return isBlockBegin;
        }

        boolean coversBlockBegin(int opId1, int opId2)
        {
            return blockForId(opId1) != blockForId(opId2);
        }

        /**
         * Determines if an {@link LIRInstruction} destroys all caller saved registers.
         *
         * @param opId an instruction {@linkplain LIRInstruction#id id}
         * @return {@code true} if the instruction denoted by {@code id} destroys all caller saved
         *         registers.
         */
        boolean hasCall(int opId)
        {
            return instructionForId(opId).destroysCallerSavedRegisters();
        }

        public boolean isProcessed(Value operand)
        {
            return !isRegister(operand) || attributes(asRegister(operand)).isAllocatable();
        }

        // * Phase 5: actual register allocation

        private TraceInterval addToList(TraceInterval first, TraceInterval prev, TraceInterval interval)
        {
            TraceInterval newFirst = first;
            if (prev != null)
            {
                prev.next = interval;
            }
            else
            {
                newFirst = interval;
            }
            return newFirst;
        }

        TraceInterval createUnhandledListByFrom(IntervalPredicate isList1)
        {
            return createUnhandledList(isList1);
        }

        TraceInterval createUnhandledListBySpillPos(IntervalPredicate isList1)
        {
            return createUnhandledList(isList1);
        }

        private TraceInterval createUnhandledList(IntervalPredicate isList1)
        {
            TraceInterval list1 = TraceInterval.EndMarker;

            TraceInterval list1Prev = null;
            TraceInterval v;

            int n = sortedIntervals.length;
            for (int i = 0; i < n; i++)
            {
                v = sortedIntervals[i];
                if (v == null)
                {
                    continue;
                }

                if (isList1.apply(v))
                {
                    list1 = addToList(list1, list1Prev, v);
                    list1Prev = v;
                }
            }

            if (list1Prev != null)
            {
                list1Prev.next = TraceInterval.EndMarker;
            }

            return list1;
        }

        private FixedInterval addToList(FixedInterval first, FixedInterval prev, FixedInterval interval)
        {
            FixedInterval newFirst = first;
            if (prev != null)
            {
                prev.next = interval;
            }
            else
            {
                newFirst = interval;
            }
            return newFirst;
        }

        FixedInterval createFixedUnhandledList()
        {
            FixedInterval list1 = FixedInterval.EndMarker;

            FixedInterval list1Prev = null;
            for (FixedInterval v : fixedIntervals)
            {
                if (v == null)
                {
                    continue;
                }

                v.rewindRange();
                list1 = addToList(list1, list1Prev, v);
                list1Prev = v;
            }

            if (list1Prev != null)
            {
                list1Prev.next = FixedInterval.EndMarker;
            }

            return list1;
        }

        // SORTING

        protected void sortIntervalsBeforeAllocation()
        {
            int sortedLen = 0;
            for (TraceInterval interval : intervals())
            {
                if (interval != null)
                {
                    sortedLen++;
                }
            }
            sortedIntervals = TraceLinearScanPhase.sortIntervalsBeforeAllocation(intervals(), new TraceInterval[sortedLen]);
        }

        void sortIntervalsAfterAllocation()
        {
            if (hasDerivedIntervals())
            {
                // no intervals have been added during allocation, so sorted list is already up to
                // date
                return;
            }

            TraceInterval[] oldList = sortedIntervals;
            TraceInterval[] newList = Arrays.copyOfRange(intervals(), firstDerivedIntervalIndex(), intervalsSize());
            int oldLen = oldList.length;
            int newLen = newList.length;

            // conventional sort-algorithm for new intervals
            Arrays.sort(newList, SORT_BY_FROM_COMP);

            // merge old and new list (both already sorted) into one combined list
            TraceInterval[] combinedList = new TraceInterval[oldLen + newLen];
            int oldIdx = 0;
            int newIdx = 0;

            while (oldIdx + newIdx < combinedList.length)
            {
                if (newIdx >= newLen || (oldIdx < oldLen && oldList[oldIdx].from() <= newList[newIdx].from()))
                {
                    combinedList[oldIdx + newIdx] = oldList[oldIdx];
                    oldIdx++;
                }
                else
                {
                    combinedList[oldIdx + newIdx] = newList[newIdx];
                    newIdx++;
                }
            }

            sortedIntervals = combinedList;
        }

        void sortIntervalsBySpillPos()
        {
            // TODO (JE): better algorithm?
            // conventional sort-algorithm for new intervals
            Arrays.sort(sortedIntervals, SORT_BY_SPILL_POS_COMP);
        }

        // wrapper for Interval.splitChildAtOpId that performs a bailout in product mode
        // instead of returning null
        public TraceInterval splitChildAtOpId(TraceInterval interval, int opId, LIRInstruction.OperandMode mode)
        {
            TraceInterval result = interval.getSplitChildAtOpId(opId, mode);

            if (result != null)
            {
                return result;
            }
            throw new GraalError("LinearScan: interval is null");
        }

        AllocatableValue canonicalSpillOpr(TraceInterval interval)
        {
            return interval.spillSlot();
        }

        boolean isMaterialized(Variable operand, int opId, OperandMode mode)
        {
            TraceInterval interval = intervalFor(operand);

            if (opId != -1)
            {
                /*
                 * Operands are not changed when an interval is split during allocation, so search
                 * the right interval here.
                 */
                interval = splitChildAtOpId(interval, opId, mode);
            }

            return isIllegal(interval.location()) && interval.canMaterialize();
        }

        boolean isCallerSave(Value operand)
        {
            return attributes(asRegister(operand)).isCallerSave();
        }

        protected void allocate(TargetDescription target, LIRGenerationResult lirGenRes, TraceAllocationContext traceContext)
        {
            MoveFactory spillMoveFactory = traceContext.spillMoveFactory;
            RegisterAllocationConfig registerAllocationConfig = traceContext.registerAllocationConfig;
            TRACE_LINEAR_SCAN_LIFETIME_ANALYSIS_PHASE.apply(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, this);

            sortIntervalsBeforeAllocation();

            TRACE_LINEAR_SCAN_REGISTER_ALLOCATION_PHASE.apply(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, this);

            // resolve intra-trace data-flow
            TRACE_LINEAR_SCAN_RESOLVE_DATA_FLOW_PHASE.apply(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, this);

            // eliminate spill moves
            OptionValues options = getOptions();
            if (Options.LIROptTraceRAEliminateSpillMoves.getValue(options))
            {
                TRACE_LINEAR_SCAN_ELIMINATE_SPILL_MOVE_PHASE.apply(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, this);
            }

            TRACE_LINEAR_SCAN_ASSIGN_LOCATIONS_PHASE.apply(target, lirGenRes, trace, spillMoveFactory, registerAllocationConfig, traceBuilderResult, this);
        }

        public LIR getLIR()
        {
            return res.getLIR();
        }

        public FrameMapBuilder getFrameMapBuilder()
        {
            return frameMapBuilder;
        }

        public AbstractBlockBase<?>[] sortedBlocks()
        {
            return trace.getBlocks();
        }

        public RegisterArray getRegisters()
        {
            return registers;
        }

        public RegisterAllocationConfig getRegisterAllocationConfig()
        {
            return regAllocConfig;
        }

        public boolean callKillsRegisters()
        {
            return regAllocConfig.getRegisterConfig().areAllAllocatableRegistersCallerSaved();
        }

        boolean neverSpillConstants()
        {
            return neverSpillConstants;
        }

        // IntervalData

        private static final int SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT = 1;

        /**
         * The index of the first entry in {@link #intervals} for a
         * {@linkplain #createDerivedInterval(TraceInterval) derived interval}.
         */
        private int firstDerivedIntervalIndex = -1;

        /**
         * @see #fixedIntervals()
         */
        private final FixedInterval[] fixedIntervals;

        /**
         * @see #intervals()
         */
        private TraceInterval[] intervals;

        /**
         * The number of valid entries in {@link #intervals}.
         */
        private int intervalsSize;

        /**
         * Map from an instruction {@linkplain LIRInstruction#id id} to the instruction. Entries
         * should be retrieved with {@link #instructionForId(int)} as the id is not simply an index
         * into this array.
         */
        private LIRInstruction[] opIdToInstructionMap;

        /**
         * Map from an instruction {@linkplain LIRInstruction#id id} to the
         * {@linkplain AbstractBlockBase block} containing the instruction. Entries should be
         * retrieved with {@link #blockForId(int)} as the id is not simply an index into this array.
         */
        private AbstractBlockBase<?>[] opIdToBlockMap;

        /**
         * Map from {@linkplain #operandNumber operand numbers} to intervals.
         */
        TraceInterval[] intervals()
        {
            return intervals;
        }

        /**
         * Map from {@linkplain Register#number} to fixed intervals.
         */
        FixedInterval[] fixedIntervals()
        {
            return fixedIntervals;
        }

        void initIntervals()
        {
            intervalsSize = operandSize();
            intervals = new TraceInterval[intervalsSize + (intervalsSize >> SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT)];
        }

        /**
         * Creates a new fixed interval.
         *
         * @param reg the operand for the interval
         * @return the created interval
         */
        private FixedInterval createFixedInterval(RegisterValue reg)
        {
            FixedInterval interval = new FixedInterval(reg);
            int operandNumber = reg.getRegister().number;
            fixedIntervals[operandNumber] = interval;
            return interval;
        }

        /**
         * Creates a new interval.
         *
         * @param operand the operand for the interval
         * @return the created interval
         */
        private TraceInterval createInterval(Variable operand)
        {
            int operandNumber = operandNumber(operand);
            TraceInterval interval = new TraceInterval(operand);
            intervals[operandNumber] = interval;
            return interval;
        }

        /**
         * Creates an interval as a result of splitting or spilling another interval.
         *
         * @param source an interval being split of spilled
         * @return a new interval derived from {@code source}
         */
        TraceInterval createDerivedInterval(TraceInterval source)
        {
            if (firstDerivedIntervalIndex == -1)
            {
                firstDerivedIntervalIndex = intervalsSize;
            }
            if (intervalsSize == intervals.length)
            {
                intervals = Arrays.copyOf(intervals, intervals.length + (intervals.length >> SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT) + 1);
            }
            // increments intervalsSize
            Variable variable = createVariable(getKind(source));

            TraceInterval interval = createInterval(variable);
            return interval;
        }

        /**
         * Creates a new variable for a derived interval. Note that the variable is not
         * {@linkplain LIR#numVariables() managed} so it must not be inserted into the {@link LIR}.
         */
        private Variable createVariable(ValueKind<?> kind)
        {
            return new Variable(kind, intervalsSize++);
        }

        boolean hasDerivedIntervals()
        {
            return firstDerivedIntervalIndex != -1;
        }

        int firstDerivedIntervalIndex()
        {
            return firstDerivedIntervalIndex;
        }

        public int intervalsSize()
        {
            return intervalsSize;
        }

        FixedInterval fixedIntervalFor(RegisterValue reg)
        {
            return fixedIntervals[reg.getRegister().number];
        }

        FixedInterval getOrCreateFixedInterval(RegisterValue reg)
        {
            FixedInterval ret = fixedIntervalFor(reg);
            if (ret == null)
            {
                return createFixedInterval(reg);
            }
            else
            {
                return ret;
            }
        }

        TraceInterval intervalFor(Variable operand)
        {
            return intervalFor(operandNumber(operand));
        }

        TraceInterval intervalFor(int operandNumber)
        {
            return intervals[operandNumber];
        }

        TraceInterval getOrCreateInterval(Variable operand)
        {
            TraceInterval ret = intervalFor(operand);
            if (ret == null)
            {
                return createInterval(operand);
            }
            else
            {
                return ret;
            }
        }

        void initOpIdMaps(int numInstructions)
        {
            opIdToInstructionMap = new LIRInstruction[numInstructions];
            opIdToBlockMap = new AbstractBlockBase<?>[numInstructions];
        }

        void putOpIdMaps(int index, LIRInstruction op, AbstractBlockBase<?> block)
        {
            opIdToInstructionMap[index] = op;
            opIdToBlockMap[index] = block;
        }

        /**
         * Gets the highest instruction id allocated by this object.
         */
        int maxOpId()
        {
            return (opIdToInstructionMap.length - 1) << 1;
        }

        /**
         * Converts an {@linkplain LIRInstruction#id instruction id} to an instruction index. All
         * LIR instructions in a method have an index one greater than their linear-scan order
         * predecessor with the first instruction having an index of 0.
         */
        private int opIdToIndex(int opId)
        {
            return opId >> 1;
        }

        /**
         * Retrieves the {@link LIRInstruction} based on its {@linkplain LIRInstruction#id id}.
         *
         * @param opId an instruction {@linkplain LIRInstruction#id id}
         * @return the instruction whose {@linkplain LIRInstruction#id} {@code == id}
         */
        LIRInstruction instructionForId(int opId)
        {
            LIRInstruction instr = opIdToInstructionMap[opIdToIndex(opId)];
            return instr;
        }

        /**
         * Gets the block containing a given instruction.
         *
         * @param opId an instruction {@linkplain LIRInstruction#id id}
         * @return the block containing the instruction denoted by {@code opId}
         */
        AbstractBlockBase<?> blockForId(int opId)
        {
            return opIdToBlockMap[opIdToIndex(opId)];
        }

        boolean hasInterTracePredecessor(AbstractBlockBase<?> block)
        {
            return TraceUtil.hasInterTracePredecessor(traceBuilderResult, trace, block);
        }

        boolean hasInterTraceSuccessor(AbstractBlockBase<?> block)
        {
            return TraceUtil.hasInterTraceSuccessor(traceBuilderResult, trace, block);
        }

        AllocatableValue getOperand(TraceInterval interval)
        {
            return interval.operand;
        }

        ValueKind<?> getKind(TraceInterval interval)
        {
            return getOperand(interval).getValueKind();
        }
    }
}
