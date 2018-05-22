package graalvm.compiler.lir.alloc.trace.lsra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRValueUtil;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.FrameMapBuilderTool;

final class TraceLocalMoveResolver
{
    private static final int STACK_SLOT_IN_CALLER_FRAME_IDX = -1;
    private final TraceLinearScan allocator;

    private int insertIdx;
    private LIRInsertionBuffer insertionBuffer; // buffer where moves are inserted

    private final ArrayList<TraceInterval> mappingFrom;
    private final ArrayList<Constant> mappingFromOpr;
    private final ArrayList<TraceInterval> mappingTo;
    private final int[] registerBlocked;

    private int[] stackBlocked;
    private final int firstVirtualStackIndex;

    private int getStackArrayIndex(Value stackSlotValue)
    {
        if (ValueUtil.isStackSlot(stackSlotValue))
        {
            return getStackArrayIndex(ValueUtil.asStackSlot(stackSlotValue));
        }
        if (LIRValueUtil.isVirtualStackSlot(stackSlotValue))
        {
            return getStackArrayIndex(LIRValueUtil.asVirtualStackSlot(stackSlotValue));
        }
        throw GraalError.shouldNotReachHere("value is not a stack slot: " + stackSlotValue);
    }

    private int getStackArrayIndex(StackSlot stackSlot)
    {
        int stackIdx;
        if (stackSlot.isInCallerFrame())
        {
            // incoming stack arguments can be ignored
            stackIdx = STACK_SLOT_IN_CALLER_FRAME_IDX;
        }
        else
        {
            int offset = -stackSlot.getRawOffset();
            stackIdx = offset;
        }
        return stackIdx;
    }

    private int getStackArrayIndex(VirtualStackSlot virtualStackSlot)
    {
        return firstVirtualStackIndex + virtualStackSlot.getId();
    }

    protected void setValueBlocked(Value location, int direction)
    {
        if (LIRValueUtil.isStackSlotValue(location))
        {
            int stackIdx = getStackArrayIndex(location);
            if (stackIdx == STACK_SLOT_IN_CALLER_FRAME_IDX)
            {
                // incoming stack arguments can be ignored
                return;
            }
            if (stackIdx >= stackBlocked.length)
            {
                stackBlocked = Arrays.copyOf(stackBlocked, stackIdx + 1);
            }
            stackBlocked[stackIdx] += direction;
        }
        else
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
    }

    protected TraceInterval getMappingFrom(int i)
    {
        return mappingFrom.get(i);
    }

    protected int mappingFromSize()
    {
        return mappingFrom.size();
    }

    protected int valueBlocked(Value location)
    {
        if (LIRValueUtil.isStackSlotValue(location))
        {
            int stackIdx = getStackArrayIndex(location);
            if (stackIdx == STACK_SLOT_IN_CALLER_FRAME_IDX)
            {
                // incoming stack arguments are always blocked (aka they can not be written)
                return 1;
            }
            if (stackIdx >= stackBlocked.length)
            {
                return 0;
            }
            return stackBlocked[stackIdx];
        }
        if (ValueUtil.isRegister(location))
        {
            return registerBlocked[ValueUtil.asRegister(location).number];
        }
        throw GraalError.shouldNotReachHere("unhandled value " + location);
    }

    /*
     * TODO (je) remove?
     */
    protected static boolean areMultipleReadsAllowed()
    {
        return true;
    }

    boolean hasMappings()
    {
        return mappingFrom.size() > 0;
    }

    protected TraceLinearScan getAllocator()
    {
        return allocator;
    }

    protected TraceLocalMoveResolver(TraceLinearScan allocator)
    {
        this.allocator = allocator;
        this.mappingFrom = new ArrayList<>(8);
        this.mappingFromOpr = new ArrayList<>(8);
        this.mappingTo = new ArrayList<>(8);
        this.insertIdx = -1;
        this.insertionBuffer = new LIRInsertionBuffer();
        this.registerBlocked = new int[allocator.getRegisters().size()];
        FrameMapBuilderTool frameMapBuilderTool = (FrameMapBuilderTool) allocator.getFrameMapBuilder();
        FrameMap frameMap = frameMapBuilderTool.getFrameMap();
        this.stackBlocked = new int[frameMapBuilderTool.getNumberOfStackSlots()];
        this.firstVirtualStackIndex = !frameMap.frameNeedsAllocating() ? 0 : frameMap.currentFrameSize() + 1;
    }

    // mark assignedReg and assignedRegHi of the interval as blocked
    private void blockRegisters(TraceInterval interval)
    {
        Value location = interval.location();
        if (mightBeBlocked(location))
        {
            int direction = 1;
            setValueBlocked(location, direction);
        }
    }

    // mark assignedReg and assignedRegHi of the interval as unblocked
    private void unblockRegisters(TraceInterval interval)
    {
        Value location = interval.location();
        if (mightBeBlocked(location))
        {
            setValueBlocked(location, -1);
        }
    }

    /**
     * Checks if the {@linkplain TraceInterval#location() location} of {@code to} is not blocked or
     * is only blocked by {@code from}.
     */
    private boolean safeToProcessMove(TraceInterval from, TraceInterval to)
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

    protected static boolean isMoveToSelf(Value from, Value to)
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

    protected static boolean mightBeBlocked(Value location)
    {
        if (ValueUtil.isRegister(location))
        {
            return true;
        }
        if (LIRValueUtil.isStackSlotValue(location))
        {
            return true;
        }
        return false;
    }

    private void createInsertionBuffer(List<LIRInstruction> list)
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

    private void insertMove(TraceInterval fromInterval, TraceInterval toInterval)
    {
        insertionBuffer.append(insertIdx, createMove(allocator.getOperand(fromInterval), allocator.getOperand(toInterval), fromInterval.location(), toInterval.location()));
    }

    /**
     * @param fromOpr {@link TraceInterval operand} of the {@code from} interval
     * @param toOpr {@link TraceInterval operand} of the {@code to} interval
     * @param fromLocation {@link TraceInterval#location() location} of the {@code to} interval
     * @param toLocation {@link TraceInterval#location() location} of the {@code to} interval
     */
    protected LIRInstruction createMove(AllocatableValue fromOpr, AllocatableValue toOpr, AllocatableValue fromLocation, AllocatableValue toLocation)
    {
        if (LIRValueUtil.isStackSlotValue(toLocation) && LIRValueUtil.isStackSlotValue(fromLocation))
        {
            return getAllocator().getSpillMoveFactory().createStackMove(toOpr, fromOpr);
        }
        return getAllocator().getSpillMoveFactory().createMove(toOpr, fromOpr);
    }

    private void insertMove(Constant fromOpr, TraceInterval toInterval)
    {
        AllocatableValue toOpr = allocator.getOperand(toInterval);
        LIRInstruction move = getAllocator().getSpillMoveFactory().createLoad(toOpr, fromOpr);
        insertionBuffer.append(insertIdx, move);
    }

    private void resolveMappings()
    {
        // Block all registers that are used as input operands of a move.
        // When a register is blocked, no move to this register is emitted.
        // This is necessary for detecting cycles in moves.
        int i;
        for (i = mappingFrom.size() - 1; i >= 0; i--)
        {
            TraceInterval fromInterval = mappingFrom.get(i);
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
                TraceInterval fromInterval = mappingFrom.get(i);
                TraceInterval toInterval = mappingTo.get(i);

                if (safeToProcessMove(fromInterval, toInterval))
                {
                    // this interval can be processed because target is free
                    if (fromInterval != null)
                    {
                        insertMove(fromInterval, toInterval);
                        unblockRegisters(fromInterval);
                    }
                    else
                    {
                        insertMove(mappingFromOpr.get(i), toInterval);
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
    }

    protected void breakCycle(int spillCandidate)
    {
        if (spillCandidate != -1)
        {
            // no move could be processed because there is a cycle in the move list
            // (e.g. r1 . r2, r2 . r1), so one interval must be spilled to memory

            // create a new spill interval and assign a stack slot to it
            TraceInterval fromInterval1 = mappingFrom.get(spillCandidate);
            // do not allocate a new spill slot for temporary interval, but
            // use spill slot assigned to fromInterval. Otherwise moves from
            // one stack slot to another can happen (not allowed by LIRAssembler
            AllocatableValue spillSlot1 = fromInterval1.spillSlot();
            if (spillSlot1 == null)
            {
                spillSlot1 = getAllocator().getFrameMapBuilder().allocateSpillSlot(allocator.getKind(fromInterval1));
                fromInterval1.setSpillSlot(spillSlot1);
            }
            spillInterval(spillCandidate, fromInterval1, spillSlot1);
            return;
        }
        // Arbitrarily select the first entry for spilling.
        int stackSpillCandidate = 0;
        TraceInterval fromInterval = getMappingFrom(stackSpillCandidate);
        // allocate new stack slot
        VirtualStackSlot spillSlot = getAllocator().getFrameMapBuilder().allocateSpillSlot(allocator.getKind(fromInterval));
        spillInterval(stackSpillCandidate, fromInterval, spillSlot);
    }

    protected void spillInterval(int spillCandidate, TraceInterval fromInterval, AllocatableValue spillSlot)
    {
        TraceInterval spillInterval = getAllocator().createDerivedInterval(fromInterval);

        // add a dummy range because real position is difficult to calculate
        // Note: this range is a special case when the integrity of the allocation is
        // checked
        spillInterval.addRange(1, 2);

        spillInterval.assignLocation(spillSlot);
        blockRegisters(spillInterval);

        // insert a move from register to stack and update the mapping
        insertMove(fromInterval, spillInterval);
        mappingFrom.set(spillCandidate, spillInterval);
        unblockRegisters(fromInterval);
    }

    void setInsertPosition(List<LIRInstruction> insertList, int insertIdx)
    {
        createInsertionBuffer(insertList);
        this.insertIdx = insertIdx;
    }

    void moveInsertPosition(List<LIRInstruction> newInsertList, int newInsertIdx)
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

    public void addMapping(TraceInterval fromInterval, TraceInterval toInterval)
    {
        if (ValueUtil.isIllegal(toInterval.location()) && toInterval.canMaterialize())
        {
            return;
        }
        if (ValueUtil.isIllegal(fromInterval.location()) && fromInterval.canMaterialize())
        {
            // Instead of a reload, re-materialize the value
            JavaConstant rematValue = fromInterval.getMaterializedValue();
            addMapping(rematValue, toInterval);
            return;
        }

        mappingFrom.add(fromInterval);
        mappingFromOpr.add(null);
        mappingTo.add(toInterval);
    }

    public void addMapping(Constant fromOpr, TraceInterval toInterval)
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
