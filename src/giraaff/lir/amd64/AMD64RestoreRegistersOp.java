package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Restores registers from stack slots.
///
@LIROpcode
// @class AMD64RestoreRegistersOp
public final class AMD64RestoreRegistersOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64RestoreRegistersOp> TYPE = LIRInstructionClass.create(AMD64RestoreRegistersOp.class);

    ///
    // The slots from which the registers are restored.
    ///
    @LIRInstruction.Use(LIRInstruction.OperandFlag.STACK)
    // @field
    protected final AllocatableValue[] ___slots;

    ///
    // The operation that saved the registers restored by this operation.
    ///
    // @field
    private final AMD64SaveRegistersOp ___save;

    // @cons AMD64RestoreRegistersOp
    public AMD64RestoreRegistersOp(AllocatableValue[] __values, AMD64SaveRegistersOp __save)
    {
        this(TYPE, __values, __save);
    }

    // @cons AMD64RestoreRegistersOp
    protected AMD64RestoreRegistersOp(LIRInstructionClass<? extends AMD64RestoreRegistersOp> __c, AllocatableValue[] __values, AMD64SaveRegistersOp __save)
    {
        super(__c);
        this.___slots = __values;
        this.___save = __save;
    }

    protected Register[] getSavedRegisters()
    {
        return this.___save.___savedRegisters;
    }

    protected void restoreRegister(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __result, StackSlot __input)
    {
        AMD64Move.stack2reg((AMD64Kind) __input.getPlatformKind(), __crb, __masm, __result, __input);
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        Register[] __savedRegisters = getSavedRegisters();
        for (int __i = 0; __i < __savedRegisters.length; __i++)
        {
            if (__savedRegisters[__i] != null)
            {
                restoreRegister(__crb, __masm, __savedRegisters[__i], ValueUtil.asStackSlot(this.___slots[__i]));
            }
        }
    }
}
