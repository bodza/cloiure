package graalvm.compiler.lir.alloc.trace;

import static jdk.vm.ci.code.ValueUtil.asAllocatableValue;
import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.asStackSlot;
import static jdk.vm.ci.code.ValueUtil.isIllegal;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;
import static graalvm.compiler.lir.LIRValueUtil.asVirtualStackSlot;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;
import static graalvm.compiler.lir.alloc.trace.TraceUtil.asShadowedRegisterValue;
import static graalvm.compiler.lir.alloc.trace.TraceUtil.isShadowedRegisterValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.FrameMapBuilder;
import graalvm.compiler.lir.framemap.FrameMapBuilderTool;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

/**
 */
public final class TraceGlobalMoveResolver extends TraceGlobalMoveResolutionPhase.MoveResolver
{
    private int insertIdx;
    private LIRInsertionBuffer insertionBuffer; // buffer where moves are inserted

    private final ArrayList<Value> mappingFrom;
    private final ArrayList<Value> mappingFromStack;
    private final ArrayList<AllocatableValue> mappingTo;
    private final int[] registerBlocked;
    private static final int STACK_SLOT_IN_CALLER_FRAME_IDX = -1;
    private int[] stackBlocked;
    private final int firstVirtualStackIndex;
    private final MoveFactory spillMoveFactory;
    private final FrameMapBuilder frameMapBuilder;
    private final OptionValues options;
    private final RegisterAllocationConfig registerAllocationConfig;
    private final LIRGenerationResult res;

    private void setValueBlocked(Value location, int direction)
    {
        if (isStackSlotValue(location))
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
            if (isRegister(location))
            {
                registerBlocked[asRegister(location).number] += direction;
            }
            else
            {
                throw GraalError.shouldNotReachHere("unhandled value " + location);
            }
        }
    }

    private int valueBlocked(Value location)
    {
        if (isStackSlotValue(location))
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
        if (isRegister(location))
        {
            return registerBlocked[asRegister(location).number];
        }
        throw GraalError.shouldNotReachHere("unhandled value " + location);
    }

    private static boolean areMultipleReadsAllowed()
    {
        return true;
    }

    private boolean hasMappings()
    {
        return mappingFrom.size() > 0;
    }

    private MoveFactory getSpillMoveFactory()
    {
        return spillMoveFactory;
    }

    private RegisterArray getRegisters()
    {
        return frameMapBuilder.getRegisterConfig().getAllocatableRegisters();
    }

    public TraceGlobalMoveResolver(LIRGenerationResult res, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, Architecture arch)
    {
        this.mappingFrom = new ArrayList<>(8);
        this.mappingFromStack = new ArrayList<>(8);
        this.mappingTo = new ArrayList<>(8);
        this.insertIdx = -1;
        this.insertionBuffer = new LIRInsertionBuffer();

        this.frameMapBuilder = res.getFrameMapBuilder();
        this.spillMoveFactory = spillMoveFactory;
        this.registerBlocked = new int[arch.getRegisters().size()];
        this.registerAllocationConfig = registerAllocationConfig;

        FrameMapBuilderTool frameMapBuilderTool = (FrameMapBuilderTool) frameMapBuilder;
        this.stackBlocked = new int[frameMapBuilderTool.getNumberOfStackSlots()];

        FrameMap frameMap = frameMapBuilderTool.getFrameMap();
        this.firstVirtualStackIndex = !frameMap.frameNeedsAllocating() ? 0 : frameMap.currentFrameSize() + 1;
        this.res = res;
        LIR lir = res.getLIR();
        this.options = lir.getOptions();
    }

    // mark assignedReg and assignedRegHi of the interval as blocked
    private void block(Value location)
    {
        if (mightBeBlocked(location))
        {
            setValueBlocked(location, 1);
        }
    }

    // mark assignedReg and assignedRegHi of the interval as unblocked
    private void unblock(Value location)
    {
        if (mightBeBlocked(location))
        {
            setValueBlocked(location, -1);
        }
    }

    /**
     * Checks if {@code to} is not blocked or is only blocked by {@code from}.
     */
    private boolean safeToProcessMove(Value fromLocation, Value toLocation)
    {
        if (mightBeBlocked(toLocation))
        {
            if ((valueBlocked(toLocation) > 1 || (valueBlocked(toLocation) == 1 && !isMoveToSelf(fromLocation, toLocation))))
            {
                return false;
            }
        }

        return true;
    }

    public static boolean isMoveToSelf(Value from, Value to)
    {
        if (to.equals(from))
        {
            return true;
        }
        if (from == null)
        {
            return false;
        }
        if (isShadowedRegisterValue(from))
        {
            /* From is a shadowed register. */
            if (isShadowedRegisterValue(to))
            {
                // both shadowed but not equal
                return false;
            }
            ShadowedRegisterValue shadowed = asShadowedRegisterValue(from);
            if (isRegisterToRegisterMoveToSelf(shadowed.getRegister(), to))
            {
                return true;
            }
            if (isStackSlotValue(to))
            {
                return to.equals(shadowed.getStackSlot());
            }
        }
        else
        {
            /*
             * A shadowed destination value is never a self move it both values are not equal. Fall
             * through.
             */
            // if (isShadowedRegisterValue(to)) return false;

            return isRegisterToRegisterMoveToSelf(from, to);
        }
        return false;
    }

    private static boolean isRegisterToRegisterMoveToSelf(Value from, Value to)
    {
        if (to.equals(from))
        {
            return true;
        }
        if (isRegister(from) && isRegister(to) && asRegister(from).equals(asRegister(to)))
        {
            // Values differ but Registers are the same
            return true;
        }
        return false;
    }

    private static boolean mightBeBlocked(Value location)
    {
        return isRegister(location) || isStackSlotValue(location);
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

    private LIRInstruction insertMove(Value fromOperand, AllocatableValue toOperand)
    {
        LIRInstruction move = createMove(fromOperand, toOperand);
        insertionBuffer.append(insertIdx, move);
        return move;
    }

    /**
     * @param fromOpr Operand of the {@code from} interval
     * @param toOpr Operand of the {@code to} interval
     */
    private LIRInstruction createMove(Value fromOpr, AllocatableValue toOpr)
    {
        if (isStackSlotValue(toOpr) && isStackSlotValue(fromOpr))
        {
            return getSpillMoveFactory().createStackMove(toOpr, asAllocatableValue(fromOpr));
        }
        return getSpillMoveFactory().createMove(toOpr, fromOpr);
    }

    private void resolveMappings()
    {
        // Block all registers that are used as input operands of a move.
        // When a register is blocked, no move to this register is emitted.
        // This is necessary for detecting cycles in moves.
        for (int i = mappingFrom.size() - 1; i >= 0; i--)
        {
            Value from = mappingFrom.get(i);
            block(from);
        }

        ArrayList<AllocatableValue> busySpillSlots = null;
        while (mappingFrom.size() > 0)
        {
            boolean processedInterval = false;

            int spillCandidate = -1;
            for (int i = mappingFrom.size() - 1; i >= 0; i--)
            {
                Value fromLocation = mappingFrom.get(i);
                AllocatableValue toLocation = mappingTo.get(i);
                if (safeToProcessMove(fromLocation, toLocation))
                {
                    // this interval can be processed because target is free
                    LIRInstruction move = insertMove(fromLocation, toLocation);
                    move.setComment(res, "TraceGlobalMoveResolver: resolveMapping");
                    unblock(fromLocation);
                    if (isStackSlotValue(toLocation))
                    {
                        if (busySpillSlots == null)
                        {
                            busySpillSlots = new ArrayList<>(2);
                        }
                        busySpillSlots.add(toLocation);
                    }
                    mappingFrom.remove(i);
                    mappingFromStack.remove(i);
                    mappingTo.remove(i);

                    processedInterval = true;
                }
                else if (fromLocation != null)
                {
                    if (isRegister(fromLocation) && (busySpillSlots == null || !busySpillSlots.contains(mappingFromStack.get(i))))
                    {
                        // this interval cannot be processed now because target is not free
                        // it starts in a register, so it is a possible candidate for spilling
                        spillCandidate = i;
                    }
                    else if (isStackSlotValue(fromLocation) && spillCandidate == -1)
                    {
                        // fall back to spill a stack slot in case no other candidate is found
                        spillCandidate = i;
                    }
                }
            }

            if (!processedInterval)
            {
                breakCycle(spillCandidate);
            }
        }
    }

    private void breakCycle(int spillCandidate)
    {
        // no move could be processed because there is a cycle in the move list
        // (e.g. r1 . r2, r2 . r1), so one interval must be spilled to memory

        // create a new spill interval and assign a stack slot to it
        Value from = mappingFrom.get(spillCandidate);
        AllocatableValue spillSlot = null;
        if (TraceRegisterAllocationPhase.Options.TraceRAreuseStackSlotsForMoveResolutionCycleBreaking.getValue(options) && !isStackSlotValue(from))
        {
            // don't use the stack slot if from is already the stack slot
            Value fromStack = mappingFromStack.get(spillCandidate);
            if (fromStack != null)
            {
                spillSlot = (AllocatableValue) fromStack;
            }
        }
        if (spillSlot == null)
        {
            spillSlot = frameMapBuilder.allocateSpillSlot(from.getValueKind());
            // insert a move from register to stack and update the mapping
            LIRInstruction move = insertMove(from, spillSlot);
            move.setComment(res, "TraceGlobalMoveResolver: breakCycle");
        }
        block(spillSlot);
        mappingFrom.set(spillCandidate, spillSlot);
        unblock(from);
    }

    public void setInsertPosition(ArrayList<LIRInstruction> insertList, int insertIdx)
    {
        createInsertionBuffer(insertList);
        this.insertIdx = insertIdx;
    }

    @Override
    public void addMapping(Value from, AllocatableValue to, Value fromStack)
    {
        mappingFrom.add(from);
        mappingFromStack.add(fromStack);
        mappingTo.add(to);
    }

    public void resolveAndAppendMoves()
    {
        if (hasMappings())
        {
            resolveMappings();
        }
        appendInsertionBuffer();
    }

    private int getStackArrayIndex(Value stackSlotValue)
    {
        if (isStackSlot(stackSlotValue))
        {
            return getStackArrayIndex(asStackSlot(stackSlotValue));
        }
        if (isVirtualStackSlot(stackSlotValue))
        {
            return getStackArrayIndex(asVirtualStackSlot(stackSlotValue));
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
}
