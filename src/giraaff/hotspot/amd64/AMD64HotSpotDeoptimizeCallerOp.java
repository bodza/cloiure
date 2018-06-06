package giraaff.hotspot.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.HotSpotHostBackend;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Removes the current frame and tail calls the uncommon trap routine.
///
@LIROpcode
// @class AMD64HotSpotDeoptimizeCallerOp
final class AMD64HotSpotDeoptimizeCallerOp extends AMD64HotSpotEpilogueBlockEndOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotDeoptimizeCallerOp> TYPE = LIRInstructionClass.create(AMD64HotSpotDeoptimizeCallerOp.class);

    // @cons AMD64HotSpotDeoptimizeCallerOp
    protected AMD64HotSpotDeoptimizeCallerOp()
    {
        super(TYPE);
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        leaveFrameAndRestoreRbp(__crb, __masm);
        AMD64Call.directJmp(__crb, __masm, __crb.___foreignCalls.lookupForeignCall(HotSpotHostBackend.UNCOMMON_TRAP_HANDLER));
    }
}
