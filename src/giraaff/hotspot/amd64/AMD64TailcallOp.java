package giraaff.hotspot.amd64;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Performs a hard-coded tail call to the specified target, which normally should be an
 * {@link InstalledCode} instance.
 */
@Opcode
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
        masm.jmp(ValueUtil.asRegister(target));
        masm.ensureUniquePC();
    }
}
