package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * AMD64 rdtsc operation. The result is in EDX:EAX.
 */
@Opcode
public class AMD64ReadTimestampCounter extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64ReadTimestampCounter> TYPE = LIRInstructionClass.create(AMD64ReadTimestampCounter.class);

    @Def({OperandFlag.REG}) protected AllocatableValue highResult;
    @Def({OperandFlag.REG}) protected AllocatableValue lowResult;

    public AMD64ReadTimestampCounter()
    {
        super(TYPE);

        this.highResult = AMD64.rdx.asValue(LIRKind.value(AMD64Kind.DWORD));
        this.lowResult = AMD64.rax.asValue(LIRKind.value(AMD64Kind.DWORD));
    }

    public AllocatableValue getHighResult()
    {
        return highResult;
    }

    public AllocatableValue getLowResult()
    {
        return lowResult;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        masm.rdtsc();
    }
}
