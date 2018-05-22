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

/**
 * AMD64 shift/rotate operation. This operation has a single operand for the first input and output.
 * The second input must be in the RCX register.
 */
public class AMD64ShiftOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64ShiftOp> TYPE = LIRInstructionClass.create(AMD64ShiftOp.class);

    @Opcode private final AMD64MOp opcode;
    private final OperandSize size;

    @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
    @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue x;
    @Alive({OperandFlag.REG}) protected AllocatableValue y;

    public AMD64ShiftOp(AMD64MOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, AllocatableValue y)
    {
        super(TYPE);
        this.opcode = opcode;
        this.size = size;

        this.result = result;
        this.x = x;
        this.y = y;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        AMD64Move.move(crb, masm, result, x);
        opcode.emit(masm, size, ValueUtil.asRegister(result));
    }
}
