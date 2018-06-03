package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

@Opcode
// @class AMD64LFenceOp
public final class AMD64LFenceOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64LFenceOp> TYPE = LIRInstructionClass.create(AMD64LFenceOp.class);

    // @cons
    public AMD64LFenceOp()
    {
        super(TYPE);
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __asm)
    {
        __asm.lfence();
    }
}
