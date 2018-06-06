package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;

///
// AMD64 mul/div operation. This operation has a single operand for the second input. The first
// input must be in RAX for mul and in RDX:RAX for div. The result is in RDX:RAX.
///
// @class AMD64MulDivOp
public final class AMD64MulDivOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64MulDivOp> TYPE = LIRInstructionClass.create(AMD64MulDivOp.class);

    @LIROpcode
    // @field
    private final AMD64Assembler.AMD64MOp ___opcode;
    // @field
    private final AMD64Assembler.OperandSize ___size;

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___highResult;
    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___lowResult;

    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
    // @field
    protected AllocatableValue ___highX;
    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___lowX;

    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
    // @field
    protected AllocatableValue ___y;

    // @State
    // @field
    protected LIRFrameState ___state;

    // @cons AMD64MulDivOp
    public AMD64MulDivOp(AMD64Assembler.AMD64MOp __opcode, AMD64Assembler.OperandSize __size, LIRKind __resultKind, AllocatableValue __x, AllocatableValue __y)
    {
        this(__opcode, __size, __resultKind, Value.ILLEGAL, __x, __y, null);
    }

    // @cons AMD64MulDivOp
    public AMD64MulDivOp(AMD64Assembler.AMD64MOp __opcode, AMD64Assembler.OperandSize __size, LIRKind __resultKind, AllocatableValue __highX, AllocatableValue __lowX, AllocatableValue __y, LIRFrameState __state)
    {
        super(TYPE);
        this.___opcode = __opcode;
        this.___size = __size;

        this.___highResult = AMD64.rdx.asValue(__resultKind);
        this.___lowResult = AMD64.rax.asValue(__resultKind);

        this.___highX = __highX;
        this.___lowX = __lowX;

        this.___y = __y;

        this.___state = __state;
    }

    public AllocatableValue getHighResult()
    {
        return this.___highResult;
    }

    public AllocatableValue getLowResult()
    {
        return this.___lowResult;
    }

    public AllocatableValue getQuotient()
    {
        return this.___lowResult;
    }

    public AllocatableValue getRemainder()
    {
        return this.___highResult;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        if (ValueUtil.isRegister(this.___y))
        {
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___y));
        }
        else
        {
            this.___opcode.emit(__masm, this.___size, (AMD64Address) __crb.asAddress(this.___y));
        }
    }
}
