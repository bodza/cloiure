package graalvm.compiler.hotspot.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static jdk.vm.ci.amd64.AMD64.rbp;
import static jdk.vm.ci.code.ValueUtil.asRegister;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Pops a deoptimized stack frame off the stack including the return address.
 */
@Opcode("LEAVE_DEOPTIMIZED_STACK_FRAME")
final class AMD64HotSpotLeaveDeoptimizedStackFrameOp extends AMD64HotSpotEpilogueOp
{
    public static final LIRInstructionClass<AMD64HotSpotLeaveDeoptimizedStackFrameOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLeaveDeoptimizedStackFrameOp.class);
    @Use(REG) AllocatableValue frameSize;
    @Use(REG) AllocatableValue framePointer;

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
        masm.addq(stackPointer, asRegister(frameSize));

        /*
         * Restore the frame pointer before stack bang because if a stack overflow is thrown it
         * needs to be pushed (and preserved).
         */
        masm.movq(rbp, asRegister(framePointer));
    }
}
