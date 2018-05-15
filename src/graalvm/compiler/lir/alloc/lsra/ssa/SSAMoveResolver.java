package graalvm.compiler.lir.alloc.lsra.ssa;

import static graalvm.compiler.lir.LIRValueUtil.asVirtualStackSlot;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;
import static jdk.vm.ci.code.ValueUtil.asStackSlot;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;

import java.util.Arrays;

import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.alloc.lsra.Interval;
import graalvm.compiler.lir.alloc.lsra.LinearScan;
import graalvm.compiler.lir.alloc.lsra.MoveResolver;
import graalvm.compiler.lir.framemap.FrameMap;
import graalvm.compiler.lir.framemap.FrameMapBuilderTool;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

public final class SSAMoveResolver extends MoveResolver {

    private static final int STACK_SLOT_IN_CALLER_FRAME_IDX = -1;
    private int[] stackBlocked;
    private final int firstVirtualStackIndex;

    public SSAMoveResolver(LinearScan allocator) {
        super(allocator);
        FrameMapBuilderTool frameMapBuilderTool = (FrameMapBuilderTool) allocator.getFrameMapBuilder();
        FrameMap frameMap = frameMapBuilderTool.getFrameMap();
        this.stackBlocked = new int[frameMapBuilderTool.getNumberOfStackSlots()];
        this.firstVirtualStackIndex = !frameMap.frameNeedsAllocating() ? 0 : frameMap.currentFrameSize() + 1;
    }

    @Override
    public boolean checkEmpty() {
        for (int i = 0; i < stackBlocked.length; i++) {
            assert stackBlocked[i] == 0 : "stack map must be empty before and after processing";
        }
        return super.checkEmpty();
    }

    @Override
    protected void checkMultipleReads() {
        // multiple reads are allowed in SSA LSRA
    }

    @Override
    protected void verifyStackSlotMapping() {
        // relax disjoint stack maps invariant
    }

    @Override
    protected boolean areMultipleReadsAllowed() {
        return true;
    }

    @Override
    protected boolean mightBeBlocked(Value location) {
        if (super.mightBeBlocked(location)) {
            return true;
        }
        if (isStackSlotValue(location)) {
            return true;
        }
        return false;
    }

    private int getStackArrayIndex(Value stackSlotValue) {
        if (isStackSlot(stackSlotValue)) {
            return getStackArrayIndex(asStackSlot(stackSlotValue));
        }
        if (isVirtualStackSlot(stackSlotValue)) {
            return getStackArrayIndex(asVirtualStackSlot(stackSlotValue));
        }
        throw GraalError.shouldNotReachHere("value is not a stack slot: " + stackSlotValue);
    }

    private int getStackArrayIndex(StackSlot stackSlot) {
        int stackIdx;
        if (stackSlot.isInCallerFrame()) {
            // incoming stack arguments can be ignored
            stackIdx = STACK_SLOT_IN_CALLER_FRAME_IDX;
        } else {
            assert stackSlot.getRawAddFrameSize() : "Unexpected stack slot: " + stackSlot;
            int offset = -stackSlot.getRawOffset();
            assert 0 <= offset && offset < firstVirtualStackIndex : String.format("Wrong stack slot offset: %d (first virtual stack slot index: %d", offset, firstVirtualStackIndex);
            stackIdx = offset;
        }
        return stackIdx;
    }

    private int getStackArrayIndex(VirtualStackSlot virtualStackSlot) {
        return firstVirtualStackIndex + virtualStackSlot.getId();
    }

    @Override
    protected void setValueBlocked(Value location, int direction) {
        assert direction == 1 || direction == -1 : "out of bounds";
        if (isStackSlotValue(location)) {
            int stackIdx = getStackArrayIndex(location);
            if (stackIdx == STACK_SLOT_IN_CALLER_FRAME_IDX) {
                // incoming stack arguments can be ignored
                return;
            }
            if (stackIdx >= stackBlocked.length) {
                stackBlocked = Arrays.copyOf(stackBlocked, stackIdx + 1);
            }
            stackBlocked[stackIdx] += direction;
        } else {
            super.setValueBlocked(location, direction);
        }
    }

    @Override
    protected int valueBlocked(Value location) {
        if (isStackSlotValue(location)) {
            int stackIdx = getStackArrayIndex(location);
            if (stackIdx == STACK_SLOT_IN_CALLER_FRAME_IDX) {
                // incoming stack arguments are always blocked (aka they can not be written)
                return 1;
            }
            if (stackIdx >= stackBlocked.length) {
                return 0;
            }
            return stackBlocked[stackIdx];
        }
        return super.valueBlocked(location);
    }

    @Override
    protected LIRInstruction createMove(AllocatableValue fromOpr, AllocatableValue toOpr, AllocatableValue fromLocation, AllocatableValue toLocation) {
        if (isStackSlotValue(toLocation) && isStackSlotValue(fromLocation)) {
            return getAllocator().getSpillMoveFactory().createStackMove(toOpr, fromOpr);
        }
        return super.createMove(fromOpr, toOpr, fromLocation, toLocation);
    }

    @Override
    protected void breakCycle(int spillCandidate) {
        if (spillCandidate != -1) {
            super.breakCycle(spillCandidate);
            return;
        }
        assert mappingFromSize() > 1;
        // Arbitrarily select the first entry for spilling.
        int stackSpillCandidate = 0;
        Interval fromInterval = getMappingFrom(stackSpillCandidate);
        // allocate new stack slot
        VirtualStackSlot spillSlot = getAllocator().getFrameMapBuilder().allocateSpillSlot(fromInterval.kind());
        spillInterval(stackSpillCandidate, fromInterval, spillSlot);
    }
}
