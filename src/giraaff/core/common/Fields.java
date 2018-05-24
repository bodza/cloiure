package giraaff.core.common;

import java.util.ArrayList;
import java.util.Collections;

import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;

/**
 * Describes fields in a class, primarily for access via {@link Unsafe}.
 */
public class Fields
{
    /**
     * Offsets used with {@link Unsafe} to access the fields.
     */
    protected final long[] offsets;

    /**
     * The names of the fields.
     */
    private final String[] names;

    /**
     * The types of the fields.
     */
    private final Class<?>[] types;

    private final Class<?>[] declaringClasses;

    public static Fields forClass(Class<?> clazz, Class<?> endClazz, boolean includeTransient, FieldsScanner.CalcOffset calcOffset)
    {
        FieldsScanner scanner = new FieldsScanner(calcOffset == null ? new FieldsScanner.DefaultCalcOffset() : calcOffset);
        scanner.scan(clazz, endClazz, includeTransient);
        return new Fields(scanner.data);
    }

    public Fields(ArrayList<? extends FieldsScanner.FieldInfo> fields)
    {
        Collections.sort(fields);
        this.offsets = new long[fields.size()];
        this.names = new String[offsets.length];
        this.types = new Class<?>[offsets.length];
        this.declaringClasses = new Class<?>[offsets.length];
        int index = 0;
        for (FieldsScanner.FieldInfo f : fields)
        {
            offsets[index] = f.offset;
            names[index] = f.name;
            types[index] = f.type;
            declaringClasses[index] = f.declaringClass;
            index++;
        }
    }

    /**
     * Gets the number of fields represented by this object.
     */
    public int getCount()
    {
        return offsets.length;
    }

    public static void translateInto(Fields fields, ArrayList<FieldsScanner.FieldInfo> infos)
    {
        for (int index = 0; index < fields.getCount(); index++)
        {
            infos.add(new FieldsScanner.FieldInfo(fields.offsets[index], fields.names[index], fields.types[index], fields.declaringClasses[index]));
        }
    }

    /**
     * Function enabling an object field value to be replaced with another value when being copied
     * within {@link Fields#copy(Object, Object, ObjectTransformer)}.
     */
    @FunctionalInterface
    public interface ObjectTransformer
    {
        Object apply(int index, Object from);
    }

    /**
     * Copies fields from {@code from} to {@code to}, both of which must be of the same type.
     *
     * @param from the object from which the fields should be copied
     * @param to the object to which the fields should be copied
     */
    public void copy(Object from, Object to)
    {
        copy(from, to, null);
    }

    /**
     * Copies fields from {@code from} to {@code to}, both of which must be of the same type.
     *
     * @param from the object from which the fields should be copied
     * @param to the object to which the fields should be copied
     * @param trans function to applied to object field values as they are copied. If {@code null},
     *            the value is copied unchanged.
     */
    public void copy(Object from, Object to, ObjectTransformer trans)
    {
        for (int index = 0; index < offsets.length; index++)
        {
            long offset = offsets[index];
            Class<?> type = types[index];
            if (type.isPrimitive())
            {
                if (type == Integer.TYPE)
                {
                    UnsafeAccess.UNSAFE.putInt(to, offset, UnsafeAccess.UNSAFE.getInt(from, offset));
                }
                else if (type == Long.TYPE)
                {
                    UnsafeAccess.UNSAFE.putLong(to, offset, UnsafeAccess.UNSAFE.getLong(from, offset));
                }
                else if (type == Boolean.TYPE)
                {
                    UnsafeAccess.UNSAFE.putBoolean(to, offset, UnsafeAccess.UNSAFE.getBoolean(from, offset));
                }
                else if (type == Float.TYPE)
                {
                    UnsafeAccess.UNSAFE.putFloat(to, offset, UnsafeAccess.UNSAFE.getFloat(from, offset));
                }
                else if (type == Double.TYPE)
                {
                    UnsafeAccess.UNSAFE.putDouble(to, offset, UnsafeAccess.UNSAFE.getDouble(from, offset));
                }
                else if (type == Short.TYPE)
                {
                    UnsafeAccess.UNSAFE.putShort(to, offset, UnsafeAccess.UNSAFE.getShort(from, offset));
                }
                else if (type == Character.TYPE)
                {
                    UnsafeAccess.UNSAFE.putChar(to, offset, UnsafeAccess.UNSAFE.getChar(from, offset));
                }
                else if (type == Byte.TYPE)
                {
                    UnsafeAccess.UNSAFE.putByte(to, offset, UnsafeAccess.UNSAFE.getByte(from, offset));
                }
            }
            else
            {
                Object obj = UnsafeAccess.UNSAFE.getObject(from, offset);
                UnsafeAccess.UNSAFE.putObject(to, offset, trans == null ? obj : trans.apply(index, obj));
            }
        }
    }

    /**
     * Gets the value of a field for a given object.
     *
     * @param object the object whose field is to be read
     * @param index the index of the field (between 0 and {@link #getCount()})
     * @return the value of the specified field which will be boxed if the field type is primitive
     */
    public Object get(Object object, int index)
    {
        long offset = offsets[index];
        Class<?> type = types[index];
        Object value = null;
        if (type.isPrimitive())
        {
            if (type == Integer.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getInt(object, offset);
            }
            else if (type == Long.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getLong(object, offset);
            }
            else if (type == Boolean.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getBoolean(object, offset);
            }
            else if (type == Float.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getFloat(object, offset);
            }
            else if (type == Double.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getDouble(object, offset);
            }
            else if (type == Short.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getShort(object, offset);
            }
            else if (type == Character.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getChar(object, offset);
            }
            else if (type == Byte.TYPE)
            {
                value = UnsafeAccess.UNSAFE.getByte(object, offset);
            }
        }
        else
        {
            value = UnsafeAccess.UNSAFE.getObject(object, offset);
        }
        return value;
    }

    /**
     * Gets the value of a field for a given object.
     *
     * @param object the object whose field is to be read
     * @param index the index of the field (between 0 and {@link #getCount()})
     * @return the value of the specified field which will be boxed if the field type is primitive
     */
    public long getRawPrimitive(Object object, int index)
    {
        long offset = offsets[index];
        Class<?> type = types[index];

        if (type == Integer.TYPE)
        {
            return UnsafeAccess.UNSAFE.getInt(object, offset);
        }
        else if (type == Long.TYPE)
        {
            return UnsafeAccess.UNSAFE.getLong(object, offset);
        }
        else if (type == Boolean.TYPE)
        {
            return UnsafeAccess.UNSAFE.getBoolean(object, offset) ? 1 : 0;
        }
        else if (type == Float.TYPE)
        {
            return Float.floatToRawIntBits(UnsafeAccess.UNSAFE.getFloat(object, offset));
        }
        else if (type == Double.TYPE)
        {
            return Double.doubleToRawLongBits(UnsafeAccess.UNSAFE.getDouble(object, offset));
        }
        else if (type == Short.TYPE)
        {
            return UnsafeAccess.UNSAFE.getShort(object, offset);
        }
        else if (type == Character.TYPE)
        {
            return UnsafeAccess.UNSAFE.getChar(object, offset);
        }
        else if (type == Byte.TYPE)
        {
            return UnsafeAccess.UNSAFE.getByte(object, offset);
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
    }

    /**
     * Determines if a field in the domain of this object is the same as the field denoted by the
     * same index in another {@link Fields} object.
     */
    public boolean isSame(Fields other, int index)
    {
        return other.offsets[index] == offsets[index];
    }

    public long[] getOffsets()
    {
        return offsets;
    }

    /**
     * Gets the name of a field.
     *
     * @param index index of a field
     */
    public String getName(int index)
    {
        return names[index];
    }

    /**
     * Gets the type of a field.
     *
     * @param index index of a field
     */
    public Class<?> getType(int index)
    {
        return types[index];
    }

    public Class<?> getDeclaringClass(int index)
    {
        return declaringClasses[index];
    }

    public void set(Object object, int index, Object value)
    {
        long offset = offsets[index];
        Class<?> type = types[index];
        if (type.isPrimitive())
        {
            if (type == Integer.TYPE)
            {
                UnsafeAccess.UNSAFE.putInt(object, offset, (Integer) value);
            }
            else if (type == Long.TYPE)
            {
                UnsafeAccess.UNSAFE.putLong(object, offset, (Long) value);
            }
            else if (type == Boolean.TYPE)
            {
                UnsafeAccess.UNSAFE.putBoolean(object, offset, (Boolean) value);
            }
            else if (type == Float.TYPE)
            {
                UnsafeAccess.UNSAFE.putFloat(object, offset, (Float) value);
            }
            else if (type == Double.TYPE)
            {
                UnsafeAccess.UNSAFE.putDouble(object, offset, (Double) value);
            }
            else if (type == Short.TYPE)
            {
                UnsafeAccess.UNSAFE.putShort(object, offset, (Short) value);
            }
            else if (type == Character.TYPE)
            {
                UnsafeAccess.UNSAFE.putChar(object, offset, (Character) value);
            }
            else if (type == Byte.TYPE)
            {
                UnsafeAccess.UNSAFE.putByte(object, offset, (Byte) value);
            }
        }
        else
        {
            UnsafeAccess.UNSAFE.putObject(object, offset, value);
        }
    }

    public void setRawPrimitive(Object object, int index, long value)
    {
        long offset = offsets[index];
        Class<?> type = types[index];
        if (type == Integer.TYPE)
        {
            UnsafeAccess.UNSAFE.putInt(object, offset, (int) value);
        }
        else if (type == Long.TYPE)
        {
            UnsafeAccess.UNSAFE.putLong(object, offset, value);
        }
        else if (type == Boolean.TYPE)
        {
            UnsafeAccess.UNSAFE.putBoolean(object, offset, value != 0);
        }
        else if (type == Float.TYPE)
        {
            UnsafeAccess.UNSAFE.putFloat(object, offset, Float.intBitsToFloat((int) value));
        }
        else if (type == Double.TYPE)
        {
            UnsafeAccess.UNSAFE.putDouble(object, offset, Double.longBitsToDouble(value));
        }
        else if (type == Short.TYPE)
        {
            UnsafeAccess.UNSAFE.putShort(object, offset, (short) value);
        }
        else if (type == Character.TYPE)
        {
            UnsafeAccess.UNSAFE.putChar(object, offset, (char) value);
        }
        else if (type == Byte.TYPE)
        {
            UnsafeAccess.UNSAFE.putByte(object, offset, (byte) value);
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
        appendFields(sb);
        return sb.append(']').toString();
    }

    public void appendFields(StringBuilder sb)
    {
        for (int i = 0; i < offsets.length; i++)
        {
            sb.append(i == 0 ? "" : ", ").append(getName(i)).append('@').append(offsets[i]);
        }
    }

    public boolean getBoolean(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getBoolean(n, offsets[i]);
    }

    public byte getByte(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getByte(n, offsets[i]);
    }

    public short getShort(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getShort(n, offsets[i]);
    }

    public char getChar(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getChar(n, offsets[i]);
    }

    public int getInt(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getInt(n, offsets[i]);
    }

    public long getLong(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getLong(n, offsets[i]);
    }

    public float getFloat(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getFloat(n, offsets[i]);
    }

    public double getDouble(Object n, int i)
    {
        return UnsafeAccess.UNSAFE.getDouble(n, offsets[i]);
    }

    public Object getObject(Object object, int i)
    {
        return UnsafeAccess.UNSAFE.getObject(object, offsets[i]);
    }

    public void putObject(Object object, int i, Object value)
    {
        UnsafeAccess.UNSAFE.putObject(object, offsets[i], value);
    }
}
