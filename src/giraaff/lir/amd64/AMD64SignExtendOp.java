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
// @class AMD64SignExtendOp
public final class AMD64SignExtendOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64SignExtendOp> TYPE = LIRInstructionClass.create(AMD64SignExtendOp.class);

    // @field
    private final OperandSize size;

    @Def({OperandFlag.REG})
    // @field
    protected AllocatableValue highResult;
    @Def({OperandFlag.REG})
    // @field
    protected AllocatableValue lowResult;

    @Use({OperandFlag.REG})
    // @field
    protected AllocatableValue input;

    // @cons
    public AMD64SignExtendOp(OperandSize __size, LIRKind __resultKind, AllocatableValue __input)
    {
        super(TYPE);
        this.size = __size;

        this.highResult = AMD64.rdx.asValue(__resultKind);
        this.lowResult = AMD64.rax.asValue(__resultKind);
        this.input = __input;
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
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        if (size == OperandSize.DWORD)
        {
            __masm.cdql();
        }
        else
        {
            __masm.cdqq();
        }
    }
}
