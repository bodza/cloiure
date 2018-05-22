package graalvm.compiler.hotspot.amd64;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.hotspot.HotSpotHostBackend;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.StandardOp.BlockEndOp;
import graalvm.compiler.lir.amd64.AMD64BlockEndOp;
import graalvm.compiler.lir.amd64.AMD64Call;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

@Opcode("DEOPT")
final class AMD64DeoptimizeOp extends AMD64BlockEndOp implements BlockEndOp
{
    public static final LIRInstructionClass<AMD64DeoptimizeOp> TYPE = LIRInstructionClass.create(AMD64DeoptimizeOp.class);

    @State private LIRFrameState info;

    AMD64DeoptimizeOp(LIRFrameState info)
    {
        super(TYPE);
        this.info = info;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        AMD64Call.directCall(crb, masm, crb.foreignCalls.lookupForeignCall(HotSpotHostBackend.UNCOMMON_TRAP_HANDLER), null, false, info);
    }
}
