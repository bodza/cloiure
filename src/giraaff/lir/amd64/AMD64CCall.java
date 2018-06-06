package giraaff.lir.amd64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64CCall
public final class AMD64CCall extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64CCall> TYPE = LIRInstructionClass.create(AMD64CCall.class);

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
    // @field
    protected Value ___result;
    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
    // @field
    protected Value[] ___parameters;
    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___functionPtr;
    @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
    // @field
    protected Value ___numberOfFloatingPointArguments;

    // @cons AMD64CCall
    public AMD64CCall(Value __result, Value __functionPtr, Value __numberOfFloatingPointArguments, Value[] __parameters)
    {
        super(TYPE);
        this.___result = __result;
        this.___functionPtr = __functionPtr;
        this.___parameters = __parameters;
        this.___numberOfFloatingPointArguments = __numberOfFloatingPointArguments;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        directCall(__masm);
    }

    private void directCall(AMD64MacroAssembler __masm)
    {
        Register __reg = ValueUtil.asRegister(this.___functionPtr);
        __masm.call(__reg);
        __masm.ensureUniquePC();
    }

    @Override
    public boolean destroysCallerSavedRegisters()
    {
        return true;
    }
}
