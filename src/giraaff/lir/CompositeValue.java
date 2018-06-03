package giraaff.lir;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;

import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;

/**
 * Base class to represent values that need to be stored in more than one register. This is mainly
 * intended to support addresses and not general arbitrary nesting of composite values. Because of
 * the possibility of sharing of CompositeValues they should be immutable.
 */
// @class CompositeValue
public abstract class CompositeValue extends Value
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Component
    {
        OperandFlag[] value() default OperandFlag.REG;
    }

    // @cons
    public CompositeValue(ValueKind<?> __kind)
    {
        super(__kind);
    }

    /**
     * Invoke {@code proc} on each {@link Value} element of this {@link CompositeValue}.
     * If {@code proc} replaces any value then a new CompositeValue should be returned.
     *
     * @return the original CompositeValue or a copy with any modified values
     */
    public abstract CompositeValue forEachComponent(LIRInstruction inst, OperandMode mode, InstructionValueProcedure proc);

    /**
     * A helper method to visit {@link Value}[] ensuring that a copy of the array is made if it's needed.
     *
     * @return the original {@code values} array or a copy if values changed
     */
    protected Value[] visitValueArray(LIRInstruction __inst, Value[] __values, OperandMode __mode, InstructionValueProcedure __proc, EnumSet<OperandFlag> __flags)
    {
        Value[] __newValues = null;
        for (int __i = 0; __i < __values.length; __i++)
        {
            Value __value = __values[__i];
            Value __newValue = __proc.doValue(__inst, __value, __mode, __flags);
            if (!__value.identityEquals(__newValue))
            {
                if (__newValues == null)
                {
                    __newValues = __values.clone();
                }
                __newValues[__i] = __value;
            }
        }
        return __newValues != null ? __newValues : __values;
    }

    protected abstract void visitEachComponent(LIRInstruction inst, OperandMode mode, InstructionValueConsumer proc);

    @Override
    public int hashCode()
    {
        return 53 * super.hashCode();
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof CompositeValue)
        {
            CompositeValue __other = (CompositeValue) __obj;
            return super.equals(__other);
        }
        return false;
    }
}
