package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Emits a pause.
 */
@Opcode
// @class AMD64PauseOp
public final class AMD64PauseOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64PauseOp> TYPE = LIRInstructionClass.create(AMD64PauseOp.class);

    // @cons
    public AMD64PauseOp()
    {
        super(TYPE);
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __asm)
    {
        __asm.pause();
    }
}
