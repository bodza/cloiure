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
    private final AMD64RMOp ___op;
    // @field
    private final OperandSize ___size;

    @Def({OperandFlag.REG})
    // @field
    protected AllocatableValue ___result;

    // @cons
    public AMD64ClearRegisterOp(OperandSize __size, AllocatableValue __result)
    {
        super(TYPE);
        this.___op = AMD64BinaryArithmetic.XOR.getRMOpcode(__size);
        this.___size = __size;
        this.___result = __result;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        this.___op.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___result));
    }
}
