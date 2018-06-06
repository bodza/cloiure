package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.ValueUtil;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.stubs.UnwindExceptionToCallerStub;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Removes the current frame and jumps to the {@link UnwindExceptionToCallerStub}.
///
@LIROpcode
// @class AMD64HotSpotUnwindOp
final class AMD64HotSpotUnwindOp extends AMD64HotSpotEpilogueBlockEndOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotUnwindOp> TYPE = LIRInstructionClass.create(AMD64HotSpotUnwindOp.class);

    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
    // @field
    protected RegisterValue ___exception;

    // @cons AMD64HotSpotUnwindOp
    AMD64HotSpotUnwindOp(RegisterValue __exception)
    {
        super(TYPE);
        this.___exception = __exception;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        leaveFrameAndRestoreRbp(__crb, __masm);

        ForeignCallLinkage __linkage = __crb.___foreignCalls.lookupForeignCall(HotSpotBackend.UNWIND_EXCEPTION_TO_CALLER);
        CallingConvention __cc = __linkage.getOutgoingCallingConvention();

        // Get return address (is on top of stack after leave).
        Register __returnAddress = ValueUtil.asRegister(__cc.getArgument(1));
        __masm.movq(__returnAddress, new AMD64Address(AMD64.rsp, 0));

        AMD64Call.directJmp(__crb, __masm, __linkage);
    }
}
