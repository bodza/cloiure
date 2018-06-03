package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Pops a deoptimized stack frame off the stack including the return address.
 */
@Opcode
// @class AMD64HotSpotLeaveDeoptimizedStackFrameOp
final class AMD64HotSpotLeaveDeoptimizedStackFrameOp extends AMD64HotSpotEpilogueOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotLeaveDeoptimizedStackFrameOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLeaveDeoptimizedStackFrameOp.class);

    @Use(OperandFlag.REG)
    // @field
    AllocatableValue frameSize;
    @Use(OperandFlag.REG)
    // @field
    AllocatableValue framePointer;

    // @cons
    AMD64HotSpotLeaveDeoptimizedStackFrameOp(AllocatableValue __frameSize, AllocatableValue __initialInfo)
    {
        super(TYPE);
        this.frameSize = __frameSize;
        this.framePointer = __initialInfo;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        Register __stackPointer = __crb.frameMap.getRegisterConfig().getFrameRegister();
        __masm.addq(__stackPointer, ValueUtil.asRegister(frameSize));

        /*
         * Restore the frame pointer before stack bang because if a stack overflow is thrown it
         * needs to be pushed (and preserved).
         */
        __masm.movq(AMD64.rbp, ValueUtil.asRegister(framePointer));
    }
}
