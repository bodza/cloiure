package giraaff.lir;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.MapCursor;

import giraaff.core.common.FieldIntrospection;
import giraaff.core.common.Fields;
import giraaff.core.common.FieldsScanner;
import giraaff.lir.LIRInstruction;

// @class LIRIntrospection
abstract class LIRIntrospection<T> extends FieldIntrospection<T>
{
    // @def
    private static final Class<Value> VALUE_CLASS = Value.class;
    // @def
    private static final Class<ConstantValue> CONSTANT_VALUE_CLASS = ConstantValue.class;
    // @def
    private static final Class<Variable> VARIABLE_CLASS = Variable.class;
    // @def
    private static final Class<RegisterValue> REGISTER_VALUE_CLASS = RegisterValue.class;
    // @def
    private static final Class<StackSlot> STACK_SLOT_CLASS = StackSlot.class;
    // @def
    private static final Class<Value[]> VALUE_ARRAY_CLASS = Value[].class;

    // @cons LIRIntrospection
    LIRIntrospection(Class<T> __clazz)
    {
        super(__clazz);
    }

    // @class LIRIntrospection.Values
    protected static final class Values extends Fields
    {
        // @field
        private final int ___directCount;
        // @field
        private final EnumSet<LIRInstruction.OperandFlag>[] ___flags;

        // @cons LIRIntrospection.Values
        public Values(LIRIntrospection.OperandModeAnnotation __mode)
        {
            this(__mode.___directCount, __mode.___values);
        }

        @SuppressWarnings({"unchecked"})
        // @cons LIRIntrospection.Values
        public Values(int __directCount, ArrayList<LIRIntrospection.ValueFieldInfo> __fields)
        {
            super(__fields);
            this.___directCount = __directCount;
            this.___flags = (EnumSet<LIRInstruction.OperandFlag>[]) new EnumSet<?>[__fields.size()];
            for (int __i = 0; __i < __fields.size(); __i++)
            {
                this.___flags[__i] = __fields.get(__i).___flags;
            }
        }

        public int getDirectCount()
        {
            return this.___directCount;
        }

        public EnumSet<LIRInstruction.OperandFlag> getFlags(int __i)
        {
            return this.___flags[__i];
        }

        protected Value getValue(Object __obj, int __index)
        {
            return (Value) getObject(__obj, __index);
        }

        protected void setValue(Object __obj, int __index, Value __value)
        {
            putObject(__obj, __index, __value);
        }

        protected Value[] getValueArray(Object __obj, int __index)
        {
            return (Value[]) getObject(__obj, __index);
        }

        protected void setValueArray(Object __obj, int __index, Value[] __valueArray)
        {
            putObject(__obj, __index, __valueArray);
        }
    }

    ///
    // The component values in an {@link LIRInstruction} or {@link CompositeValue}.
    ///
    // @field
    protected LIRIntrospection.Values ___values;

    // @class LIRIntrospection.ValueFieldInfo
    protected static final class ValueFieldInfo extends FieldsScanner.FieldInfo
    {
        // @field
        final EnumSet<LIRInstruction.OperandFlag> ___flags;

        // @cons LIRIntrospection.ValueFieldInfo
        public ValueFieldInfo(long __offset, String __name, Class<?> __type, Class<?> __declaringClass, EnumSet<LIRInstruction.OperandFlag> __flags)
        {
            super(__offset, __name, __type, __declaringClass);
            this.___flags = __flags;
        }

        ///
        // Sorts non-array fields before array fields.
        ///
        @Override
        public int compareTo(FieldsScanner.FieldInfo __o)
        {
            if (VALUE_ARRAY_CLASS.isAssignableFrom(__o.___type))
            {
                if (!VALUE_ARRAY_CLASS.isAssignableFrom(this.___type))
                {
                    return -1;
                }
            }
            else
            {
                if (VALUE_ARRAY_CLASS.isAssignableFrom(this.___type))
                {
                    return 1;
                }
            }
            return super.compareTo(__o);
        }
    }

    // @class LIRIntrospection.OperandModeAnnotation
    protected static final class OperandModeAnnotation
    {
        ///
        // Number of non-array fields in {@link #values}.
        ///
        // @field
        public int ___directCount;
        // @field
        public final ArrayList<LIRIntrospection.ValueFieldInfo> ___values = new ArrayList<>();
    }

    // @class LIRIntrospection.LIRFieldsScanner
    protected abstract static class LIRFieldsScanner extends FieldsScanner
    {
        // @field
        public final EconomicMap<Class<? extends Annotation>, LIRIntrospection.OperandModeAnnotation> ___valueAnnotations;

        // @cons LIRIntrospection.LIRFieldsScanner
        public LIRFieldsScanner(FieldsScanner.CalcOffset __calc)
        {
            super(__calc);
            this.___valueAnnotations = EconomicMap.create(Equivalence.DEFAULT);
        }

        protected LIRIntrospection.OperandModeAnnotation getOperandModeAnnotation(Field __field)
        {
            LIRIntrospection.OperandModeAnnotation __result = null;
            MapCursor<Class<? extends Annotation>, LIRIntrospection.OperandModeAnnotation> __cursor = this.___valueAnnotations.getEntries();
            while (__cursor.advance())
            {
                Annotation __annotation = __field.getAnnotation(__cursor.getKey());
                if (__annotation != null)
                {
                    __result = __cursor.getValue();
                }
            }
            return __result;
        }

        protected abstract EnumSet<LIRInstruction.OperandFlag> getFlags(Field __field);

        @Override
        protected void scanField(Field __field, long __offset)
        {
            Class<?> __type = __field.getType();
            if (VALUE_CLASS.isAssignableFrom(__type) && !CONSTANT_VALUE_CLASS.isAssignableFrom(__type))
            {
                LIRIntrospection.OperandModeAnnotation __annotation = getOperandModeAnnotation(__field);
                EnumSet<LIRInstruction.OperandFlag> __flags = getFlags(__field);
                __annotation.___values.add(new LIRIntrospection.ValueFieldInfo(__offset, __field.getName(), __type, __field.getDeclaringClass(), __flags));
                __annotation.___directCount++;
            }
            else if (VALUE_ARRAY_CLASS.isAssignableFrom(__type))
            {
                LIRIntrospection.OperandModeAnnotation __annotation = getOperandModeAnnotation(__field);
                EnumSet<LIRInstruction.OperandFlag> __flags = getFlags(__field);
                __annotation.___values.add(new LIRIntrospection.ValueFieldInfo(__offset, __field.getName(), __type, __field.getDeclaringClass(), __flags));
            }
            else
            {
                super.scanField(__field, __offset);
            }
        }
    }

    protected static void forEach(LIRInstruction __inst, LIRIntrospection.Values __values, LIRInstruction.OperandMode __mode, InstructionValueProcedure __proc)
    {
        for (int __i = 0; __i < __values.getCount(); __i++)
        {
            if (__i < __values.getDirectCount())
            {
                Value __value = __values.getValue(__inst, __i);
                Value __newValue;
                if (__value instanceof CompositeValue)
                {
                    CompositeValue __composite = (CompositeValue) __value;
                    __newValue = __composite.forEachComponent(__inst, __mode, __proc);
                }
                else
                {
                    __newValue = __proc.doValue(__inst, __value, __mode, __values.getFlags(__i));
                }
                if (!__value.identityEquals(__newValue))
                {
                    __values.setValue(__inst, __i, __newValue);
                }
            }
            else
            {
                Value[] __valueArray = __values.getValueArray(__inst, __i);
                for (int __j = 0; __j < __valueArray.length; __j++)
                {
                    Value __value = __valueArray[__j];
                    Value __newValue;
                    if (__value instanceof CompositeValue)
                    {
                        CompositeValue __composite = (CompositeValue) __value;
                        __newValue = __composite.forEachComponent(__inst, __mode, __proc);
                    }
                    else
                    {
                        __newValue = __proc.doValue(__inst, __value, __mode, __values.getFlags(__i));
                    }
                    if (!__value.identityEquals(__newValue))
                    {
                        __valueArray[__j] = __newValue;
                    }
                }
            }
        }
    }

    protected static void visitEach(LIRInstruction __inst, LIRIntrospection.Values __values, LIRInstruction.OperandMode __mode, InstructionValueConsumer __proc)
    {
        for (int __i = 0; __i < __values.getCount(); __i++)
        {
            if (__i < __values.getDirectCount())
            {
                Value __value = __values.getValue(__inst, __i);
                if (__value instanceof CompositeValue)
                {
                    CompositeValue __composite = (CompositeValue) __value;
                    __composite.visitEachComponent(__inst, __mode, __proc);
                }
                else
                {
                    __proc.visitValue(__inst, __value, __mode, __values.getFlags(__i));
                }
            }
            else
            {
                Value[] __valueArray = __values.getValueArray(__inst, __i);
                for (int __j = 0; __j < __valueArray.length; __j++)
                {
                    Value __value = __valueArray[__j];
                    if (__value instanceof CompositeValue)
                    {
                        CompositeValue __composite = (CompositeValue) __value;
                        __composite.visitEachComponent(__inst, __mode, __proc);
                    }
                    else
                    {
                        __proc.visitValue(__inst, __value, __mode, __values.getFlags(__i));
                    }
                }
            }
        }
    }
}
