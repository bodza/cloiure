package graalvm.compiler.lir.alloc.trace;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.STACK;

import java.util.EnumSet;

import graalvm.compiler.lir.CompositeValue;
import graalvm.compiler.lir.InstructionValueConsumer;
import graalvm.compiler.lir.InstructionValueProcedure;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;

import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Represents a {@link #register} which has a shadow copy on the {@link #stackslot stack}.
 */
public final class ShadowedRegisterValue extends CompositeValue
{
    private static final EnumSet<OperandFlag> registerFlags = EnumSet.of(REG);
    private static final EnumSet<OperandFlag> stackslotFlags = EnumSet.of(STACK);

    @Component({REG}) protected RegisterValue register;
    @Component({STACK}) protected AllocatableValue stackslot;

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
