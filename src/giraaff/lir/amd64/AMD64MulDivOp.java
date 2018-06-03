package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.AMD64MOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
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

    @Opcode
    // @field
    private final AMD64MOp ___opcode;
    // @field
    private final OperandSize ___size;

    @Def({OperandFlag.REG})
    // @field
    protected AllocatableValue ___highResult;
    @Def({OperandFlag.REG})
    // @field
    protected AllocatableValue ___lowResult;

    @Use({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected AllocatableValue ___highX;
    @Use({OperandFlag.REG})
    // @field
    protected AllocatableValue ___lowX;

    @Use({OperandFlag.REG, OperandFlag.STACK})
    // @field
    protected AllocatableValue ___y;

    // @State
    // @field
    protected LIRFrameState ___state;

    // @cons
    public AMD64MulDivOp(AMD64MOp __opcode, OperandSize __size, LIRKind __resultKind, AllocatableValue __x, AllocatableValue __y)
    {
        this(__opcode, __size, __resultKind, Value.ILLEGAL, __x, __y, null);
    }

    // @cons
    public AMD64MulDivOp(AMD64MOp __opcode, OperandSize __size, LIRKind __resultKind, AllocatableValue __highX, AllocatableValue __lowX, AllocatableValue __y, LIRFrameState __state)
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
