package graalvm.compiler.lir.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.AllocatableValue;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

/**
 * AMD64 rdtsc operation. The result is in EDX:EAX.
 */
@Opcode("RDTSC")
public class AMD64ReadTimestampCounter extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64ReadTimestampCounter> TYPE = LIRInstructionClass.create(AMD64ReadTimestampCounter.class);

    @Def({REG}) protected AllocatableValue highResult;
    @Def({REG}) protected AllocatableValue lowResult;

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
