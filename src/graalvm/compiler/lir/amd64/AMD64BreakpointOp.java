package graalvm.compiler.lir.amd64;

import jdk.vm.ci.meta.Value;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

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
    @Use({OperandFlag.REG, OperandFlag.STACK}) protected Value[] parameters;

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
