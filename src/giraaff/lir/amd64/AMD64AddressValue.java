package giraaff.lir.amd64;

import java.util.EnumSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.asm.amd64.AMD64Address;
import giraaff.lir.CompositeValue;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.InstructionValueProcedure;
import giraaff.lir.LIRInstruction;

// @class AMD64AddressValue
public final class AMD64AddressValue extends CompositeValue
{
    @CompositeValue.Component({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
    // @field
    protected AllocatableValue ___base;
    @CompositeValue.Component({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
    // @field
    protected AllocatableValue ___index;
    // @field
    protected final AMD64Address.Scale ___scale;
    // @field
    protected final int ___displacement;

    // @def
    private static final EnumSet<LIRInstruction.OperandFlag> flags = EnumSet.of(LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL);

    // @cons AMD64AddressValue
    public AMD64AddressValue(ValueKind<?> __kind, AllocatableValue __base, int __displacement)
    {
        this(__kind, __base, Value.ILLEGAL, AMD64Address.Scale.Times1, __displacement);
    }

    // @cons AMD64AddressValue
    public AMD64AddressValue(ValueKind<?> __kind, AllocatableValue __base, AllocatableValue __index, AMD64Address.Scale __scale, int __displacement)
    {
        super(__kind);
        this.___base = __base;
        this.___index = __index;
        this.___scale = __scale;
        this.___displacement = __displacement;
    }

    public AllocatableValue getBase()
    {
        return this.___base;
    }

    public AllocatableValue getIndex()
    {
        return this.___index;
    }

    @Override
    public CompositeValue forEachComponent(LIRInstruction __inst, LIRInstruction.OperandMode __mode, InstructionValueProcedure __proc)
    {
        AllocatableValue __newBase = (AllocatableValue) __proc.doValue(__inst, this.___base, __mode, flags);
        AllocatableValue __newIndex = (AllocatableValue) __proc.doValue(__inst, this.___index, __mode, flags);
        if (!this.___base.identityEquals(__newBase) || !this.___index.identityEquals(__newIndex))
        {
            return new AMD64AddressValue(getValueKind(), __newBase, __newIndex, this.___scale, this.___displacement);
        }
        return this;
    }

    @Override
    protected void visitEachComponent(LIRInstruction __inst, LIRInstruction.OperandMode __mode, InstructionValueConsumer __proc)
    {
        __proc.visitValue(__inst, this.___base, __mode, flags);
        __proc.visitValue(__inst, this.___index, __mode, flags);
    }

    public AMD64AddressValue withKind(ValueKind<?> __newKind)
    {
        return new AMD64AddressValue(__newKind, this.___base, this.___index, this.___scale, this.___displacement);
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
        return new AMD64Address(toRegister(this.___base), toRegister(this.___index), this.___scale, this.___displacement);
    }

    public boolean isValidImplicitNullCheckFor(Value __value, int __implicitNullCheckLimit)
    {
        return __value.equals(this.___base) && this.___index.equals(Value.ILLEGAL) && this.___displacement >= 0 && this.___displacement < __implicitNullCheckLimit;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof AMD64AddressValue)
        {
            AMD64AddressValue __addr = (AMD64AddressValue) __obj;
            return getValueKind().equals(__addr.getValueKind()) && this.___displacement == __addr.___displacement && this.___base.equals(__addr.___base) && this.___scale == __addr.___scale && this.___index.equals(__addr.___index);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return this.___base.hashCode() ^ this.___index.hashCode() ^ (this.___displacement << 4) ^ (this.___scale.___value << 8) ^ getValueKind().hashCode();
    }
}
