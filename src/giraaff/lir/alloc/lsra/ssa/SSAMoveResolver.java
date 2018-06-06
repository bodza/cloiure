package giraaff.lir.alloc.lsra.ssa;

import java.util.Arrays;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.alloc.lsra.Interval;
import giraaff.lir.alloc.lsra.LinearScan;
import giraaff.lir.alloc.lsra.MoveResolver;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.framemap.FrameMapBuilderTool;
import giraaff.util.GraalError;

// @class SSAMoveResolver
public final class SSAMoveResolver extends MoveResolver
{
    // @def
    private static final int STACK_SLOT_IN_CALLER_FRAME_IDX = -1;
    // @field
    private int[] ___stackBlocked;
    // @field
    private final int ___firstVirtualStackIndex;

    // @cons SSAMoveResolver
    public SSAMoveResolver(LinearScan __allocator)
    {
        super(__allocator);
        FrameMapBuilderTool __frameMapBuilderTool = (FrameMapBuilderTool) __allocator.getFrameMapBuilder();
        FrameMap __frameMap = __frameMapBuilderTool.getFrameMap();
        this.___stackBlocked = new int[__frameMapBuilderTool.getNumberOfStackSlots()];
        this.___firstVirtualStackIndex = !__frameMap.frameNeedsAllocating() ? 0 : __frameMap.currentFrameSize() + 1;
    }

    @Override
    protected boolean areMultipleReadsAllowed()
    {
        return true;
    }

    @Override
    protected boolean mightBeBlocked(Value __location)
    {
        if (super.mightBeBlocked(__location))
        {
            return true;
        }
        if (LIRValueUtil.isStackSlotValue(__location))
        {
            return true;
        }
        return false;
    }

    private int getStackArrayIndex(Value __stackSlotValue)
    {
        if (ValueUtil.isStackSlot(__stackSlotValue))
        {
            return getStackArrayIndex(ValueUtil.asStackSlot(__stackSlotValue));
        }
        if (LIRValueUtil.isVirtualStackSlot(__stackSlotValue))
        {
            return getStackArrayIndex(LIRValueUtil.asVirtualStackSlot(__stackSlotValue));
        }
        throw GraalError.shouldNotReachHere("value is not a stack slot: " + __stackSlotValue);
    }

    private int getStackArrayIndex(StackSlot __stackSlot)
    {
        int __stackIdx;
        if (__stackSlot.isInCallerFrame())
        {
            // incoming stack arguments can be ignored
            __stackIdx = STACK_SLOT_IN_CALLER_FRAME_IDX;
        }
        else
        {
            int __offset = -__stackSlot.getRawOffset();
            __stackIdx = __offset;
        }
        return __stackIdx;
    }

    private int getStackArrayIndex(VirtualStackSlot __virtualStackSlot)
    {
        return this.___firstVirtualStackIndex + __virtualStackSlot.getId();
    }

    @Override
    protected void setValueBlocked(Value __location, int __direction)
    {
        if (LIRValueUtil.isStackSlotValue(__location))
        {
            int __stackIdx = getStackArrayIndex(__location);
            if (__stackIdx == STACK_SLOT_IN_CALLER_FRAME_IDX)
            {
                // incoming stack arguments can be ignored
                return;
            }
            if (__stackIdx >= this.___stackBlocked.length)
            {
                this.___stackBlocked = Arrays.copyOf(this.___stackBlocked, __stackIdx + 1);
            }
            this.___stackBlocked[__stackIdx] += __direction;
        }
        else
        {
            super.setValueBlocked(__location, __direction);
        }
    }

    @Override
    protected int valueBlocked(Value __location)
    {
        if (LIRValueUtil.isStackSlotValue(__location))
        {
            int __stackIdx = getStackArrayIndex(__location);
            if (__stackIdx == STACK_SLOT_IN_CALLER_FRAME_IDX)
            {
                // incoming stack arguments are always blocked (aka they can not be written)
                return 1;
            }
            if (__stackIdx >= this.___stackBlocked.length)
            {
                return 0;
            }
            return this.___stackBlocked[__stackIdx];
        }
        return super.valueBlocked(__location);
    }

    @Override
    protected LIRInstruction createMove(AllocatableValue __fromOpr, AllocatableValue __toOpr, AllocatableValue __fromLocation, AllocatableValue __toLocation)
    {
        if (LIRValueUtil.isStackSlotValue(__toLocation) && LIRValueUtil.isStackSlotValue(__fromLocation))
        {
            return getAllocator().getSpillMoveFactory().createStackMove(__toOpr, __fromOpr);
        }
        return super.createMove(__fromOpr, __toOpr, __fromLocation, __toLocation);
    }

    @Override
    protected void breakCycle(int __spillCandidate)
    {
        if (__spillCandidate != -1)
        {
            super.breakCycle(__spillCandidate);
            return;
        }
        // Arbitrarily select the first entry for spilling.
        int __stackSpillCandidate = 0;
        Interval __fromInterval = getMappingFrom(__stackSpillCandidate);
        // allocate new stack slot
        VirtualStackSlot __spillSlot = getAllocator().getFrameMapBuilder().allocateSpillSlot(__fromInterval.kind());
        spillInterval(__stackSpillCandidate, __fromInterval, __spillSlot);
    }
}
