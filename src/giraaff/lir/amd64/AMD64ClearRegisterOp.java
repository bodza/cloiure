package giraaff.lir.amd64;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic;
import giraaff.asm.amd64.AMD64Assembler.AMD64RMOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64ClearRegisterOp
public final class AMD64ClearRegisterOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ClearRegisterOp> TYPE = LIRInstructionClass.create(AMD64ClearRegisterOp.class);

    @Opcode
    // @field
    private final AMD64RMOp op;
    // @field
    private final OperandSize size;

    @Def({OperandFlag.REG})
    // @field
    protected AllocatableValue result;

    // @cons
    public AMD64ClearRegisterOp(OperandSize __size, AllocatableValue __result)
    {
        super(TYPE);
        this.op = AMD64BinaryArithmetic.XOR.getRMOpcode(__size);
        this.size = __size;
        this.result = __result;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        op.emit(__masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(result));
    }
}
