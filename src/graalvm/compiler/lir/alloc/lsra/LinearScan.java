package graalvm.compiler.lir.alloc.lsra;

import static jdk.vm.ci.code.CodeUtil.isEven;
import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isIllegal;
import static jdk.vm.ci.code.ValueUtil.isLegal;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;

import org.graalvm.collections.Pair;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.ValueConsumer;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.alloc.lsra.Interval.RegisterBinding;
import graalvm.compiler.lir.framemap.FrameMapBuilder;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
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

/**
 * An implementation of the linear scan register allocator algorithm described in
 * <a href="http://doi.acm.org/10.1145/1064979.1064998" > "Optimized Interval Splitting in a Linear
 * Scan Register Allocator"</a> by Christian Wimmer and Hanspeter Moessenboeck.
 */
public class LinearScan
{
    public static class Options
    {
        @Option(help = "Enable spill position optimization", type = OptionType.Debug)
        public static final OptionKey<Boolean> LIROptLSRAOptimizeSpillPosition = new NestedBooleanOptionKey(LIROptimization, true);
    }

    public static class BlockData
    {
        /**
         * Bit map specifying which operands are live upon entry to this block. These are values
         * used in this block or any of its successors where such value are not defined in this
         * block. The bit index of an operand is its {@linkplain LinearScan#operandNumber(Value)
         * operand number}.
         */
        public BitSet liveIn;

        /**
         * Bit map specifying which operands are live upon exit from this block. These are values
         * used in a successor block that are either defined in this block or were live upon entry
         * to this block. The bit index of an operand is its
         * {@linkplain LinearScan#operandNumber(Value) operand number}.
         */
        public BitSet liveOut;

        /**
         * Bit map specifying which operands are used (before being defined) in this block. That is,
         * these are the values that are live upon entry to the block. The bit index of an operand
         * is its {@linkplain LinearScan#operandNumber(Value) operand number}.
         */
        public BitSet liveGen;

        /**
         * Bit map specifying which operands are defined/overwritten in this block. The bit index of
         * an operand is its {@linkplain LinearScan#operandNumber(Value) operand number}.
         */
        public BitSet liveKill;
    }

    public static final int DOMINATOR_SPILL_MOVE_ID = -2;
    private static final int SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT = 1;

    private final LIR ir;
    private final FrameMapBuilder frameMapBuilder;
    private final RegisterAttributes[] registerAttributes;
    private final RegisterArray registers;
    private final RegisterAllocationConfig regAllocConfig;
    private final MoveFactory moveFactory;

    private final BlockMap<BlockData> blockData;

    /**
     * List of blocks in linear-scan order. This is only correct as long as the CFG does not change.
     */
    private final AbstractBlockBase<?>[] sortedBlocks;

    /**
     * @see #intervals()
     */
    private Interval[] intervals;

    /**
     * The number of valid entries in {@link #intervals}.
     */
    private int intervalsSize;

    /**
     * The index of the first entry in {@link #intervals} for a
     * {@linkplain #createDerivedInterval(Interval) derived interval}.
     */
    private int firstDerivedIntervalIndex = -1;

    /**
     * Intervals sorted by {@link Interval#from()}.
     */
    private Interval[] sortedIntervals;

    /**
     * Map from an instruction {@linkplain LIRInstruction#id id} to the instruction. Entries should
     * be retrieved with {@link #instructionForId(int)} as the id is not simply an index into this
     * array.
     */
    private LIRInstruction[] opIdToInstructionMap;

    /**
     * Map from an instruction {@linkplain LIRInstruction#id id} to the
     * {@linkplain AbstractBlockBase block} containing the instruction. Entries should be retrieved
     * with {@link #blockForId(int)} as the id is not simply an index into this array.
     */
    private AbstractBlockBase<?>[] opIdToBlockMap;

    /**
     * The {@linkplain #operandNumber(Value) number} of the first variable operand allocated.
     */
    private final int firstVariableNumber;
    /**
     * Number of variables.
     */
    private int numVariables;
    private final boolean neverSpillConstants;

    /**
     * Sentinel interval to denote the end of an interval list.
     */
    protected final Interval intervalEndMarker;
    public final Range rangeEndMarker;
    private final LIRGenerationResult res;

    protected LinearScan(TargetDescription target, LIRGenerationResult res, MoveFactory spillMoveFactory, RegisterAllocationConfig regAllocConfig, AbstractBlockBase<?>[] sortedBlocks, boolean neverSpillConstants)
    {
        this.ir = res.getLIR();
        this.res = res;
        this.moveFactory = spillMoveFactory;
        this.frameMapBuilder = res.getFrameMapBuilder();
        this.sortedBlocks = sortedBlocks;
        this.registerAttributes = regAllocConfig.getRegisterConfig().getAttributesMap();
        this.regAllocConfig = regAllocConfig;

        this.registers = target.arch.getRegisters();
        this.firstVariableNumber = getRegisters().size();
        this.numVariables = ir.numVariables();
        this.blockData = new BlockMap<>(ir.getControlFlowGraph());
        this.neverSpillConstants = neverSpillConstants;
        this.rangeEndMarker = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE, null);
        this.intervalEndMarker = new Interval(Value.ILLEGAL, Interval.END_MARKER_OPERAND_NUMBER, null, rangeEndMarker);
        this.intervalEndMarker.next = intervalEndMarker;
    }

    public LIRGenerationResult getLIRGenerationResult()
    {
        return res;
    }

    public Interval intervalEndMarker()
    {
        return intervalEndMarker;
    }

    public OptionValues getOptions()
    {
        return ir.getOptions();
    }

    public int getFirstLirInstructionId(AbstractBlockBase<?> block)
    {
        int result = ir.getLIRforBlock(block).get(0).id();
        return result;
    }

    public int getLastLirInstructionId(AbstractBlockBase<?> block)
    {
        ArrayList<LIRInstruction> instructions = ir.getLIRforBlock(block);
        int result = instructions.get(instructions.size() - 1).id();
        return result;
    }

    public MoveFactory getSpillMoveFactory()
    {
        return moveFactory;
    }

    protected MoveResolver createMoveResolver()
    {
        MoveResolver moveResolver = new MoveResolver(this);
        return moveResolver;
    }

    public static boolean isVariableOrRegister(Value value)
    {
        return isVariable(value) || isRegister(value);
    }

    /**
     * Converts an operand (variable or register) to an index in a flat address space covering all
     * the {@linkplain Variable variables} and {@linkplain RegisterValue registers} being processed
     * by this allocator.
     */
    int operandNumber(Value operand)
    {
        if (isRegister(operand))
        {
            int number = asRegister(operand).number;
            return number;
        }
        return firstVariableNumber + ((Variable) operand).index;
    }

    /**
     * Gets the number of operands. This value will increase by 1 for new variable.
     */
    int operandSize()
    {
        return firstVariableNumber + numVariables;
    }

    /**
     * Gets the highest operand number for a register operand. This value will never change.
     */
    int maxRegisterNumber()
    {
        return firstVariableNumber - 1;
    }

    public BlockData getBlockData(AbstractBlockBase<?> block)
    {
        return blockData.get(block);
    }

    void initBlockData(AbstractBlockBase<?> block)
    {
        blockData.put(block, new BlockData());
    }

    static final IntervalPredicate IS_PRECOLORED_INTERVAL = new IntervalPredicate()
    {
        @Override
        public boolean apply(Interval i)
        {
            return isRegister(i.operand);
        }
    };

    static final IntervalPredicate IS_VARIABLE_INTERVAL = new IntervalPredicate()
    {
        @Override
        public boolean apply(Interval i)
        {
            return isVariable(i.operand);
        }
    };

    static final IntervalPredicate IS_STACK_INTERVAL = new IntervalPredicate()
    {
        @Override
        public boolean apply(Interval i)
        {
            return !isRegister(i.operand);
        }
    };

    /**
     * Gets an object describing the attributes of a given register according to this register
     * configuration.
     */
    public RegisterAttributes attributes(Register reg)
    {
        return registerAttributes[reg.number];
    }

    void assignSpillSlot(Interval interval)
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
            VirtualStackSlot slot = frameMapBuilder.allocateSpillSlot(interval.kind());
            interval.setSpillSlot(slot);
            interval.assignLocation(slot);
        }
    }

    /**
     * Map from {@linkplain #operandNumber(Value) operand numbers} to intervals.
     */
    public Interval[] intervals()
    {
        return intervals;
    }

    void initIntervals()
    {
        intervalsSize = operandSize();
        intervals = new Interval[intervalsSize + (intervalsSize >> SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT)];
    }

    /**
     * Creates a new interval.
     *
     * @param operand the operand for the interval
     * @return the created interval
     */
    Interval createInterval(AllocatableValue operand)
    {
        int operandNumber = operandNumber(operand);
        Interval interval = new Interval(operand, operandNumber, intervalEndMarker, rangeEndMarker);
        intervals[operandNumber] = interval;
        return interval;
    }

    /**
     * Creates an interval as a result of splitting or spilling another interval.
     *
     * @param source an interval being split of spilled
     * @return a new interval derived from {@code source}
     */
    Interval createDerivedInterval(Interval source)
    {
        if (firstDerivedIntervalIndex == -1)
        {
            firstDerivedIntervalIndex = intervalsSize;
        }
        if (intervalsSize == intervals.length)
        {
            intervals = Arrays.copyOf(intervals, intervals.length + (intervals.length >> SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT) + 1);
        }
        intervalsSize++;
        /*
         * Note that these variables are not managed and must therefore never be inserted into the
         * LIR
         */
        Variable variable = new Variable(source.kind(), numVariables++);

        Interval interval = createInterval(variable);
        return interval;
    }

    // access to block list (sorted in linear scan order)
    public int blockCount()
    {
        return sortedBlocks.length;
    }

    public AbstractBlockBase<?> blockAt(int index)
    {
        return sortedBlocks[index];
    }

    /**
     * Gets the size of the {@link BlockData#liveIn} and {@link BlockData#liveOut} sets for a basic
     * block. These sets do not include any operands allocated as a result of creating
     * {@linkplain #createDerivedInterval(Interval) derived intervals}.
     */
    public int liveSetSize()
    {
        return firstDerivedIntervalIndex == -1 ? operandSize() : firstDerivedIntervalIndex;
    }

    int numLoops()
    {
        return ir.getControlFlowGraph().getLoops().size();
    }

    Interval intervalFor(int operandNumber)
    {
        return intervals[operandNumber];
    }

    public Interval intervalFor(Value operand)
    {
        int operandNumber = operandNumber(operand);
        return intervals[operandNumber];
    }

    public Interval getOrCreateInterval(AllocatableValue operand)
    {
        Interval ret = intervalFor(operand);
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
     * Converts an {@linkplain LIRInstruction#id instruction id} to an instruction index. All LIR
     * instructions in a method have an index one greater than their linear-scan order predecessor
     * with the first instruction having an index of 0.
     */
    private static int opIdToIndex(int opId)
    {
        return opId >> 1;
    }

    /**
     * Retrieves the {@link LIRInstruction} based on its {@linkplain LIRInstruction#id id}.
     *
     * @param opId an instruction {@linkplain LIRInstruction#id id}
     * @return the instruction whose {@linkplain LIRInstruction#id} {@code == id}
     */
    public LIRInstruction instructionForId(int opId)
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
    public AbstractBlockBase<?> blockForId(int opId)
    {
        return opIdToBlockMap[opIdToIndex(opId)];
    }

    boolean isBlockBegin(int opId)
    {
        return opId == 0 || blockForId(opId) != blockForId(opId - 1);
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

    abstract static class IntervalPredicate
    {
        abstract boolean apply(Interval i);
    }

    public boolean isProcessed(Value operand)
    {
        return !isRegister(operand) || attributes(asRegister(operand)).isAllocatable();
    }

    // * Phase 5: actual register allocation

    private static boolean isSorted(Interval[] intervals)
    {
        int from = -1;
        for (Interval interval : intervals)
        {
            from = interval.from();
        }
        return true;
    }

    static Interval addToList(Interval first, Interval prev, Interval interval)
    {
        Interval newFirst = first;
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

    Pair<Interval, Interval> createUnhandledLists(IntervalPredicate isList1, IntervalPredicate isList2)
    {
        Interval list1 = intervalEndMarker;
        Interval list2 = intervalEndMarker;

        Interval list1Prev = null;
        Interval list2Prev = null;
        Interval v;

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
            else if (isList2 == null || isList2.apply(v))
            {
                list2 = addToList(list2, list2Prev, v);
                list2Prev = v;
            }
        }

        if (list1Prev != null)
        {
            list1Prev.next = intervalEndMarker;
        }
        if (list2Prev != null)
        {
            list2Prev.next = intervalEndMarker;
        }

        return Pair.create(list1, list2);
    }

    protected void sortIntervalsBeforeAllocation()
    {
        int sortedLen = 0;
        for (Interval interval : intervals)
        {
            if (interval != null)
            {
                sortedLen++;
            }
        }

        Interval[] sortedList = new Interval[sortedLen];
        int sortedIdx = 0;
        int sortedFromMax = -1;

        // special sorting algorithm: the original interval-list is almost sorted,
        // only some intervals are swapped. So this is much faster than a complete QuickSort
        for (Interval interval : intervals)
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
        sortedIntervals = sortedList;
    }

    void sortIntervalsAfterAllocation()
    {
        if (firstDerivedIntervalIndex == -1)
        {
            // no intervals have been added during allocation, so sorted list is already up to date
            return;
        }

        Interval[] oldList = sortedIntervals;
        Interval[] newList = Arrays.copyOfRange(intervals, firstDerivedIntervalIndex, intervalsSize);
        int oldLen = oldList.length;
        int newLen = newList.length;

        // conventional sort-algorithm for new intervals
        Arrays.sort(newList, (Interval a, Interval b) -> a.from() - b.from());

        // merge old and new list (both already sorted) into one combined list
        Interval[] combinedList = new Interval[oldLen + newLen];
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

    // wrapper for Interval.splitChildAtOpId that performs a bailout in product mode
    // instead of returning null
    public Interval splitChildAtOpId(Interval interval, int opId, LIRInstruction.OperandMode mode)
    {
        Interval result = interval.getSplitChildAtOpId(opId, mode, this);

        if (result != null)
        {
            return result;
        }
        throw new GraalError("LinearScan: interval is null");
    }

    static AllocatableValue canonicalSpillOpr(Interval interval)
    {
        return interval.spillSlot();
    }

    boolean isMaterialized(AllocatableValue operand, int opId, OperandMode mode)
    {
        Interval interval = intervalFor(operand);

        if (opId != -1)
        {
            /*
             * Operands are not changed when an interval is split during allocation, so search the
             * right interval here.
             */
            interval = splitChildAtOpId(interval, opId, mode);
        }

        return isIllegal(interval.location()) && interval.canMaterialize();
    }

    boolean isCallerSave(Value operand)
    {
        return attributes(asRegister(operand)).isCallerSave();
    }

    protected void allocate(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        createLifetimeAnalysisPhase().apply(target, lirGenRes, context);

        sortIntervalsBeforeAllocation();

        createRegisterAllocationPhase().apply(target, lirGenRes, context);

        if (LinearScan.Options.LIROptLSRAOptimizeSpillPosition.getValue(getOptions()))
        {
            createOptimizeSpillPositionPhase().apply(target, lirGenRes, context);
        }
        createResolveDataFlowPhase().apply(target, lirGenRes, context);

        sortIntervalsAfterAllocation();

        beforeSpillMoveElimination();
        createSpillMoveEliminationPhase().apply(target, lirGenRes, context);
        createAssignLocationsPhase().apply(target, lirGenRes, context);
    }

    protected void beforeSpillMoveElimination()
    {
    }

    protected LinearScanLifetimeAnalysisPhase createLifetimeAnalysisPhase()
    {
        return new LinearScanLifetimeAnalysisPhase(this);
    }

    protected LinearScanRegisterAllocationPhase createRegisterAllocationPhase()
    {
        return new LinearScanRegisterAllocationPhase(this);
    }

    protected LinearScanOptimizeSpillPositionPhase createOptimizeSpillPositionPhase()
    {
        return new LinearScanOptimizeSpillPositionPhase(this);
    }

    protected LinearScanResolveDataFlowPhase createResolveDataFlowPhase()
    {
        return new LinearScanResolveDataFlowPhase(this);
    }

    protected LinearScanEliminateSpillMovePhase createSpillMoveEliminationPhase()
    {
        return new LinearScanEliminateSpillMovePhase(this);
    }

    protected LinearScanAssignLocationsPhase createAssignLocationsPhase()
    {
        return new LinearScanAssignLocationsPhase(this);
    }

    class CheckConsumer implements ValueConsumer
    {
        boolean ok;
        Interval curInterval;

        @Override
        public void visitValue(Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            if (isRegister(operand))
            {
                if (intervalFor(operand) == curInterval)
                {
                    ok = true;
                }
            }
        }
    }

    void verifyNoOopsInFixedIntervals()
    {
        CheckConsumer checkConsumer = new CheckConsumer();

        Interval fixedIntervals;
        Interval otherIntervals;
        fixedIntervals = createUnhandledLists(IS_PRECOLORED_INTERVAL, null).getLeft();
        // to ensure a walking until the last instruction id, add a dummy interval
        // with a high operation id
        otherIntervals = new Interval(Value.ILLEGAL, -1, intervalEndMarker, rangeEndMarker);
        otherIntervals.addRange(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        IntervalWalker iw = new IntervalWalker(this, fixedIntervals, otherIntervals);

        for (AbstractBlockBase<?> block : sortedBlocks)
        {
            ArrayList<LIRInstruction> instructions = ir.getLIRforBlock(block);

            for (int j = 0; j < instructions.size(); j++)
            {
                LIRInstruction op = instructions.get(j);

                if (op.hasState())
                {
                    iw.walkBefore(op.id());
                    boolean checkLive = true;

                    /*
                     * Make sure none of the fixed registers is live across an oopmap since we
                     * can't handle that correctly.
                     */
                    if (checkLive)
                    {
                        for (Interval interval = iw.activeLists.get(RegisterBinding.Fixed); !interval.isEndMarker(); interval = interval.next)
                        {
                            if (interval.currentTo() > op.id() + 1)
                            {
                                /*
                                 * This interval is live out of this op so make sure that this
                                 * interval represents some value that's referenced by this op
                                 * either as an input or output.
                                 */
                                checkConsumer.curInterval = interval;
                                checkConsumer.ok = false;

                                op.visitEachInput(checkConsumer);
                                op.visitEachAlive(checkConsumer);
                                op.visitEachTemp(checkConsumer);
                                op.visitEachOutput(checkConsumer);
                            }
                        }
                    }
                }
            }
        }
    }

    public LIR getLIR()
    {
        return ir;
    }

    public FrameMapBuilder getFrameMapBuilder()
    {
        return frameMapBuilder;
    }

    public AbstractBlockBase<?>[] sortedBlocks()
    {
        return sortedBlocks;
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
}
