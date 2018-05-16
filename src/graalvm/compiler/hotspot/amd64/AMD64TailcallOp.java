package graalvm.compiler.hotspot.amd64;

import static jdk.vm.ci.code.ValueUtil.asRegister;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.meta.Value;

/**
 * Performs a hard-coded tail call to the specified target, which normally should be an
 * {@link InstalledCode} instance.
 */
@Opcode("TAILCALL")
public final class AMD64TailcallOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64TailcallOp> TYPE = LIRInstructionClass.create(AMD64TailcallOp.class);

    @Use protected Value target;
    @Alive protected Value[] parameters;

    public AMD64TailcallOp(Value[] parameters, Value target)
    {
        super(TYPE);
        this.target = target;
        this.parameters = parameters;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        // destroy the current frame (now the return address is the top of stack)
        masm.leave();

        // jump to the target method
        masm.jmp(asRegister(target));
        masm.ensureUniquePC();
    }
}
