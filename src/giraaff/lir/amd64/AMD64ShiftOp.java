package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
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

    @LIROpcode
    // @field
    private final AMD64Assembler.AMD64MOp ___opcode;
    // @field
    private final AMD64Assembler.OperandSize ___size;

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
    // @field
    protected AllocatableValue ___result;
    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
    // @field
    protected AllocatableValue ___x;
    @LIRInstruction.Alive({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___y;

    // @cons AMD64ShiftOp
    public AMD64ShiftOp(AMD64Assembler.AMD64MOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
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
