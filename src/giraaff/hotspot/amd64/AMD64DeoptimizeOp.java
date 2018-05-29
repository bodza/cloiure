package giraaff.hotspot.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.HotSpotHostBackend;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.BlockEndOp;
import giraaff.lir.amd64.AMD64BlockEndOp;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.asm.CompilationResultBuilder;

@Opcode
// @class AMD64DeoptimizeOp
final class AMD64DeoptimizeOp extends AMD64BlockEndOp implements BlockEndOp
{
    public static final LIRInstructionClass<AMD64DeoptimizeOp> TYPE = LIRInstructionClass.create(AMD64DeoptimizeOp.class);

    // @State
    private LIRFrameState state;

    // @cons
    AMD64DeoptimizeOp(LIRFrameState state)
    {
        super(TYPE);
        this.state = state;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        AMD64Call.directCall(crb, masm, crb.foreignCalls.lookupForeignCall(HotSpotHostBackend.UNCOMMON_TRAP_HANDLER), null, false, state);
    }
}
