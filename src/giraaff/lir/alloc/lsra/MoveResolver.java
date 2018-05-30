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
    private final LinearScan allocator;

    private int insertIdx;
    private LIRInsertionBuffer insertionBuffer; // buffer where moves are inserted

    private final ArrayList<Interval> mappingFrom;
    private final ArrayList<Constant> mappingFromOpr;
    private final ArrayList<Interval> mappingTo;
    private boolean multipleReadsAllowed;
    private final int[] registerBlocked;

    private final LIRGenerationResult res;

    protected void setValueBlocked(Value location, int direction)
    {
        if (ValueUtil.isRegister(location))
        {
            registerBlocked[ValueUtil.asRegister(location).number] += direction;
        }
        else
        {
            throw GraalError.shouldNotReachHere("unhandled value " + location);
        }
    }

    protected Interval getMappingFrom(int i)
    {
        return mappingFrom.get(i);
    }

    protected int mappingFromSize()
    {
        return mappingFrom.size();
    }

    protected int valueBlocked(Value location)
    {
        if (ValueUtil.isRegister(location))
        {
            return registerBlocked[ValueUtil.asRegister(location).number];
        }
        throw GraalError.shouldNotReachHere("unhandled value " + location);
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
    protected MoveResolver(LinearScan allocator)
    {
        super();
        this.allocator = allocator;
        this.multipleReadsAllowed = false;
        this.mappingFrom = new ArrayList<>(8);
        this.mappingFromOpr = new ArrayList<>(8);
        this.mappingTo = new ArrayList<>(8);
        this.insertIdx = -1;
        this.insertionBuffer = new LIRInsertionBuffer();
        this.registerBlocked = new int[allocator.getRegisters().size()];
        this.res = allocator.getLIRGenerationResult();
    }

    // mark assignedReg and assignedRegHi of the interval as blocked
    private void blockRegisters(Interval interval)
    {
        Value location = interval.location();
        if (mightBeBlocked(location))
        {
            int direction = 1;
            setValueBlocked(location, direction);
        }
    }

    // mark assignedReg and assignedRegHi of the interval as unblocked
    private void unblockRegisters(Interval interval)
    {
        Value location = interval.location();
        if (mightBeBlocked(location))
        {
            setValueBlocked(location, -1);
        }
    }

    /**
     * Checks if the {@linkplain Interval#location() location} of {@code to} is not blocked or is
     * only blocked by {@code from}.
     */
    private boolean safeToProcessMove(Interval from, Interval to)
    {
        Value fromReg = from != null ? from.location() : null;

        Value location = to.location();
        if (mightBeBlocked(location))
        {
            if ((valueBlocked(location) > 1 || (valueBlocked(location) == 1 && !isMoveToSelf(fromReg, location))))
            {
                return false;
            }
        }

        return true;
    }

    protected boolean isMoveToSelf(Value from, Value to)
    {
        if (to.equals(from))
        {
            return true;
        }
        if (from != null && ValueUtil.isRegister(from) && ValueUtil.isRegister(to) && ValueUtil.asRegister(from).equals(ValueUtil.asRegister(to)))
        {
            return true;
        }
        return false;
    }

    protected boolean mightBeBlocked(Value location)
    {
        return ValueUtil.isRegister(location);
    }

    private void createInsertionBuffer(ArrayList<LIRInstruction> list)
    {
        insertionBuffer.init(list);
    }

    private void appendInsertionBuffer()
    {
        if (insertionBuffer.initialized())
        {
            insertionBuffer.finish();
        }

        insertIdx = -1;
    }

    private LIRInstruction insertMove(Interval fromInterval, Interval toInterval)
    {
        LIRInstruction move = createMove(fromInterval.operand, toInterval.operand, fromInterval.location(), toInterval.location());
        insertionBuffer.append(insertIdx, move);

        return move;
    }

    /**
     * @param fromOpr {@link Interval#operand operand} of the {@code from} interval
     * @param toOpr {@link Interval#operand operand} of the {@code to} interval
     * @param fromLocation {@link Interval#location() location} of the {@code to} interval
     * @param toLocation {@link Interval#location() location} of the {@code to} interval
     */
    protected LIRInstruction createMove(AllocatableValue fromOpr, AllocatableValue toOpr, AllocatableValue fromLocation, AllocatableValue toLocation)
    {
        return getAllocator().getSpillMoveFactory().createMove(toOpr, fromOpr);
    }

    private LIRInstruction insertMove(Constant fromOpr, Interval toInterval)
    {
        AllocatableValue toOpr = toInterval.operand;
        LIRInstruction move;
        if (LIRValueUtil.isStackSlotValue(toInterval.location()))
        {
            move = getAllocator().getSpillMoveFactory().createStackLoad(toOpr, fromOpr);
        }
        else
        {
            move = getAllocator().getSpillMoveFactory().createLoad(toOpr, fromOpr);
        }
        insertionBuffer.append(insertIdx, move);

        return move;
    }

    private void resolveMappings()
    {
        // Block all registers that are used as input operands of a move.
        // When a register is blocked, no move to this register is emitted.
        // This is necessary for detecting cycles in moves.
        int i;
        for (i = mappingFrom.size() - 1; i >= 0; i--)
        {
            Interval fromInterval = mappingFrom.get(i);
            if (fromInterval != null)
            {
                blockRegisters(fromInterval);
            }
        }

        ArrayList<AllocatableValue> busySpillSlots = null;
        while (mappingFrom.size() > 0)
        {
            boolean processedInterval = false;

            int spillCandidate = -1;
            for (i = mappingFrom.size() - 1; i >= 0; i--)
            {
                Interval fromInterval = mappingFrom.get(i);
                Interval toInterval = mappingTo.get(i);

                if (safeToProcessMove(fromInterval, toInterval))
                {
                    // this interval can be processed because target is free
                    final LIRInstruction move;
                    if (fromInterval != null)
                    {
                        move = insertMove(fromInterval, toInterval);
                        unblockRegisters(fromInterval);
                    }
                    else
                    {
                        move = insertMove(mappingFromOpr.get(i), toInterval);
                    }
                    if (LIRValueUtil.isStackSlotValue(toInterval.location()))
                    {
                        if (busySpillSlots == null)
                        {
                            busySpillSlots = new ArrayList<>(2);
                        }
                        busySpillSlots.add(toInterval.location());
                    }
                    mappingFrom.remove(i);
                    mappingFromOpr.remove(i);
                    mappingTo.remove(i);

                    processedInterval = true;
                }
                else if (fromInterval != null && ValueUtil.isRegister(fromInterval.location()) && (busySpillSlots == null || !busySpillSlots.contains(fromInterval.spillSlot())))
                {
                    // this interval cannot be processed now because target is not free
                    // it starts in a register, so it is a possible candidate for spilling
                    spillCandidate = i;
                }
            }

            if (!processedInterval)
            {
                breakCycle(spillCandidate);
            }
        }

        // reset to default value
        multipleReadsAllowed = false;
    }

    protected void breakCycle(int spillCandidate)
    {
        // no move could be processed because there is a cycle in the move list
        // (e.g. r1 . r2, r2 . r1), so one interval must be spilled to memory

        // create a new spill interval and assign a stack slot to it
        Interval fromInterval = mappingFrom.get(spillCandidate);
        // do not allocate a new spill slot for temporary interval, but
        // use spill slot assigned to fromInterval. Otherwise moves from
        // one stack slot to another can happen (not allowed by LIRAssembler
        AllocatableValue spillSlot = fromInterval.spillSlot();
        if (spillSlot == null)
        {
            spillSlot = getAllocator().getFrameMapBuilder().allocateSpillSlot(fromInterval.kind());
            fromInterval.setSpillSlot(spillSlot);
        }
        spillInterval(spillCandidate, fromInterval, spillSlot);
    }

    protected void spillInterval(int spillCandidate, Interval fromInterval, AllocatableValue spillSlot)
    {
        Interval spillInterval = getAllocator().createDerivedInterval(fromInterval);
        spillInterval.setKind(fromInterval.kind());

        // add a dummy range because real position is difficult to calculate
        // note: this range is a special case when the integrity of the allocation is checked
        spillInterval.addRange(1, 2);

        spillInterval.assignLocation(spillSlot);

        blockRegisters(spillInterval);

        // insert a move from register to stack and update the mapping
        LIRInstruction move = insertMove(fromInterval, spillInterval);
        mappingFrom.set(spillCandidate, spillInterval);
        unblockRegisters(fromInterval);
    }

    void setInsertPosition(ArrayList<LIRInstruction> insertList, int insertIdx)
    {
        createInsertionBuffer(insertList);
        this.insertIdx = insertIdx;
    }

    void moveInsertPosition(ArrayList<LIRInstruction> newInsertList, int newInsertIdx)
    {
        if (insertionBuffer.lirList() != null && (insertionBuffer.lirList() != newInsertList || this.insertIdx != newInsertIdx))
        {
            // insert position changed . resolve current mappings
            resolveMappings();
        }

        if (insertionBuffer.lirList() != newInsertList)
        {
            // block changed . append insertionBuffer because it is
            // bound to a specific block and create a new insertionBuffer
            appendInsertionBuffer();
            createInsertionBuffer(newInsertList);
        }

        this.insertIdx = newInsertIdx;
    }

    public void addMapping(Interval fromInterval, Interval toInterval)
    {
        if (ValueUtil.isIllegal(toInterval.location()) && toInterval.canMaterialize())
        {
            return;
        }
        if (ValueUtil.isIllegal(fromInterval.location()) && fromInterval.canMaterialize())
        {
            // instead of a reload, re-materialize the value
            Constant rematValue = fromInterval.getMaterializedValue();
            addMapping(rematValue, toInterval);
            return;
        }

        mappingFrom.add(fromInterval);
        mappingFromOpr.add(null);
        mappingTo.add(toInterval);
    }

    public void addMapping(Constant fromOpr, Interval toInterval)
    {
        mappingFrom.add(null);
        mappingFromOpr.add(fromOpr);
        mappingTo.add(toInterval);
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
