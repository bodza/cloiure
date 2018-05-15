package graalvm.compiler.lir.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.ILLEGAL;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.STACK;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

public final class AMD64CCall extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64CCall> TYPE = LIRInstructionClass.create(AMD64CCall.class);

    @Def({REG, ILLEGAL}) protected Value result;
    @Use({REG, STACK}) protected Value[] parameters;
    @Use({REG}) protected Value functionPtr;
    @Use({REG}) protected Value numberOfFloatingPointArguments;

    public AMD64CCall(Value result, Value functionPtr, Value numberOfFloatingPointArguments, Value[] parameters) {
        super(TYPE);
        this.result = result;
        this.functionPtr = functionPtr;
        this.parameters = parameters;
        this.numberOfFloatingPointArguments = numberOfFloatingPointArguments;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        directCall(masm);
    }

    private void directCall(AMD64MacroAssembler masm) {
        Register reg = ValueUtil.asRegister(functionPtr);
        masm.call(reg);
        masm.ensureUniquePC();
    }

    @Override
    public boolean destroysCallerSavedRegisters() {
        return true;
    }

}
