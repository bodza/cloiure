package giraaff.lir.amd64;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64ClearRegisterOp
public final class AMD64ClearRegisterOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ClearRegisterOp> TYPE = LIRInstructionClass.create(AMD64ClearRegisterOp.class);

    @LIROpcode
    // @field
    private final AMD64Assembler.AMD64RMOp ___op;
    // @field
    private final AMD64Assembler.OperandSize ___size;

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___result;

    // @cons AMD64ClearRegisterOp
    public AMD64ClearRegisterOp(AMD64Assembler.OperandSize __size, AllocatableValue __result)
    {
        super(TYPE);
        this.___op = AMD64Assembler.AMD64BinaryArithmetic.XOR.getRMOpcode(__size);
        this.___size = __size;
        this.___result = __result;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        this.___op.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___result));
    }
}
