package giraaff.hotspot;

import java.util.Arrays;

import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.LIRKind;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.framemap.FrameMapBuilder;

///
// Manages allocation and re-use of lock slots in a scoped manner. The slots are used in HotSpot's
// lightweight locking mechanism to store the mark word of an object being locked.
///
// @class HotSpotLockStack
public final class HotSpotLockStack extends LIRInstruction
{
    // @def
    public static final LIRInstructionClass<HotSpotLockStack> TYPE = LIRInstructionClass.create(HotSpotLockStack.class);

    // @def
    private static final AllocatableValue[] EMPTY = new AllocatableValue[0];

    @LIRInstruction.Def({LIRInstruction.OperandFlag.STACK})
    // @field
    private AllocatableValue[] ___locks;
    // @field
    private final FrameMapBuilder ___frameMapBuilder;
    // @field
    private final LIRKind ___slotKind;

    // @cons HotSpotLockStack
    public HotSpotLockStack(FrameMapBuilder __frameMapBuilder, LIRKind __slotKind)
    {
        super(TYPE);
        this.___frameMapBuilder = __frameMapBuilder;
        this.___slotKind = __slotKind;
        this.___locks = EMPTY;
    }

    ///
    // Gets a stack slot for a lock at a given lock nesting depth, allocating it first if necessary.
    ///
    public VirtualStackSlot makeLockSlot(int __lockDepth)
    {
        if (this.___locks == EMPTY)
        {
            this.___locks = new AllocatableValue[__lockDepth + 1];
        }
        else if (this.___locks.length < __lockDepth + 1)
        {
            this.___locks = Arrays.copyOf(this.___locks, __lockDepth + 1);
        }
        if (this.___locks[__lockDepth] == null)
        {
            this.___locks[__lockDepth] = this.___frameMapBuilder.allocateSpillSlot(this.___slotKind);
        }
        return (VirtualStackSlot) this.___locks[__lockDepth];
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb)
    {
        // do nothing
    }
}
