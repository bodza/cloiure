package giraaff.lir.amd64;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.StandardOp.BlockEndOp;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64BlockEndOp
public abstract class AMD64BlockEndOp extends LIRInstruction implements BlockEndOp
{
    // @def
    public static final LIRInstructionClass<AMD64BlockEndOp> TYPE = LIRInstructionClass.create(AMD64BlockEndOp.class);

    // @cons
    protected AMD64BlockEndOp(LIRInstructionClass<? extends AMD64BlockEndOp> __c)
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
