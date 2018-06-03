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
        protected CompositeValueClass<?> computeValue(Class<?> __type)
        {
            return new CompositeValueClass<>(__type);
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> CompositeValueClass<T> get(Class<T> __type)
    {
        return (CompositeValueClass<T>) compositeClass.get(__type);
    }

    // @field
    private final Values values;

    // @cons
    private CompositeValueClass(Class<T> __clazz)
    {
        super(__clazz);

        CompositeValueFieldsScanner __vfs = new CompositeValueFieldsScanner(new FieldsScanner.DefaultCalcOffset());
        __vfs.scan(__clazz, CompositeValue.class, false);

        values = new Values(__vfs.valueAnnotations.get(CompositeValue.Component.class));
        data = new Fields(__vfs.data);
    }

    // @class CompositeValueClass.CompositeValueFieldsScanner
    private static final class CompositeValueFieldsScanner extends LIRFieldsScanner
    {
        // @cons
        CompositeValueFieldsScanner(FieldsScanner.CalcOffset __calc)
        {
            super(__calc);
            valueAnnotations.put(CompositeValue.Component.class, new OperandModeAnnotation());
        }

        @Override
        protected EnumSet<OperandFlag> getFlags(Field __field)
        {
            EnumSet<OperandFlag> __result = EnumSet.noneOf(OperandFlag.class);
            if (__field.isAnnotationPresent(CompositeValue.Component.class))
            {
                __result.addAll(Arrays.asList(__field.getAnnotation(CompositeValue.Component.class).value()));
            }
            else
            {
                GraalError.shouldNotReachHere();
            }
            return __result;
        }
    }

    @Override
    public Fields[] getAllFields()
    {
        return new Fields[] { data, values };
    }
}
