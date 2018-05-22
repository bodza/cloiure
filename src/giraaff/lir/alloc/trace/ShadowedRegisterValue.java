package giraaff.lir.alloc.trace;

import java.util.EnumSet;

import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.lir.CompositeValue;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.InstructionValueProcedure;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;

/**
 * Represents a {@link #register} which has a shadow copy on the {@link #stackslot stack}.
 */
public final class ShadowedRegisterValue extends CompositeValue
{
    private static final EnumSet<OperandFlag> registerFlags = EnumSet.of(OperandFlag.REG);
    private static final EnumSet<OperandFlag> stackslotFlags = EnumSet.of(OperandFlag.STACK);

    @Component({OperandFlag.REG}) protected RegisterValue register;
    @Component({OperandFlag.STACK}) protected AllocatableValue stackslot;

    public ShadowedRegisterValue(RegisterValue register, AllocatableValue stackslot)
    {
        super(register.getValueKind());
        this.register = register;
        this.stackslot = stackslot;
    }

    public RegisterValue getRegister()
    {
        return register;
    }

    public AllocatableValue getStackSlot()
    {
        return stackslot;
    }

    @Override
    public CompositeValue forEachComponent(LIRInstruction inst, OperandMode mode, InstructionValueProcedure proc)
    {
        RegisterValue newRegister = (RegisterValue) proc.doValue(inst, register, mode, registerFlags);
        AllocatableValue newStackSlot = (AllocatableValue) proc.doValue(inst, stackslot, mode, stackslotFlags);
        if (register.equals(newRegister) || stackslot.equals(newStackSlot))
        {
            return this;
        }
        return new ShadowedRegisterValue(newRegister, newStackSlot);
    }

    @Override
    protected void visitEachComponent(LIRInstruction inst, OperandMode mode, InstructionValueConsumer proc)
    {
        proc.visitValue(inst, register, mode, registerFlags);
        proc.visitValue(inst, stackslot, mode, stackslotFlags);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (this == obj)
        {
            return true;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        ShadowedRegisterValue other = (ShadowedRegisterValue) obj;
        if (!register.equals(other.register))
        {
            return false;
        }
        if (!stackslot.equals(other.stackslot))
        {
            return false;
        }
        return true;
    }
}
