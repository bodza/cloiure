package giraaff.lir.amd64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64CCall
public final class AMD64CCall extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64CCall> TYPE = LIRInstructionClass.create(AMD64CCall.class);

    @Def({OperandFlag.REG, OperandFlag.ILLEGAL}) protected Value result;
    @Use({OperandFlag.REG, OperandFlag.STACK}) protected Value[] parameters;
    @Use({OperandFlag.REG}) protected Value functionPtr;
    @Use({OperandFlag.REG}) protected Value numberOfFloatingPointArguments;

    // @cons
    public AMD64CCall(Value result, Value functionPtr, Value numberOfFloatingPointArguments, Value[] parameters)
    {
        super(TYPE);
        this.result = result;
        this.functionPtr = functionPtr;
        this.parameters = parameters;
        this.numberOfFloatingPointArguments = numberOfFloatingPointArguments;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        directCall(masm);
    }

    private void directCall(AMD64MacroAssembler masm)
    {
        Register reg = ValueUtil.asRegister(functionPtr);
        masm.call(reg);
        masm.ensureUniquePC();
    }

    @Override
    public boolean destroysCallerSavedRegisters()
    {
        return true;
    }
}
