package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Restores registers from stack slots.
 */
@Opcode("RESTORE_REGISTER")
public class AMD64RestoreRegistersOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64RestoreRegistersOp> TYPE = LIRInstructionClass.create(AMD64RestoreRegistersOp.class);

    /**
     * The slots from which the registers are restored.
     */
    @Use(OperandFlag.STACK) protected final AllocatableValue[] slots;

    /**
     * The operation that saved the registers restored by this operation.
     */
    private final AMD64SaveRegistersOp save;

    public AMD64RestoreRegistersOp(AllocatableValue[] values, AMD64SaveRegistersOp save)
    {
        this(TYPE, values, save);
    }

    protected AMD64RestoreRegistersOp(LIRInstructionClass<? extends AMD64RestoreRegistersOp> c, AllocatableValue[] values, AMD64SaveRegistersOp save)
    {
        super(c);
        this.slots = values;
        this.save = save;
    }

    protected Register[] getSavedRegisters()
    {
        return save.savedRegisters;
    }

    protected void restoreRegister(CompilationResultBuilder crb, AMD64MacroAssembler masm, Register result, StackSlot input)
    {
        AMD64Move.stack2reg((AMD64Kind) input.getPlatformKind(), crb, masm, result, input);
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        Register[] savedRegisters = getSavedRegisters();
        for (int i = 0; i < savedRegisters.length; i++)
        {
            if (savedRegisters[i] != null)
            {
                restoreRegister(crb, masm, savedRegisters[i], ValueUtil.asStackSlot(slots[i]));
            }
        }
    }
}
