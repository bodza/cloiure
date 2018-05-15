package graalvm.compiler.hotspot.amd64;

import static graalvm.compiler.hotspot.HotSpotHostBackend.UNCOMMON_TRAP_HANDLER;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64Call;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

/**
 * Removes the current frame and tail calls the uncommon trap routine.
 */
@Opcode("DEOPT_CALLER")
final class AMD64HotSpotDeoptimizeCallerOp extends AMD64HotSpotEpilogueBlockEndOp {

    public static final LIRInstructionClass<AMD64HotSpotDeoptimizeCallerOp> TYPE = LIRInstructionClass.create(AMD64HotSpotDeoptimizeCallerOp.class);

    protected AMD64HotSpotDeoptimizeCallerOp() {
        super(TYPE);
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        leaveFrameAndRestoreRbp(crb, masm);
        AMD64Call.directJmp(crb, masm, crb.foreignCalls.lookupForeignCall(UNCOMMON_TRAP_HANDLER));
    }
}
