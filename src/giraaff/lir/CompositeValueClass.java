package giraaff.lir;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.EnumSet;

import giraaff.core.common.FieldIntrospection;
import giraaff.core.common.Fields;
import giraaff.core.common.FieldsScanner;
import giraaff.lir.CompositeValue;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRIntrospection;
import giraaff.util.GraalError;

///
// Lazily associated metadata for every {@link CompositeValue} type. The metadata includes:
//
// <li>The offsets of fields annotated with {@link CompositeValue.Component} as well as methods for iterating over
// such fields.</li>
///
// @class CompositeValueClass
public final class CompositeValueClass<T> extends FieldIntrospection<T>
{
    ///
    // The CompositeValueClass is only used for formatting for the most part so cache it as a ClassValue.
    ///
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
    private final LIRIntrospection.Values ___values;

    // @cons CompositeValueClass
    private CompositeValueClass(Class<T> __clazz)
    {
        super(__clazz);

        CompositeValueClass.CompositeValueFieldsScanner __vfs = new CompositeValueClass.CompositeValueFieldsScanner(new FieldsScanner.DefaultCalcOffset());
        __vfs.scan(__clazz, CompositeValue.class, false);

        this.___values = new LIRIntrospection.Values(__vfs.___valueAnnotations.get(CompositeValue.Component.class));
        this.___data = new Fields(__vfs.___data);
    }

    // @class CompositeValueClass.CompositeValueFieldsScanner
    private static final class CompositeValueFieldsScanner extends LIRIntrospection.LIRFieldsScanner
    {
        // @cons CompositeValueClass.CompositeValueFieldsScanner
        CompositeValueFieldsScanner(FieldsScanner.CalcOffset __calc)
        {
            super(__calc);
            this.___valueAnnotations.put(CompositeValue.Component.class, new LIRIntrospection.OperandModeAnnotation());
        }

        @Override
        protected EnumSet<LIRInstruction.OperandFlag> getFlags(Field __field)
        {
            EnumSet<LIRInstruction.OperandFlag> __result = EnumSet.noneOf(LIRInstruction.OperandFlag.class);
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
        return new Fields[] { this.___data, this.___values };
    }
}
