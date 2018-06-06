package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Sets up the arguments for an exception handler in the callers frame, removes the current frame
// and jumps to the handler.
///
@LIROpcode
// @class AMD64HotSpotJumpToExceptionHandlerInCallerOp
final class AMD64HotSpotJumpToExceptionHandlerInCallerOp extends AMD64HotSpotEpilogueBlockEndOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotJumpToExceptionHandlerInCallerOp> TYPE = LIRInstructionClass.create(AMD64HotSpotJumpToExceptionHandlerInCallerOp.class);

    @LIRInstruction.Use(LIRInstruction.OperandFlag.REG)
    // @field
    AllocatableValue ___handlerInCallerPc;
    @LIRInstruction.Use(LIRInstruction.OperandFlag.REG)
    // @field
    AllocatableValue ___exception;
    @LIRInstruction.Use(LIRInstruction.OperandFlag.REG)
    // @field
    AllocatableValue ___exceptionPc;
    // @field
    private final Register ___thread;
    // @field
    private final int ___isMethodHandleReturnOffset;

    // @cons AMD64HotSpotJumpToExceptionHandlerInCallerOp
    AMD64HotSpotJumpToExceptionHandlerInCallerOp(AllocatableValue __handlerInCallerPc, AllocatableValue __exception, AllocatableValue __exceptionPc, int __isMethodHandleReturnOffset, Register __thread)
    {
        super(TYPE);
        this.___handlerInCallerPc = __handlerInCallerPc;
        this.___exception = __exception;
        this.___exceptionPc = __exceptionPc;
        this.___isMethodHandleReturnOffset = __isMethodHandleReturnOffset;
        this.___thread = __thread;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        leaveFrameAndRestoreRbp(__crb, __masm);

        // discard the return address, thus completing restoration of caller frame
        __masm.incrementq(AMD64.rsp, 8);

        __masm.jmp(ValueUtil.asRegister(this.___handlerInCallerPc));
    }
}
