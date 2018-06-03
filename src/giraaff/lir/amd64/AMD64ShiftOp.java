package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Assembler.AMD64MOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

///
// AMD64 shift/rotate operation. This operation has a single operand for the first input and output.
// The second input must be in the RCX register.
///
// @class AMD64ShiftOp
public final class AMD64ShiftOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ShiftOp> TYPE = LIRInstructionClass.create(AMD64ShiftOp.class);

    @Opcode
    // @field
    private final AMD64MOp ___opcode;
    // @field
    private final OperandSize ___size;

    @Def({OperandFlag.REG, OperandFlag.HINT})
    // @field
    protected AllocatableValue ___result;
    @Use({OperandFlag.REG, OperandFlag.STACK})
    // @field
    protected AllocatableValue ___x;
    @Alive({OperandFlag.REG})
    // @field
    protected AllocatableValue ___y;

    // @cons
    public AMD64ShiftOp(AMD64MOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
    {
        super(TYPE);
        this.___opcode = __opcode;
        this.___size = __size;

        this.___result = __result;
        this.___x = __x;
        this.___y = __y;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        AMD64Move.move(__crb, __masm, this.___result, this.___x);
        this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result));
    }
}
