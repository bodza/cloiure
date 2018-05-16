package graalvm.compiler.lir.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.STACK;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.meta.Value;

/**
 * Emits a breakpoint.
 */
@Opcode("BREAKPOINT")
public final class AMD64BreakpointOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64BreakpointOp> TYPE = LIRInstructionClass.create(AMD64BreakpointOp.class);

    /**
     * A set of values loaded into the Java ABI parameter locations (for inspection by a debugger).
     */
    @Use({REG, STACK}) protected Value[] parameters;

    public AMD64BreakpointOp(Value[] parameters)
    {
        super(TYPE);
        this.parameters = parameters;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm)
    {
        asm.int3();
    }
}
