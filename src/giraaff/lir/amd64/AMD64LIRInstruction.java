package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Convenience class to provide AMD64MacroAssembler for the {@link #emitCode} method.
///
// @class AMD64LIRInstruction
public abstract class AMD64LIRInstruction extends LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64LIRInstruction> TYPE = LIRInstructionClass.create(AMD64LIRInstruction.class);

    // @cons
    protected AMD64LIRInstruction(LIRInstructionClass<? extends AMD64LIRInstruction> __c)
    {
        super(__c);
    }

    @Override
    public final void emitCode(CompilationResultBuilder __crb)
    {
        emitCode(__crb, (AMD64MacroAssembler) __crb.___asm);
    }

    public abstract void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm);
}
