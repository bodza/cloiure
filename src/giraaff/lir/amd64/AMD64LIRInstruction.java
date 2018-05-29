package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Convenience class to provide AMD64MacroAssembler for the {@link #emitCode} method.
 */
// @class AMD64LIRInstruction
public abstract class AMD64LIRInstruction extends LIRInstruction
{
    public static final LIRInstructionClass<AMD64LIRInstruction> TYPE = LIRInstructionClass.create(AMD64LIRInstruction.class);

    // @cons
    protected AMD64LIRInstruction(LIRInstructionClass<? extends AMD64LIRInstruction> c)
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
