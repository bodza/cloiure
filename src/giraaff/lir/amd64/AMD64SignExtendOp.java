package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

@Opcode
public class AMD64SignExtendOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64SignExtendOp> TYPE = LIRInstructionClass.create(AMD64SignExtendOp.class);

    private final OperandSize size;

    @Def({OperandFlag.REG}) protected AllocatableValue highResult;
    @Def({OperandFlag.REG}) protected AllocatableValue lowResult;

    @Use({OperandFlag.REG}) protected AllocatableValue input;

    public AMD64SignExtendOp(OperandSize size, LIRKind resultKind, AllocatableValue input)
    {
        super(TYPE);
        this.size = size;

        this.highResult = AMD64.rdx.asValue(resultKind);
        this.lowResult = AMD64.rax.asValue(resultKind);
        this.input = input;
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
        if (size == OperandSize.DWORD)
        {
            masm.cdql();
        }
        else
        {
            masm.cdqq();
        }
    }
}
