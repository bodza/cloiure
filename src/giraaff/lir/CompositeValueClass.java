package giraaff.lir;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;

import giraaff.core.common.FieldIntrospection;
import giraaff.core.common.Fields;
import giraaff.core.common.FieldsScanner;
import giraaff.lir.CompositeValue.Component;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRIntrospection.LIRFieldsScanner;
import giraaff.lir.LIRIntrospection.OperandModeAnnotation;
import giraaff.lir.LIRIntrospection.Values;
import giraaff.util.GraalError;

/**
 * Lazily associated metadata for every {@link CompositeValue} type. The metadata includes:
 *
 * <li>The offsets of fields annotated with {@link Component} as well as methods for iterating over
 * such fields.</li>
 */
// @class CompositeValueClass
public final class CompositeValueClass<T> extends FieldIntrospection<T>
{
    /**
     * The CompositeValueClass is only used for formatting for the most part so cache it as a ClassValue.
     */
    // @closure
    private static final ClassValue<CompositeValueClass<?>> compositeClass = new ClassValue<CompositeValueClass<?>>()
    {
        @Override
        protected CompositeValueClass<?> computeValue(Class<?> type)
        {
            return new CompositeValueClass<>(type);
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> CompositeValueClass<T> get(Class<T> type)
    {
        return (CompositeValueClass<T>) compositeClass.get(type);
    }

    private final Values values;

    // @cons
    private CompositeValueClass(Class<T> clazz)
    {
        super(clazz);

        CompositeValueFieldsScanner vfs = new CompositeValueFieldsScanner(new FieldsScanner.DefaultCalcOffset());
        vfs.scan(clazz, CompositeValue.class, false);

        values = new Values(vfs.valueAnnotations.get(CompositeValue.Component.class));
        data = new Fields(vfs.data);
    }

    // @class CompositeValueClass.CompositeValueFieldsScanner
    private static final class CompositeValueFieldsScanner extends LIRFieldsScanner
    {
        // @cons
        CompositeValueFieldsScanner(FieldsScanner.CalcOffset calc)
        {
            super(calc);
            valueAnnotations.put(CompositeValue.Component.class, new OperandModeAnnotation());
        }

        @Override
        protected EnumSet<OperandFlag> getFlags(Field field)
        {
            EnumSet<OperandFlag> result = EnumSet.noneOf(OperandFlag.class);
            if (field.isAnnotationPresent(CompositeValue.Component.class))
            {
                result.addAll(Arrays.asList(field.getAnnotation(CompositeValue.Component.class).value()));
            }
            else
            {
                GraalError.shouldNotReachHere();
            }
            return result;
        }
    }

    @Override
    public Fields[] getAllFields()
    {
        return new Fields[] { data, values };
    }
}
