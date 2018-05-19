package graalvm.compiler.lir.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static jdk.vm.ci.code.ValueUtil.isLegal;

import java.util.EnumSet;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64Address.Scale;
import graalvm.compiler.lir.CompositeValue;
import graalvm.compiler.lir.InstructionValueConsumer;
import graalvm.compiler.lir.InstructionValueProcedure;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

public final class AMD64AddressValue extends CompositeValue
{
    @Component({REG, OperandFlag.ILLEGAL}) protected AllocatableValue base;
    @Component({REG, OperandFlag.ILLEGAL}) protected AllocatableValue index;
    protected final Scale scale;
    protected final int displacement;

    private static final EnumSet<OperandFlag> flags = EnumSet.of(OperandFlag.REG, OperandFlag.ILLEGAL);

    public AMD64AddressValue(ValueKind<?> kind, AllocatableValue base, int displacement)
    {
        this(kind, base, Value.ILLEGAL, Scale.Times1, displacement);
    }

    public AMD64AddressValue(ValueKind<?> kind, AllocatableValue base, AllocatableValue index, Scale scale, int displacement)
    {
        super(kind);
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.displacement = displacement;
    }

    public AllocatableValue getBase()
    {
        return base;
    }

    public AllocatableValue getIndex()
    {
        return index;
    }

    @Override
    public CompositeValue forEachComponent(LIRInstruction inst, OperandMode mode, InstructionValueProcedure proc)
    {
        AllocatableValue newBase = (AllocatableValue) proc.doValue(inst, base, mode, flags);
        AllocatableValue newIndex = (AllocatableValue) proc.doValue(inst, index, mode, flags);
        if (!base.identityEquals(newBase) || !index.identityEquals(newIndex))
        {
            return new AMD64AddressValue(getValueKind(), newBase, newIndex, scale, displacement);
        }
        return this;
    }

    @Override
    protected void visitEachComponent(LIRInstruction inst, OperandMode mode, InstructionValueConsumer proc)
    {
        proc.visitValue(inst, base, mode, flags);
        proc.visitValue(inst, index, mode, flags);
    }

    public AMD64AddressValue withKind(ValueKind<?> newKind)
    {
        return new AMD64AddressValue(newKind, base, index, scale, displacement);
    }

    private static Register toRegister(AllocatableValue value)
    {
        if (value.equals(Value.ILLEGAL))
        {
            return Register.None;
        }
        else
        {
            RegisterValue reg = (RegisterValue) value;
            return reg.getRegister();
        }
    }

    public AMD64Address toAddress()
    {
        return new AMD64Address(toRegister(base), toRegister(index), scale, displacement);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder("[");
        String sep = "";
        if (isLegal(base))
        {
            s.append(base);
            sep = " + ";
        }
        if (isLegal(index))
        {
            s.append(sep).append(index).append(" * ").append(scale.value);
            sep = " + ";
        }
        if (displacement < 0)
        {
            s.append(" - ").append(-displacement);
        }
        else if (displacement > 0)
        {
            s.append(sep).append(displacement);
        }
        s.append("]");
        return s.toString();
    }

    public boolean isValidImplicitNullCheckFor(Value value, int implicitNullCheckLimit)
    {
        return value.equals(base) && index.equals(Value.ILLEGAL) && displacement >= 0 && displacement < implicitNullCheckLimit;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof AMD64AddressValue)
        {
            AMD64AddressValue addr = (AMD64AddressValue) obj;
            return getValueKind().equals(addr.getValueKind()) && displacement == addr.displacement && base.equals(addr.base) && scale == addr.scale && index.equals(addr.index);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return base.hashCode() ^ index.hashCode() ^ (displacement << 4) ^ (scale.value << 8) ^ getValueKind().hashCode();
    }
}
