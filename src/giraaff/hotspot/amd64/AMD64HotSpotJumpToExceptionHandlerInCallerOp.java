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
 * Sets up the arguments for an exception handler in the callers frame, removes the current frame
 * and jumps to the handler.
 */
@Opcode
// @class AMD64HotSpotJumpToExceptionHandlerInCallerOp
final class AMD64HotSpotJumpToExceptionHandlerInCallerOp extends AMD64HotSpotEpilogueBlockEndOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotJumpToExceptionHandlerInCallerOp> TYPE = LIRInstructionClass.create(AMD64HotSpotJumpToExceptionHandlerInCallerOp.class);

    @Use(OperandFlag.REG)
    // @field
    AllocatableValue handlerInCallerPc;
    @Use(OperandFlag.REG)
    // @field
    AllocatableValue exception;
    @Use(OperandFlag.REG)
    // @field
    AllocatableValue exceptionPc;
    // @field
    private final Register thread;
    // @field
    private final int isMethodHandleReturnOffset;

    // @cons
    AMD64HotSpotJumpToExceptionHandlerInCallerOp(AllocatableValue __handlerInCallerPc, AllocatableValue __exception, AllocatableValue __exceptionPc, int __isMethodHandleReturnOffset, Register __thread)
    {
        super(TYPE);
        this.handlerInCallerPc = __handlerInCallerPc;
        this.exception = __exception;
        this.exceptionPc = __exceptionPc;
        this.isMethodHandleReturnOffset = __isMethodHandleReturnOffset;
        this.thread = __thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        leaveFrameAndRestoreRbp(__crb, __masm);

        // discard the return address, thus completing restoration of caller frame
        __masm.incrementq(AMD64.rsp, 8);

        __masm.jmp(ValueUtil.asRegister(handlerInCallerPc));
    }
}
