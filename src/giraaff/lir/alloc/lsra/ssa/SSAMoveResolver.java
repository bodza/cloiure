package giraaff.lir.alloc.lsra.ssa;

import java.util.Arrays;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.debug.GraalError;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.alloc.lsra.Interval;
import giraaff.lir.alloc.lsra.LinearScan;
import giraaff.lir.alloc.lsra.MoveResolver;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.FrameMapBuilderTool;

public final class SSAMoveResolver extends MoveResolver
{
    private static final int STACK_SLOT_IN_CALLER_FRAME_IDX = -1;
    private int[] stackBlocked;
    private final int firstVirtualStackIndex;

    public SSAMoveResolver(LinearScan allocator)
    {
        super(allocator);
        FrameMapBuilderTool frameMapBuilderTool = (FrameMapBuilderTool) allocator.getFrameMapBuilder();
        FrameMap frameMap = frameMapBuilderTool.getFrameMap();
        this.stackBlocked = new int[frameMapBuilderTool.getNumberOfStackSlots()];
        this.firstVirtualStackIndex = !frameMap.frameNeedsAllocating() ? 0 : frameMap.currentFrameSize() + 1;
    }

    @Override
    protected boolean areMultipleReadsAllowed()
    {
        return true;
    }

    @Override
    protected boolean mightBeBlocked(Value location)
    {
        if (super.mightBeBlocked(location))
        {
            return true;
        }
        if (LIRValueUtil.isStackSlotValue(location))
        {
            return true;
        }
        return false;
    }

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

    @Override
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
            super.setValueBlocked(location, direction);
        }
    }

    @Override
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
        return super.valueBlocked(location);
    }

    @Override
    protected LIRInstruction createMove(AllocatableValue fromOpr, AllocatableValue toOpr, AllocatableValue fromLocation, AllocatableValue toLocation)
    {
        if (LIRValueUtil.isStackSlotValue(toLocation) && LIRValueUtil.isStackSlotValue(fromLocation))
        {
            return getAllocator().getSpillMoveFactory().createStackMove(toOpr, fromOpr);
        }
        return super.createMove(fromOpr, toOpr, fromLocation, toLocation);
    }

    @Override
    protected void breakCycle(int spillCandidate)
    {
        if (spillCandidate != -1)
        {
            super.breakCycle(spillCandidate);
            return;
        }
        // Arbitrarily select the first entry for spilling.
        int stackSpillCandidate = 0;
        Interval fromInterval = getMappingFrom(stackSpillCandidate);
        // allocate new stack slot
        VirtualStackSlot spillSlot = getAllocator().getFrameMapBuilder().allocateSpillSlot(fromInterval.kind());
        spillInterval(stackSpillCandidate, fromInterval, spillSlot);
    }
}
