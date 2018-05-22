package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

@Opcode("LFENCE")
public final class AMD64LFenceOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64LFenceOp> TYPE = LIRInstructionClass.create(AMD64LFenceOp.class);

    public AMD64LFenceOp()
    {
        super(TYPE);
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm)
    {
        asm.lfence();
    }
}
