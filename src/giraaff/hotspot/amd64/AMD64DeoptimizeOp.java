package giraaff.hotspot.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.HotSpotHostBackend;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.StandardOp;
import giraaff.lir.amd64.AMD64BlockEndOp;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.asm.CompilationResultBuilder;

@LIROpcode
// @class AMD64DeoptimizeOp
final class AMD64DeoptimizeOp extends AMD64BlockEndOp implements StandardOp.BlockEndOp
{
    // @def
    public static final LIRInstructionClass<AMD64DeoptimizeOp> TYPE = LIRInstructionClass.create(AMD64DeoptimizeOp.class);

    // @State
    // @field
    private LIRFrameState ___state;

    // @cons AMD64DeoptimizeOp
    AMD64DeoptimizeOp(LIRFrameState __state)
    {
        super(TYPE);
        this.___state = __state;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        AMD64Call.directCall(__crb, __masm, __crb.___foreignCalls.lookupForeignCall(HotSpotHostBackend.UNCOMMON_TRAP_HANDLER), null, false, this.___state);
    }
}
