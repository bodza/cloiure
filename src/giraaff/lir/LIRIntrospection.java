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
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;

// @class LIRIntrospection
abstract class LIRIntrospection<T> extends FieldIntrospection<T>
{
    private static final Class<Value> VALUE_CLASS = Value.class;
    private static final Class<ConstantValue> CONSTANT_VALUE_CLASS = ConstantValue.class;
    private static final Class<Variable> VARIABLE_CLASS = Variable.class;
    private static final Class<RegisterValue> REGISTER_VALUE_CLASS = RegisterValue.class;
    private static final Class<StackSlot> STACK_SLOT_CLASS = StackSlot.class;
    private static final Class<Value[]> VALUE_ARRAY_CLASS = Value[].class;

    // @cons
    LIRIntrospection(Class<T> clazz)
    {
        super(clazz);
    }

    // @class LIRIntrospection.Values
    protected static final class Values extends Fields
    {
        private final int directCount;
        private final EnumSet<OperandFlag>[] flags;

        // @cons
        public Values(OperandModeAnnotation mode)
        {
            this(mode.directCount, mode.values);
        }

        @SuppressWarnings({"unchecked"})
        // @cons
        public Values(int directCount, ArrayList<ValueFieldInfo> fields)
        {
            super(fields);
            this.directCount = directCount;
            flags = (EnumSet<OperandFlag>[]) new EnumSet<?>[fields.size()];
            for (int i = 0; i < fields.size(); i++)
            {
                flags[i] = fields.get(i).flags;
            }
        }

        public int getDirectCount()
        {
            return directCount;
        }

        public EnumSet<OperandFlag> getFlags(int i)
        {
            return flags[i];
        }

        protected Value getValue(Object obj, int index)
        {
            return (Value) getObject(obj, index);
        }

        protected void setValue(Object obj, int index, Value value)
        {
            putObject(obj, index, value);
        }

        protected Value[] getValueArray(Object obj, int index)
        {
            return (Value[]) getObject(obj, index);
        }

        protected void setValueArray(Object obj, int index, Value[] valueArray)
        {
            putObject(obj, index, valueArray);
        }
    }

    /**
     * The component values in an {@link LIRInstruction} or {@link CompositeValue}.
     */
    protected Values values;

    // @class LIRIntrospection.ValueFieldInfo
    protected static final class ValueFieldInfo extends FieldsScanner.FieldInfo
    {
        final EnumSet<OperandFlag> flags;

        // @cons
        public ValueFieldInfo(long offset, String name, Class<?> type, Class<?> declaringClass, EnumSet<OperandFlag> flags)
        {
            super(offset, name, type, declaringClass);
            this.flags = flags;
        }

        /**
         * Sorts non-array fields before array fields.
         */
        @Override
        public int compareTo(FieldsScanner.FieldInfo o)
        {
            if (VALUE_ARRAY_CLASS.isAssignableFrom(o.type))
            {
                if (!VALUE_ARRAY_CLASS.isAssignableFrom(type))
                {
                    return -1;
                }
            }
            else
            {
                if (VALUE_ARRAY_CLASS.isAssignableFrom(type))
                {
                    return 1;
                }
            }
            return super.compareTo(o);
        }
    }

    // @class LIRIntrospection.OperandModeAnnotation
    protected static final class OperandModeAnnotation
    {
        /**
         * Number of non-array fields in {@link #values}.
         */
        public int directCount;
        public final ArrayList<ValueFieldInfo> values = new ArrayList<>();
    }

    // @class LIRIntrospection.LIRFieldsScanner
    protected abstract static class LIRFieldsScanner extends FieldsScanner
    {
        public final EconomicMap<Class<? extends Annotation>, OperandModeAnnotation> valueAnnotations;

        // @cons
        public LIRFieldsScanner(FieldsScanner.CalcOffset calc)
        {
            super(calc);
            valueAnnotations = EconomicMap.create(Equivalence.DEFAULT);
        }

        protected OperandModeAnnotation getOperandModeAnnotation(Field field)
        {
            OperandModeAnnotation result = null;
            MapCursor<Class<? extends Annotation>, OperandModeAnnotation> cursor = valueAnnotations.getEntries();
            while (cursor.advance())
            {
                Annotation annotation = field.getAnnotation(cursor.getKey());
                if (annotation != null)
                {
                    result = cursor.getValue();
                }
            }
            return result;
        }

        protected abstract EnumSet<OperandFlag> getFlags(Field field);

        @Override
        protected void scanField(Field field, long offset)
        {
            Class<?> type = field.getType();
            if (VALUE_CLASS.isAssignableFrom(type) && !CONSTANT_VALUE_CLASS.isAssignableFrom(type))
            {
                OperandModeAnnotation annotation = getOperandModeAnnotation(field);
                EnumSet<OperandFlag> flags = getFlags(field);
                annotation.values.add(new ValueFieldInfo(offset, field.getName(), type, field.getDeclaringClass(), flags));
                annotation.directCount++;
            }
            else if (VALUE_ARRAY_CLASS.isAssignableFrom(type))
            {
                OperandModeAnnotation annotation = getOperandModeAnnotation(field);
                EnumSet<OperandFlag> flags = getFlags(field);
                annotation.values.add(new ValueFieldInfo(offset, field.getName(), type, field.getDeclaringClass(), flags));
            }
            else
            {
                super.scanField(field, offset);
            }
        }
    }

    protected static void forEach(LIRInstruction inst, Values values, OperandMode mode, InstructionValueProcedure proc)
    {
        for (int i = 0; i < values.getCount(); i++)
        {
            if (i < values.getDirectCount())
            {
                Value value = values.getValue(inst, i);
                Value newValue;
                if (value instanceof CompositeValue)
                {
                    CompositeValue composite = (CompositeValue) value;
                    newValue = composite.forEachComponent(inst, mode, proc);
                }
                else
                {
                    newValue = proc.doValue(inst, value, mode, values.getFlags(i));
                }
                if (!value.identityEquals(newValue))
                {
                    values.setValue(inst, i, newValue);
                }
            }
            else
            {
                Value[] valueArray = values.getValueArray(inst, i);
                for (int j = 0; j < valueArray.length; j++)
                {
                    Value value = valueArray[j];
                    Value newValue;
                    if (value instanceof CompositeValue)
                    {
                        CompositeValue composite = (CompositeValue) value;
                        newValue = composite.forEachComponent(inst, mode, proc);
                    }
                    else
                    {
                        newValue = proc.doValue(inst, value, mode, values.getFlags(i));
                    }
                    if (!value.identityEquals(newValue))
                    {
                        valueArray[j] = newValue;
                    }
                }
            }
        }
    }

    protected static void visitEach(LIRInstruction inst, Values values, OperandMode mode, InstructionValueConsumer proc)
    {
        for (int i = 0; i < values.getCount(); i++)
        {
            if (i < values.getDirectCount())
            {
                Value value = values.getValue(inst, i);
                if (value instanceof CompositeValue)
                {
                    CompositeValue composite = (CompositeValue) value;
                    composite.visitEachComponent(inst, mode, proc);
                }
                else
                {
                    proc.visitValue(inst, value, mode, values.getFlags(i));
                }
            }
            else
            {
                Value[] valueArray = values.getValueArray(inst, i);
                for (int j = 0; j < valueArray.length; j++)
                {
                    Value value = valueArray[j];
                    if (value instanceof CompositeValue)
                    {
                        CompositeValue composite = (CompositeValue) value;
                        composite.visitEachComponent(inst, mode, proc);
                    }
                    else
                    {
                        proc.visitValue(inst, value, mode, values.getFlags(i));
                    }
                }
            }
        }
    }
}
