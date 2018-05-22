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
public abstract class CompositeValue extends Value
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Component
    {
        OperandFlag[] value() default OperandFlag.REG;
    }

    public CompositeValue(ValueKind<?> kind)
    {
        super(kind);
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
    protected Value[] visitValueArray(LIRInstruction inst, Value[] values, OperandMode mode, InstructionValueProcedure proc, EnumSet<OperandFlag> flags)
    {
        Value[] newValues = null;
        for (int i = 0; i < values.length; i++)
        {
            Value value = values[i];
            Value newValue = proc.doValue(inst, value, mode, flags);
            if (!value.identityEquals(newValue))
            {
                if (newValues == null)
                {
                    newValues = values.clone();
                }
                newValues[i] = value;
            }
        }
        return newValues != null ? newValues : values;
    }

    protected abstract void visitEachComponent(LIRInstruction inst, OperandMode mode, InstructionValueConsumer proc);

    @Override
    public String toString()
    {
        return CompositeValueClass.format(this);
    }

    @Override
    public int hashCode()
    {
        return 53 * super.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CompositeValue)
        {
            CompositeValue other = (CompositeValue) obj;
            return super.equals(other);
        }
        return false;
    }
}
