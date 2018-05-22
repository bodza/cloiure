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
@Opcode("LEAVE_DEOPTIMIZED_STACK_FRAME")
final class AMD64HotSpotLeaveDeoptimizedStackFrameOp extends AMD64HotSpotEpilogueOp
{
    public static final LIRInstructionClass<AMD64HotSpotLeaveDeoptimizedStackFrameOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLeaveDeoptimizedStackFrameOp.class);
    @Use(OperandFlag.REG) AllocatableValue frameSize;
    @Use(OperandFlag.REG) AllocatableValue framePointer;

    AMD64HotSpotLeaveDeoptimizedStackFrameOp(AllocatableValue frameSize, AllocatableValue initialInfo)
    {
        super(TYPE);
        this.frameSize = frameSize;
        this.framePointer = initialInfo;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        Register stackPointer = crb.frameMap.getRegisterConfig().getFrameRegister();
        masm.addq(stackPointer, ValueUtil.asRegister(frameSize));

        /*
         * Restore the frame pointer before stack bang because if a stack overflow is thrown it
         * needs to be pushed (and preserved).
         */
        masm.movq(AMD64.rbp, ValueUtil.asRegister(framePointer));
    }
}
