package graalvm.compiler.lir.amd64;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

/**
 * Convenience class to provide AMD64MacroAssembler for the {@link #emitCode} method.
 */
public abstract class AMD64LIRInstruction extends LIRInstruction {
    public static final LIRInstructionClass<AMD64LIRInstruction> TYPE = LIRInstructionClass.create(AMD64LIRInstruction.class);

    protected AMD64LIRInstruction(LIRInstructionClass<? extends AMD64LIRInstruction> c) {
        super(c);
    }

    @Override
    public final void emitCode(CompilationResultBuilder crb) {
        emitCode(crb, (AMD64MacroAssembler) crb.asm);
    }

    public abstract void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm);
}
