package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Emits a pause.
 */
@Opcode("PAUSE")
public final class AMD64PauseOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64PauseOp> TYPE = LIRInstructionClass.create(AMD64PauseOp.class);

    public AMD64PauseOp()
    {
        super(TYPE);
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm)
    {
        asm.pause();
    }
}
