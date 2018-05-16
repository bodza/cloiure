package graalvm.compiler.lir;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;

import graalvm.compiler.core.common.FieldIntrospection;
import graalvm.compiler.core.common.Fields;
import graalvm.compiler.core.common.FieldsScanner;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.CompositeValue.Component;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRIntrospection.LIRFieldsScanner;
import graalvm.compiler.lir.LIRIntrospection.OperandModeAnnotation;
import graalvm.compiler.lir.LIRIntrospection.Values;

/**
 * Lazily associated metadata for every {@link CompositeValue} type. The metadata includes:
 * <ul>
 * <li>The offsets of fields annotated with {@link Component} as well as methods for iterating over
 * such fields.</li>
 * </ul>
 */
public final class CompositeValueClass<T> extends FieldIntrospection<T>
{
    /**
     * The CompositeValueClass is only used for formatting for the most part so cache it as a
     * ClassValue.
     */
    private static final ClassValue<CompositeValueClass<?>> compositeClass = new ClassValue<CompositeValueClass<?>>()
    {
        @Override
        protected CompositeValueClass<?> computeValue(Class<?> type)
        {
            CompositeValueClass<?> compositeValueClass = new CompositeValueClass<>(type);
            assert compositeValueClass.values.getDirectCount() == compositeValueClass.values.getCount() : "only direct fields are allowed in composites";
            return compositeValueClass;
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> CompositeValueClass<T> get(Class<T> type)
    {
        return (CompositeValueClass<T>) compositeClass.get(type);
    }

    private final Values values;

    private CompositeValueClass(Class<T> clazz)
    {
        super(clazz);

        CompositeValueFieldsScanner vfs = new CompositeValueFieldsScanner(new FieldsScanner.DefaultCalcOffset());
        vfs.scan(clazz, CompositeValue.class, false);

        values = new Values(vfs.valueAnnotations.get(CompositeValue.Component.class));
        data = new Fields(vfs.data);
    }

    private static class CompositeValueFieldsScanner extends LIRFieldsScanner
    {
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
        return new Fields[]{data, values};
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append(getClass().getSimpleName()).append(" ").append(getClazz().getSimpleName()).append(" components[");
        values.appendFields(str);
        str.append("] data[");
        data.appendFields(str);
        str.append("]");
        return str.toString();
    }

    public static String format(CompositeValue obj)
    {
        CompositeValueClass<?> valueClass = compositeClass.get(obj.getClass());
        StringBuilder result = new StringBuilder();

        LIRIntrospection.appendValues(result, obj, "", "", "{", "}", new String[]{""}, valueClass.values);

        for (int i = 0; i < valueClass.data.getCount(); i++)
        {
            result.append(" ").append(valueClass.data.getName(i)).append(": ").append(LIRIntrospection.getFieldString(obj, i, valueClass.data));
        }

        return result.toString();
    }
}
