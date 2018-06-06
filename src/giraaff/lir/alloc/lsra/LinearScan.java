package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.Pair;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.BlockMap;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.Variable;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.LIRPhase;
import giraaff.util.GraalError;

///
// An implementation of the linear scan register allocator algorithm described in
// <a href="http://doi.acm.org/10.1145/1064979.1064998">"Optimized Interval Splitting in a Linear Scan Register Allocator"</a>
// by Christian Wimmer and Hanspeter Moessenboeck.
///
// @class LinearScan
public class LinearScan
{
    // @class LinearScan.BlockData
    public static final class BlockData
    {
        ///
        // Bit map specifying which operands are live upon entry to this block. These are values
        // used in this block or any of its successors where such value are not defined in this
        // block. The bit index of an operand is its {@linkplain LinearScan#operandNumber(Value)
        // operand number}.
        ///
        // @field
        public BitSet ___liveIn;

        ///
        // Bit map specifying which operands are live upon exit from this block. These are values
        // used in a successor block that are either defined in this block or were live upon entry
        // to this block. The bit index of an operand is its
        // {@linkplain LinearScan#operandNumber(Value) operand number}.
        ///
        // @field
        public BitSet ___liveOut;

        ///
        // Bit map specifying which operands are used (before being defined) in this block. That is,
        // these are the values that are live upon entry to the block. The bit index of an operand
        // is its {@linkplain LinearScan#operandNumber(Value) operand number}.
        ///
        // @field
        public BitSet ___liveGen;

        ///
        // Bit map specifying which operands are defined/overwritten in this block. The bit index of
        // an operand is its {@linkplain LinearScan#operandNumber(Value) operand number}.
        ///
        // @field
        public BitSet ___liveKill;
    }

    // @def
    public static final int DOMINATOR_SPILL_MOVE_ID = -2;
    // @def
    private static final int SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT = 1;

    // @field
    private final LIR ___ir;
    // @field
    private final FrameMapBuilder ___frameMapBuilder;
    // @field
    private final RegisterAttributes[] ___registerAttributes;
    // @field
    private final RegisterArray ___registers;
    // @field
    private final RegisterAllocationConfig ___regAllocConfig;
    // @field
    private final LIRGeneratorTool.MoveFactory ___moveFactory;

    // @field
    private final BlockMap<LinearScan.BlockData> ___blockData;

    ///
    // List of blocks in linear-scan order. This is only correct as long as the CFG does not change.
    ///
    // @field
    private final AbstractBlockBase<?>[] ___sortedBlocks;

    ///
    // @see #intervals()
    ///
    // @field
    private Interval[] ___intervals;

    ///
    // The number of valid entries in {@link #intervals}.
    ///
    // @field
    private int ___intervalsSize;

    ///
    // The index of the first entry in {@link #intervals} for a
    // {@linkplain #createDerivedInterval(Interval) derived interval}.
    ///
    // @field
    private int ___firstDerivedIntervalIndex = -1;

    ///
    // Intervals sorted by {@link Interval#from()}.
    ///
    // @field
    private Interval[] ___sortedIntervals;

    ///
    // Map from an instruction {@linkplain LIRInstruction#id id} to the instruction. Entries should
    // be retrieved with {@link #instructionForId(int)} as the id is not simply an index into this array.
    ///
    // @field
    private LIRInstruction[] ___opIdToInstructionMap;

    ///
    // Map from an instruction {@linkplain LIRInstruction#id id} to the
    // {@linkplain AbstractBlockBase block} containing the instruction. Entries should be retrieved
    // with {@link #blockForId(int)} as the id is not simply an index into this array.
    ///
    // @field
    private AbstractBlockBase<?>[] ___opIdToBlockMap;

    ///
    // The {@linkplain #operandNumber(Value) number} of the first variable operand allocated.
    ///
    // @field
    private final int ___firstVariableNumber;
    ///
    // Number of variables.
    ///
    // @field
    private int ___numVariables;
    // @field
    private final boolean ___neverSpillConstants;

    ///
    // Sentinel interval to denote the end of an interval list.
    ///
    // @field
    protected final Interval ___intervalEndMarker;
    // @field
    public final Range ___rangeEndMarker;
    // @field
    private final LIRGenerationResult ___res;

    // @cons LinearScan
    protected LinearScan(TargetDescription __target, LIRGenerationResult __res, LIRGeneratorTool.MoveFactory __spillMoveFactory, RegisterAllocationConfig __regAllocConfig, AbstractBlockBase<?>[] __sortedBlocks, boolean __neverSpillConstants)
    {
        super();
        this.___ir = __res.getLIR();
        this.___res = __res;
        this.___moveFactory = __spillMoveFactory;
        this.___frameMapBuilder = __res.getFrameMapBuilder();
        this.___sortedBlocks = __sortedBlocks;
        this.___registerAttributes = __regAllocConfig.getRegisterConfig().getAttributesMap();
        this.___regAllocConfig = __regAllocConfig;

        this.___registers = __target.arch.getRegisters();
        this.___firstVariableNumber = getRegisters().size();
        this.___numVariables = this.___ir.numVariables();
        this.___blockData = new BlockMap<>(this.___ir.getControlFlowGraph());
        this.___neverSpillConstants = __neverSpillConstants;
        this.___rangeEndMarker = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE, null);
        this.___intervalEndMarker = new Interval(Value.ILLEGAL, Interval.END_MARKER_OPERAND_NUMBER, null, this.___rangeEndMarker);
        this.___intervalEndMarker.___next = this.___intervalEndMarker;
    }

    public LIRGenerationResult getLIRGenerationResult()
    {
        return this.___res;
    }

    public Interval intervalEndMarker()
    {
        return this.___intervalEndMarker;
    }

    public int getFirstLirInstructionId(AbstractBlockBase<?> __block)
    {
        return this.___ir.getLIRforBlock(__block).get(0).id();
    }

    public int getLastLirInstructionId(AbstractBlockBase<?> __block)
    {
        ArrayList<LIRInstruction> __instructions = this.___ir.getLIRforBlock(__block);
        return __instructions.get(__instructions.size() - 1).id();
    }

    public LIRGeneratorTool.MoveFactory getSpillMoveFactory()
    {
        return this.___moveFactory;
    }

    protected MoveResolver createMoveResolver()
    {
        return new MoveResolver(this);
    }

    public static boolean isVariableOrRegister(Value __value)
    {
        return LIRValueUtil.isVariable(__value) || ValueUtil.isRegister(__value);
    }

    ///
    // Converts an operand (variable or register) to an index in a flat address space covering all
    // the {@linkplain Variable variables} and {@linkplain RegisterValue registers} being processed
    // by this allocator.
    ///
    int operandNumber(Value __operand)
    {
        if (ValueUtil.isRegister(__operand))
        {
            return ValueUtil.asRegister(__operand).number;
        }
        return this.___firstVariableNumber + ((Variable) __operand).___index;
    }

    ///
    // Gets the number of operands. This value will increase by 1 for new variable.
    ///
    int operandSize()
    {
        return this.___firstVariableNumber + this.___numVariables;
    }

    ///
    // Gets the highest operand number for a register operand. This value will never change.
    ///
    int maxRegisterNumber()
    {
        return this.___firstVariableNumber - 1;
    }

    public LinearScan.BlockData getBlockData(AbstractBlockBase<?> __block)
    {
        return this.___blockData.get(__block);
    }

    void initBlockData(AbstractBlockBase<?> __block)
    {
        this.___blockData.put(__block, new LinearScan.BlockData());
    }

    // @closure
    static final LinearScan.IntervalPredicate IS_PRECOLORED_INTERVAL = new LinearScan.IntervalPredicate()
    {
        @Override
        public boolean apply(Interval __i)
        {
            return ValueUtil.isRegister(__i.___operand);
        }
    };

    // @closure
    static final LinearScan.IntervalPredicate IS_VARIABLE_INTERVAL = new LinearScan.IntervalPredicate()
    {
        @Override
        public boolean apply(Interval __i)
        {
            return LIRValueUtil.isVariable(__i.___operand);
        }
    };

    // @closure
    static final LinearScan.IntervalPredicate IS_STACK_INTERVAL = new LinearScan.IntervalPredicate()
    {
        @Override
        public boolean apply(Interval __i)
        {
            return !ValueUtil.isRegister(__i.___operand);
        }
    };

    ///
    // Gets an object describing the attributes of a given register according to this register configuration.
    ///
    public RegisterAttributes attributes(Register __reg)
    {
        return this.___registerAttributes[__reg.number];
    }

    void assignSpillSlot(Interval __interval)
    {
        // Assign the canonical spill slot of the parent (if a part of the interval is already
        // spilled) or allocate a new spill slot.
        if (__interval.canMaterialize())
        {
            __interval.assignLocation(Value.ILLEGAL);
        }
        else if (__interval.spillSlot() != null)
        {
            __interval.assignLocation(__interval.spillSlot());
        }
        else
        {
            VirtualStackSlot __slot = this.___frameMapBuilder.allocateSpillSlot(__interval.kind());
            __interval.setSpillSlot(__slot);
            __interval.assignLocation(__slot);
        }
    }

    ///
    // Map from {@linkplain #operandNumber(Value) operand numbers} to intervals.
    ///
    public Interval[] intervals()
    {
        return this.___intervals;
    }

    void initIntervals()
    {
        this.___intervalsSize = operandSize();
        this.___intervals = new Interval[this.___intervalsSize + (this.___intervalsSize >> SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT)];
    }

    ///
    // Creates a new interval.
    //
    // @param operand the operand for the interval
    // @return the created interval
    ///
    Interval createInterval(AllocatableValue __operand)
    {
        int __operandNumber = operandNumber(__operand);
        Interval __interval = new Interval(__operand, __operandNumber, this.___intervalEndMarker, this.___rangeEndMarker);
        this.___intervals[__operandNumber] = __interval;
        return __interval;
    }

    ///
    // Creates an interval as a result of splitting or spilling another interval.
    //
    // @param source an interval being split of spilled
    // @return a new interval derived from {@code source}
    ///
    Interval createDerivedInterval(Interval __source)
    {
        if (this.___firstDerivedIntervalIndex == -1)
        {
            this.___firstDerivedIntervalIndex = this.___intervalsSize;
        }
        if (this.___intervalsSize == this.___intervals.length)
        {
            this.___intervals = Arrays.copyOf(this.___intervals, this.___intervals.length + (this.___intervals.length >> SPLIT_INTERVALS_CAPACITY_RIGHT_SHIFT) + 1);
        }
        this.___intervalsSize++;
        // note: these variables are not managed and must therefore never be inserted into the LIR
        Variable __variable = new Variable(__source.kind(), this.___numVariables++);

        return createInterval(__variable);
    }

    // access to block list (sorted in linear scan order)
    public int blockCount()
    {
        return this.___sortedBlocks.length;
    }

    public AbstractBlockBase<?> blockAt(int __index)
    {
        return this.___sortedBlocks[__index];
    }

    ///
    // Gets the size of the {@link LinearScan.BlockData#liveIn} and {@link LinearScan.BlockData#liveOut} sets
    // for a basic block. These sets do not include any operands allocated as a result of creating
    // {@linkplain #createDerivedInterval(Interval) derived intervals}.
    ///
    public int liveSetSize()
    {
        return this.___firstDerivedIntervalIndex == -1 ? operandSize() : this.___firstDerivedIntervalIndex;
    }

    int numLoops()
    {
        return this.___ir.getControlFlowGraph().getLoops().size();
    }

    Interval intervalFor(int __operandNumber)
    {
        return this.___intervals[__operandNumber];
    }

    public Interval intervalFor(Value __operand)
    {
        int __operandNumber = operandNumber(__operand);
        return this.___intervals[__operandNumber];
    }

    public Interval getOrCreateInterval(AllocatableValue __operand)
    {
        Interval __ret = intervalFor(__operand);
        if (__ret == null)
        {
            return createInterval(__operand);
        }
        else
        {
            return __ret;
        }
    }

    void initOpIdMaps(int __numInstructions)
    {
        this.___opIdToInstructionMap = new LIRInstruction[__numInstructions];
        this.___opIdToBlockMap = new AbstractBlockBase<?>[__numInstructions];
    }

    void putOpIdMaps(int __index, LIRInstruction __op, AbstractBlockBase<?> __block)
    {
        this.___opIdToInstructionMap[__index] = __op;
        this.___opIdToBlockMap[__index] = __block;
    }

    ///
    // Gets the highest instruction id allocated by this object.
    ///
    int maxOpId()
    {
        return (this.___opIdToInstructionMap.length - 1) << 1;
    }

    ///
    // Converts an {@linkplain LIRInstruction#id instruction id} to an instruction index. All LIR
    // instructions in a method have an index one greater than their linear-scan order predecessor
    // with the first instruction having an index of 0.
    ///
    private static int opIdToIndex(int __opId)
    {
        return __opId >> 1;
    }

    ///
    // Retrieves the {@link LIRInstruction} based on its {@linkplain LIRInstruction#id id}.
    //
    // @param opId an instruction {@linkplain LIRInstruction#id id}
    // @return the instruction whose {@linkplain LIRInstruction#id} {@code == id}
    ///
    public LIRInstruction instructionForId(int __opId)
    {
        return this.___opIdToInstructionMap[opIdToIndex(__opId)];
    }

    ///
    // Gets the block containing a given instruction.
    //
    // @param opId an instruction {@linkplain LIRInstruction#id id}
    // @return the block containing the instruction denoted by {@code opId}
    ///
    public AbstractBlockBase<?> blockForId(int __opId)
    {
        return this.___opIdToBlockMap[opIdToIndex(__opId)];
    }

    boolean isBlockBegin(int __opId)
    {
        return __opId == 0 || blockForId(__opId) != blockForId(__opId - 1);
    }

    boolean coversBlockBegin(int __opId1, int __opId2)
    {
        return blockForId(__opId1) != blockForId(__opId2);
    }

    ///
    // Determines if an {@link LIRInstruction} destroys all caller saved registers.
    //
    // @param opId an instruction {@linkplain LIRInstruction#id id}
    // @return {@code true} if the instruction denoted by {@code id} destroys all caller saved registers.
    ///
    boolean hasCall(int __opId)
    {
        return instructionForId(__opId).destroysCallerSavedRegisters();
    }

    // @class LinearScan.IntervalPredicate
    abstract static class IntervalPredicate
    {
        abstract boolean apply(Interval __i);
    }

    public boolean isProcessed(Value __operand)
    {
        return !ValueUtil.isRegister(__operand) || attributes(ValueUtil.asRegister(__operand)).isAllocatable();
    }

    // * Phase 5: actual register allocation

    private static boolean isSorted(Interval[] __intervals)
    {
        int __from = -1;
        for (Interval __interval : __intervals)
        {
            __from = __interval.from();
        }
        return true;
    }

    static Interval addToList(Interval __first, Interval __prev, Interval __interval)
    {
        Interval __newFirst = __first;
        if (__prev != null)
        {
            __prev.___next = __interval;
        }
        else
        {
            __newFirst = __interval;
        }
        return __newFirst;
    }

    Pair<Interval, Interval> createUnhandledLists(LinearScan.IntervalPredicate __isList1, LinearScan.IntervalPredicate __isList2)
    {
        Interval __list1 = this.___intervalEndMarker;
        Interval __list2 = this.___intervalEndMarker;

        Interval __list1Prev = null;
        Interval __list2Prev = null;
        Interval __v;

        int __n = this.___sortedIntervals.length;
        for (int __i = 0; __i < __n; __i++)
        {
            __v = this.___sortedIntervals[__i];
            if (__v == null)
            {
                continue;
            }

            if (__isList1.apply(__v))
            {
                __list1 = addToList(__list1, __list1Prev, __v);
                __list1Prev = __v;
            }
            else if (__isList2 == null || __isList2.apply(__v))
            {
                __list2 = addToList(__list2, __list2Prev, __v);
                __list2Prev = __v;
            }
        }

        if (__list1Prev != null)
        {
            __list1Prev.___next = this.___intervalEndMarker;
        }
        if (__list2Prev != null)
        {
            __list2Prev.___next = this.___intervalEndMarker;
        }

        return Pair.create(__list1, __list2);
    }

    protected void sortIntervalsBeforeAllocation()
    {
        int __sortedLen = 0;
        for (Interval __interval : this.___intervals)
        {
            if (__interval != null)
            {
                __sortedLen++;
            }
        }

        Interval[] __sortedList = new Interval[__sortedLen];
        int __sortedIdx = 0;
        int __sortedFromMax = -1;

        // special sorting algorithm: the original interval-list is almost sorted,
        // only some intervals are swapped. So this is much faster than a complete QuickSort
        for (Interval __interval : this.___intervals)
        {
            if (__interval != null)
            {
                int __from = __interval.from();

                if (__sortedFromMax <= __from)
                {
                    __sortedList[__sortedIdx++] = __interval;
                    __sortedFromMax = __interval.from();
                }
                else
                {
                    // the assumption that the intervals are already sorted failed,
                    // so this interval must be sorted in manually
                    int __j;
                    for (__j = __sortedIdx - 1; __j >= 0 && __from < __sortedList[__j].from(); __j--)
                    {
                        __sortedList[__j + 1] = __sortedList[__j];
                    }
                    __sortedList[__j + 1] = __interval;
                    __sortedIdx++;
                }
            }
        }
        this.___sortedIntervals = __sortedList;
    }

    void sortIntervalsAfterAllocation()
    {
        if (this.___firstDerivedIntervalIndex == -1)
        {
            // no intervals have been added during allocation, so sorted list is already up to date
            return;
        }

        Interval[] __oldList = this.___sortedIntervals;
        Interval[] __newList = Arrays.copyOfRange(this.___intervals, this.___firstDerivedIntervalIndex, this.___intervalsSize);
        int __oldLen = __oldList.length;
        int __newLen = __newList.length;

        // conventional sort-algorithm for new intervals
        Arrays.sort(__newList, (Interval __a, Interval __b) -> __a.from() - __b.from());

        // merge old and new list (both already sorted) into one combined list
        Interval[] __combinedList = new Interval[__oldLen + __newLen];
        int __oldIdx = 0;
        int __newIdx = 0;

        while (__oldIdx + __newIdx < __combinedList.length)
        {
            if (__newIdx >= __newLen || (__oldIdx < __oldLen && __oldList[__oldIdx].from() <= __newList[__newIdx].from()))
            {
                __combinedList[__oldIdx + __newIdx] = __oldList[__oldIdx];
                __oldIdx++;
            }
            else
            {
                __combinedList[__oldIdx + __newIdx] = __newList[__newIdx];
                __newIdx++;
            }
        }

        this.___sortedIntervals = __combinedList;
    }

    // wrapper for Interval.splitChildAtOpId that performs a bailout in product mode
    // instead of returning null
    public Interval splitChildAtOpId(Interval __interval, int __opId, LIRInstruction.OperandMode __mode)
    {
        Interval __result = __interval.getSplitChildAtOpId(__opId, __mode, this);

        if (__result != null)
        {
            return __result;
        }
        throw new GraalError("linear scan: interval is null");
    }

    static AllocatableValue canonicalSpillOpr(Interval __interval)
    {
        return __interval.spillSlot();
    }

    boolean isMaterialized(AllocatableValue __operand, int __opId, LIRInstruction.OperandMode __mode)
    {
        Interval __interval = intervalFor(__operand);

        if (__opId != -1)
        {
            // Operands are not changed when an interval is split during allocation, so search the right interval here.
            __interval = splitChildAtOpId(__interval, __opId, __mode);
        }

        return ValueUtil.isIllegal(__interval.location()) && __interval.canMaterialize();
    }

    boolean isCallerSave(Value __operand)
    {
        return attributes(ValueUtil.asRegister(__operand)).isCallerSave();
    }

    protected void allocate(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationPhase.AllocationContext __context)
    {
        createLifetimeAnalysisPhase().apply(__target, __lirGenRes, __context);

        sortIntervalsBeforeAllocation();

        createRegisterAllocationPhase().apply(__target, __lirGenRes, __context);

        if (GraalOptions.lirOptLSRAOptimizeSpillPosition)
        {
            createOptimizeSpillPositionPhase().apply(__target, __lirGenRes, __context);
        }
        createResolveDataFlowPhase().apply(__target, __lirGenRes, __context);

        sortIntervalsAfterAllocation();

        beforeSpillMoveElimination();
        createSpillMoveEliminationPhase().apply(__target, __lirGenRes, __context);
        createAssignLocationsPhase().apply(__target, __lirGenRes, __context);
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

    public LIR getLIR()
    {
        return this.___ir;
    }

    public FrameMapBuilder getFrameMapBuilder()
    {
        return this.___frameMapBuilder;
    }

    public AbstractBlockBase<?>[] sortedBlocks()
    {
        return this.___sortedBlocks;
    }

    public RegisterArray getRegisters()
    {
        return this.___registers;
    }

    public RegisterAllocationConfig getRegisterAllocationConfig()
    {
        return this.___regAllocConfig;
    }

    public boolean callKillsRegisters()
    {
        return this.___regAllocConfig.getRegisterConfig().areAllAllocatableRegistersCallerSaved();
    }

    boolean neverSpillConstants()
    {
        return this.___neverSpillConstants;
    }
}
