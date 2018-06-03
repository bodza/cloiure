package giraaff.lir.alloc.lsra;

import java.util.ArrayList;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.util.GraalError;

// @class MoveResolver
public class MoveResolver
{
    // @field
    private final LinearScan allocator;

    // @field
    private int insertIdx;
    // @field
    private LIRInsertionBuffer insertionBuffer; // buffer where moves are inserted

    // @field
    private final ArrayList<Interval> mappingFrom;
    // @field
    private final ArrayList<Constant> mappingFromOpr;
    // @field
    private final ArrayList<Interval> mappingTo;
    // @field
    private boolean multipleReadsAllowed;
    // @field
    private final int[] registerBlocked;

    // @field
    private final LIRGenerationResult res;

    protected void setValueBlocked(Value __location, int __direction)
    {
        if (ValueUtil.isRegister(__location))
        {
            registerBlocked[ValueUtil.asRegister(__location).number] += __direction;
        }
        else
        {
            throw GraalError.shouldNotReachHere("unhandled value " + __location);
        }
    }

    protected Interval getMappingFrom(int __i)
    {
        return mappingFrom.get(__i);
    }

    protected int mappingFromSize()
    {
        return mappingFrom.size();
    }

    protected int valueBlocked(Value __location)
    {
        if (ValueUtil.isRegister(__location))
        {
            return registerBlocked[ValueUtil.asRegister(__location).number];
        }
        throw GraalError.shouldNotReachHere("unhandled value " + __location);
    }

    void setMultipleReadsAllowed()
    {
        multipleReadsAllowed = true;
    }

    protected boolean areMultipleReadsAllowed()
    {
        return multipleReadsAllowed;
    }

    boolean hasMappings()
    {
        return mappingFrom.size() > 0;
    }

    protected LinearScan getAllocator()
    {
        return allocator;
    }

    // @cons
    protected MoveResolver(LinearScan __allocator)
    {
        super();
        this.allocator = __allocator;
        this.multipleReadsAllowed = false;
        this.mappingFrom = new ArrayList<>(8);
        this.mappingFromOpr = new ArrayList<>(8);
        this.mappingTo = new ArrayList<>(8);
        this.insertIdx = -1;
        this.insertionBuffer = new LIRInsertionBuffer();
        this.registerBlocked = new int[__allocator.getRegisters().size()];
        this.res = __allocator.getLIRGenerationResult();
    }

    // mark assignedReg and assignedRegHi of the interval as blocked
    private void blockRegisters(Interval __interval)
    {
        Value __location = __interval.location();
        if (mightBeBlocked(__location))
        {
            int __direction = 1;
            setValueBlocked(__location, __direction);
        }
    }

    // mark assignedReg and assignedRegHi of the interval as unblocked
    private void unblockRegisters(Interval __interval)
    {
        Value __location = __interval.location();
        if (mightBeBlocked(__location))
        {
            setValueBlocked(__location, -1);
        }
    }

    /**
     * Checks if the {@linkplain Interval#location() location} of {@code to} is not blocked or is
     * only blocked by {@code from}.
     */
    private boolean safeToProcessMove(Interval __from, Interval __to)
    {
        Value __fromReg = __from != null ? __from.location() : null;

        Value __location = __to.location();
        if (mightBeBlocked(__location))
        {
            if ((valueBlocked(__location) > 1 || (valueBlocked(__location) == 1 && !isMoveToSelf(__fromReg, __location))))
            {
                return false;
            }
        }

        return true;
    }

    protected boolean isMoveToSelf(Value __from, Value __to)
    {
        if (__to.equals(__from))
        {
            return true;
        }
        if (__from != null && ValueUtil.isRegister(__from) && ValueUtil.isRegister(__to) && ValueUtil.asRegister(__from).equals(ValueUtil.asRegister(__to)))
        {
            return true;
        }
        return false;
    }

    protected boolean mightBeBlocked(Value __location)
    {
        return ValueUtil.isRegister(__location);
    }

    private void createInsertionBuffer(ArrayList<LIRInstruction> __list)
    {
        insertionBuffer.init(__list);
    }

    private void appendInsertionBuffer()
    {
        if (insertionBuffer.initialized())
        {
            insertionBuffer.finish();
        }

        insertIdx = -1;
    }

    private LIRInstruction insertMove(Interval __fromInterval, Interval __toInterval)
    {
        LIRInstruction __move = createMove(__fromInterval.operand, __toInterval.operand, __fromInterval.location(), __toInterval.location());
        insertionBuffer.append(insertIdx, __move);

        return __move;
    }

    /**
     * @param fromOpr {@link Interval#operand operand} of the {@code from} interval
     * @param toOpr {@link Interval#operand operand} of the {@code to} interval
     * @param fromLocation {@link Interval#location() location} of the {@code to} interval
     * @param toLocation {@link Interval#location() location} of the {@code to} interval
     */
    protected LIRInstruction createMove(AllocatableValue __fromOpr, AllocatableValue __toOpr, AllocatableValue __fromLocation, AllocatableValue __toLocation)
    {
        return getAllocator().getSpillMoveFactory().createMove(__toOpr, __fromOpr);
    }

    private LIRInstruction insertMove(Constant __fromOpr, Interval __toInterval)
    {
        AllocatableValue __toOpr = __toInterval.operand;
        LIRInstruction __move;
        if (LIRValueUtil.isStackSlotValue(__toInterval.location()))
        {
            __move = getAllocator().getSpillMoveFactory().createStackLoad(__toOpr, __fromOpr);
        }
        else
        {
            __move = getAllocator().getSpillMoveFactory().createLoad(__toOpr, __fromOpr);
        }
        insertionBuffer.append(insertIdx, __move);

        return __move;
    }

    private void resolveMappings()
    {
        // Block all registers that are used as input operands of a move.
        // When a register is blocked, no move to this register is emitted.
        // This is necessary for detecting cycles in moves.
        int __i;
        for (__i = mappingFrom.size() - 1; __i >= 0; __i--)
        {
            Interval __fromInterval = mappingFrom.get(__i);
            if (__fromInterval != null)
            {
                blockRegisters(__fromInterval);
            }
        }

        ArrayList<AllocatableValue> __busySpillSlots = null;
        while (mappingFrom.size() > 0)
        {
            boolean __processedInterval = false;

            int __spillCandidate = -1;
            for (__i = mappingFrom.size() - 1; __i >= 0; __i--)
            {
                Interval __fromInterval = mappingFrom.get(__i);
                Interval __toInterval = mappingTo.get(__i);

                if (safeToProcessMove(__fromInterval, __toInterval))
                {
                    // this interval can be processed because target is free
                    final LIRInstruction __move;
                    if (__fromInterval != null)
                    {
                        __move = insertMove(__fromInterval, __toInterval);
                        unblockRegisters(__fromInterval);
                    }
                    else
                    {
                        __move = insertMove(mappingFromOpr.get(__i), __toInterval);
                    }
                    if (LIRValueUtil.isStackSlotValue(__toInterval.location()))
                    {
                        if (__busySpillSlots == null)
                        {
                            __busySpillSlots = new ArrayList<>(2);
                        }
                        __busySpillSlots.add(__toInterval.location());
                    }
                    mappingFrom.remove(__i);
                    mappingFromOpr.remove(__i);
                    mappingTo.remove(__i);

                    __processedInterval = true;
                }
                else if (__fromInterval != null && ValueUtil.isRegister(__fromInterval.location()) && (__busySpillSlots == null || !__busySpillSlots.contains(__fromInterval.spillSlot())))
                {
                    // this interval cannot be processed now because target is not free
                    // it starts in a register, so it is a possible candidate for spilling
                    __spillCandidate = __i;
                }
            }

            if (!__processedInterval)
            {
                breakCycle(__spillCandidate);
            }
        }

        // reset to default value
        multipleReadsAllowed = false;
    }

    protected void breakCycle(int __spillCandidate)
    {
        // no move could be processed because there is a cycle in the move list
        // (e.g. r1 . r2, r2 . r1), so one interval must be spilled to memory

        // create a new spill interval and assign a stack slot to it
        Interval __fromInterval = mappingFrom.get(__spillCandidate);
        // do not allocate a new spill slot for temporary interval, but
        // use spill slot assigned to fromInterval. Otherwise moves from
        // one stack slot to another can happen (not allowed by LIRAssembler
        AllocatableValue __spillSlot = __fromInterval.spillSlot();
        if (__spillSlot == null)
        {
            __spillSlot = getAllocator().getFrameMapBuilder().allocateSpillSlot(__fromInterval.kind());
            __fromInterval.setSpillSlot(__spillSlot);
        }
        spillInterval(__spillCandidate, __fromInterval, __spillSlot);
    }

    protected void spillInterval(int __spillCandidate, Interval __fromInterval, AllocatableValue __spillSlot)
    {
        Interval __spillInterval = getAllocator().createDerivedInterval(__fromInterval);
        __spillInterval.setKind(__fromInterval.kind());

        // add a dummy range because real position is difficult to calculate
        // note: this range is a special case when the integrity of the allocation is checked
        __spillInterval.addRange(1, 2);

        __spillInterval.assignLocation(__spillSlot);

        blockRegisters(__spillInterval);

        // insert a move from register to stack and update the mapping
        LIRInstruction __move = insertMove(__fromInterval, __spillInterval);
        mappingFrom.set(__spillCandidate, __spillInterval);
        unblockRegisters(__fromInterval);
    }

    void setInsertPosition(ArrayList<LIRInstruction> __insertList, int __insertIdx)
    {
        createInsertionBuffer(__insertList);
        this.insertIdx = __insertIdx;
    }

    void moveInsertPosition(ArrayList<LIRInstruction> __newInsertList, int __newInsertIdx)
    {
        if (insertionBuffer.lirList() != null && (insertionBuffer.lirList() != __newInsertList || this.insertIdx != __newInsertIdx))
        {
            // insert position changed . resolve current mappings
            resolveMappings();
        }

        if (insertionBuffer.lirList() != __newInsertList)
        {
            // block changed . append insertionBuffer because it is
            // bound to a specific block and create a new insertionBuffer
            appendInsertionBuffer();
            createInsertionBuffer(__newInsertList);
        }

        this.insertIdx = __newInsertIdx;
    }

    public void addMapping(Interval __fromInterval, Interval __toInterval)
    {
        if (ValueUtil.isIllegal(__toInterval.location()) && __toInterval.canMaterialize())
        {
            return;
        }
        if (ValueUtil.isIllegal(__fromInterval.location()) && __fromInterval.canMaterialize())
        {
            // instead of a reload, re-materialize the value
            Constant __rematValue = __fromInterval.getMaterializedValue();
            addMapping(__rematValue, __toInterval);
            return;
        }

        mappingFrom.add(__fromInterval);
        mappingFromOpr.add(null);
        mappingTo.add(__toInterval);
    }

    public void addMapping(Constant __fromOpr, Interval __toInterval)
    {
        mappingFrom.add(null);
        mappingFromOpr.add(__fromOpr);
        mappingTo.add(__toInterval);
    }

    void resolveAndAppendMoves()
    {
        if (hasMappings())
        {
            resolveMappings();
        }
        appendInsertionBuffer();
    }
}
