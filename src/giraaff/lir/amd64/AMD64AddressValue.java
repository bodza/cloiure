package giraaff.lir.amd64;

import java.util.EnumSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.lir.CompositeValue;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.InstructionValueProcedure;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;

// @class AMD64AddressValue
public final class AMD64AddressValue extends CompositeValue
{
    @Component({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected AllocatableValue base;
    @Component({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected AllocatableValue index;
    // @field
    protected final Scale scale;
    // @field
    protected final int displacement;

    // @def
    private static final EnumSet<OperandFlag> flags = EnumSet.of(OperandFlag.REG, OperandFlag.ILLEGAL);

    // @cons
    public AMD64AddressValue(ValueKind<?> __kind, AllocatableValue __base, int __displacement)
    {
        this(__kind, __base, Value.ILLEGAL, Scale.Times1, __displacement);
    }

    // @cons
    public AMD64AddressValue(ValueKind<?> __kind, AllocatableValue __base, AllocatableValue __index, Scale __scale, int __displacement)
    {
        super(__kind);
        this.base = __base;
        this.index = __index;
        this.scale = __scale;
        this.displacement = __displacement;
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
    public CompositeValue forEachComponent(LIRInstruction __inst, OperandMode __mode, InstructionValueProcedure __proc)
    {
        AllocatableValue __newBase = (AllocatableValue) __proc.doValue(__inst, base, __mode, flags);
        AllocatableValue __newIndex = (AllocatableValue) __proc.doValue(__inst, index, __mode, flags);
        if (!base.identityEquals(__newBase) || !index.identityEquals(__newIndex))
        {
            return new AMD64AddressValue(getValueKind(), __newBase, __newIndex, scale, displacement);
        }
        return this;
    }

    @Override
    protected void visitEachComponent(LIRInstruction __inst, OperandMode __mode, InstructionValueConsumer __proc)
    {
        __proc.visitValue(__inst, base, __mode, flags);
        __proc.visitValue(__inst, index, __mode, flags);
    }

    public AMD64AddressValue withKind(ValueKind<?> __newKind)
    {
        return new AMD64AddressValue(__newKind, base, index, scale, displacement);
    }

    private static Register toRegister(AllocatableValue __value)
    {
        if (__value.equals(Value.ILLEGAL))
        {
            return Register.None;
        }
        else
        {
            RegisterValue __reg = (RegisterValue) __value;
            return __reg.getRegister();
        }
    }

    public AMD64Address toAddress()
    {
        return new AMD64Address(toRegister(base), toRegister(index), scale, displacement);
    }

    public boolean isValidImplicitNullCheckFor(Value __value, int __implicitNullCheckLimit)
    {
        return __value.equals(base) && index.equals(Value.ILLEGAL) && displacement >= 0 && displacement < __implicitNullCheckLimit;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof AMD64AddressValue)
        {
            AMD64AddressValue __addr = (AMD64AddressValue) __obj;
            return getValueKind().equals(__addr.getValueKind()) && displacement == __addr.displacement && base.equals(__addr.base) && scale == __addr.scale && index.equals(__addr.index);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return base.hashCode() ^ index.hashCode() ^ (displacement << 4) ^ (scale.value << 8) ^ getValueKind().hashCode();
    }
}
