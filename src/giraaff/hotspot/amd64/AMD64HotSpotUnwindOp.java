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
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Removes the current frame and jumps to the {@link UnwindExceptionToCallerStub}.
 */
@Opcode("UNWIND")
final class AMD64HotSpotUnwindOp extends AMD64HotSpotEpilogueBlockEndOp
{
    public static final LIRInstructionClass<AMD64HotSpotUnwindOp> TYPE = LIRInstructionClass.create(AMD64HotSpotUnwindOp.class);

    @Use({OperandFlag.REG}) protected RegisterValue exception;

    AMD64HotSpotUnwindOp(RegisterValue exception)
    {
        super(TYPE);
        this.exception = exception;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        leaveFrameAndRestoreRbp(crb, masm);

        ForeignCallLinkage linkage = crb.foreignCalls.lookupForeignCall(HotSpotBackend.UNWIND_EXCEPTION_TO_CALLER);
        CallingConvention cc = linkage.getOutgoingCallingConvention();

        // Get return address (is on top of stack after leave).
        Register returnAddress = ValueUtil.asRegister(cc.getArgument(1));
        masm.movq(returnAddress, new AMD64Address(AMD64.rsp, 0));

        AMD64Call.directJmp(crb, masm, linkage);
    }
}
