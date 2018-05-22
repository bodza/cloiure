package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.StandardOp.BlockEndOp;
import giraaff.lir.asm.CompilationResultBuilder;

public abstract class AMD64BlockEndOp extends LIRInstruction implements BlockEndOp
{
    public static final LIRInstructionClass<AMD64BlockEndOp> TYPE = LIRInstructionClass.create(AMD64BlockEndOp.class);

    protected AMD64BlockEndOp(LIRInstructionClass<? extends AMD64BlockEndOp> c)
    {
        super(c);
    }

    @Override
    public final void emitCode(CompilationResultBuilder crb)
    {
        emitCode(crb, (AMD64MacroAssembler) crb.asm);
    }

    public abstract void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm);
}
