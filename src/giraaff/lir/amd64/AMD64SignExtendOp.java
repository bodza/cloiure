package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;

@LIROpcode
// @class AMD64SignExtendOp
public final class AMD64SignExtendOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64SignExtendOp> TYPE = LIRInstructionClass.create(AMD64SignExtendOp.class);

    // @field
    private final AMD64Assembler.OperandSize ___size;

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___highResult;
    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___lowResult;

    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___input;

    // @cons AMD64SignExtendOp
    public AMD64SignExtendOp(AMD64Assembler.OperandSize __size, LIRKind __resultKind, AllocatableValue __input)
    {
        super(TYPE);
        this.___size = __size;

        this.___highResult = AMD64.rdx.asValue(__resultKind);
        this.___lowResult = AMD64.rax.asValue(__resultKind);
        this.___input = __input;
    }

    public AllocatableValue getHighResult()
    {
        return this.___highResult;
    }

    public AllocatableValue getLowResult()
    {
        return this.___lowResult;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        if (this.___size == AMD64Assembler.OperandSize.DWORD)
        {
            __masm.cdql();
        }
        else
        {
            __masm.cdqq();
        }
    }
}
