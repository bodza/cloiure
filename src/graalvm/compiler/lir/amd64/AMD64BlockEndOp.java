package graalvm.compiler.lir.amd64;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.StandardOp.BlockEndOp;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

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
