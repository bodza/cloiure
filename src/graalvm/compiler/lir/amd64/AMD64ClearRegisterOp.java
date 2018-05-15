package graalvm.compiler.lir.amd64;

import static graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic.XOR;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static jdk.vm.ci.code.ValueUtil.asRegister;

import graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp;
import graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.meta.AllocatableValue;

public class AMD64ClearRegisterOp extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64ClearRegisterOp> TYPE = LIRInstructionClass.create(AMD64ClearRegisterOp.class);

    @Opcode private final AMD64RMOp op;
    private final OperandSize size;

    @Def({REG}) protected AllocatableValue result;

    public AMD64ClearRegisterOp(OperandSize size, AllocatableValue result) {
        super(TYPE);
        this.op = XOR.getRMOpcode(size);
        this.size = size;
        this.result = result;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        op.emit(masm, size, asRegister(result), asRegister(result));
    }
}
