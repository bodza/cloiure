package graalvm.compiler.hotspot;

import java.util.Arrays;

import jdk.vm.ci.meta.AllocatableValue;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.framemap.FrameMapBuilder;

/**
 * Manages allocation and re-use of lock slots in a scoped manner. The slots are used in HotSpot's
 * lightweight locking mechanism to store the mark word of an object being locked.
 */
public class HotSpotLockStack extends LIRInstruction
{
    public static final LIRInstructionClass<HotSpotLockStack> TYPE = LIRInstructionClass.create(HotSpotLockStack.class);
    private static final AllocatableValue[] EMPTY = new AllocatableValue[0];

    @Def({OperandFlag.STACK}) private AllocatableValue[] locks;
    private final FrameMapBuilder frameMapBuilder;
    private final LIRKind slotKind;

    public HotSpotLockStack(FrameMapBuilder frameMapBuilder, LIRKind slotKind)
    {
        super(TYPE);
        this.frameMapBuilder = frameMapBuilder;
        this.slotKind = slotKind;
        this.locks = EMPTY;
    }

    /**
     * Gets a stack slot for a lock at a given lock nesting depth, allocating it first if necessary.
     */
    public VirtualStackSlot makeLockSlot(int lockDepth)
    {
        if (locks == EMPTY)
        {
            locks = new AllocatableValue[lockDepth + 1];
        }
        else if (locks.length < lockDepth + 1)
        {
            locks = Arrays.copyOf(locks, lockDepth + 1);
        }
        if (locks[lockDepth] == null)
        {
            locks[lockDepth] = frameMapBuilder.allocateSpillSlot(slotKind);
        }
        return (VirtualStackSlot) locks[lockDepth];
    }

    @Override
    public void emitCode(CompilationResultBuilder crb)
    {
        // do nothing
    }
}
