package giraaff.core.common;

import java.util.ArrayList;
import java.util.Collections;

import giraaff.util.GraalError;
import giraaff.util.UnsafeAccess;

/**
 * Describes fields in a class, primarily for access via {@link Unsafe}.
 */
// @class Fields
public class Fields
{
    /**
     * Offsets used with {@link Unsafe} to access the fields.
     */
    // @field
    protected final long[] offsets;

    /**
     * The names of the fields.
     */
    // @field
    private final String[] names;

    /**
     * The types of the fields.
     */
    // @field
    private final Class<?>[] types;

    // @field
    private final Class<?>[] declaringClasses;

    public static Fields forClass(Class<?> __clazz, Class<?> __endClazz, boolean __includeTransient, FieldsScanner.CalcOffset __calcOffset)
    {
        FieldsScanner __scanner = new FieldsScanner(__calcOffset == null ? new FieldsScanner.DefaultCalcOffset() : __calcOffset);
        __scanner.scan(__clazz, __endClazz, __includeTransient);
        return new Fields(__scanner.data);
    }

    // @cons
    public Fields(ArrayList<? extends FieldsScanner.FieldInfo> __fields)
    {
        super();
        Collections.sort(__fields);
        this.offsets = new long[__fields.size()];
        this.names = new String[offsets.length];
        this.types = new Class<?>[offsets.length];
        this.declaringClasses = new Class<?>[offsets.length];
        int __index = 0;
        for (FieldsScanner.FieldInfo __f : __fields)
        {
            offsets[__index] = __f.offset;
            names[__index] = __f.name;
            types[__index] = __f.type;
            declaringClasses[__index] = __f.declaringClass;
            __index++;
        }
    }

    /**
     * Gets the number of fields represented by this object.
     */
    public int getCount()
    {
        return offsets.length;
    }

    public static void translateInto(Fields __fields, ArrayList<FieldsScanner.FieldInfo> __infos)
    {
        for (int __index = 0; __index < __fields.getCount(); __index++)
        {
            __infos.add(new FieldsScanner.FieldInfo(__fields.offsets[__index], __fields.names[__index], __fields.types[__index], __fields.declaringClasses[__index]));
        }
    }

    /**
     * Function enabling an object field value to be replaced with another value when being copied
     * within {@link Fields#copy(Object, Object, ObjectTransformer)}.
     */
    @FunctionalInterface
    // @iface Fields.ObjectTransformer
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
    public void copy(Object __from, Object __to)
    {
        copy(__from, __to, null);
    }

    /**
     * Copies fields from {@code from} to {@code to}, both of which must be of the same type.
     *
     * @param from the object from which the fields should be copied
     * @param to the object to which the fields should be copied
     * @param trans function to applied to object field values as they are copied. If {@code null},
     *            the value is copied unchanged.
     */
    public void copy(Object __from, Object __to, ObjectTransformer __trans)
    {
        for (int __index = 0; __index < offsets.length; __index++)
        {
            long __offset = offsets[__index];
            Class<?> __type = types[__index];
            if (__type.isPrimitive())
            {
                if (__type == Integer.TYPE)
                {
                    UnsafeAccess.UNSAFE.putInt(__to, __offset, UnsafeAccess.UNSAFE.getInt(__from, __offset));
                }
                else if (__type == Long.TYPE)
                {
                    UnsafeAccess.UNSAFE.putLong(__to, __offset, UnsafeAccess.UNSAFE.getLong(__from, __offset));
                }
                else if (__type == Boolean.TYPE)
                {
                    UnsafeAccess.UNSAFE.putBoolean(__to, __offset, UnsafeAccess.UNSAFE.getBoolean(__from, __offset));
                }
                else if (__type == Float.TYPE)
                {
                    UnsafeAccess.UNSAFE.putFloat(__to, __offset, UnsafeAccess.UNSAFE.getFloat(__from, __offset));
                }
                else if (__type == Double.TYPE)
                {
                    UnsafeAccess.UNSAFE.putDouble(__to, __offset, UnsafeAccess.UNSAFE.getDouble(__from, __offset));
                }
                else if (__type == Short.TYPE)
                {
                    UnsafeAccess.UNSAFE.putShort(__to, __offset, UnsafeAccess.UNSAFE.getShort(__from, __offset));
                }
                else if (__type == Character.TYPE)
                {
                    UnsafeAccess.UNSAFE.putChar(__to, __offset, UnsafeAccess.UNSAFE.getChar(__from, __offset));
                }
                else if (__type == Byte.TYPE)
                {
                    UnsafeAccess.UNSAFE.putByte(__to, __offset, UnsafeAccess.UNSAFE.getByte(__from, __offset));
                }
            }
            else
            {
                Object __obj = UnsafeAccess.UNSAFE.getObject(__from, __offset);
                UnsafeAccess.UNSAFE.putObject(__to, __offset, __trans == null ? __obj : __trans.apply(__index, __obj));
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
    public Object get(Object __object, int __index)
    {
        long __offset = offsets[__index];
        Class<?> __type = types[__index];
        Object __value = null;
        if (__type.isPrimitive())
        {
            if (__type == Integer.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getInt(__object, __offset);
            }
            else if (__type == Long.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getLong(__object, __offset);
            }
            else if (__type == Boolean.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getBoolean(__object, __offset);
            }
            else if (__type == Float.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getFloat(__object, __offset);
            }
            else if (__type == Double.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getDouble(__object, __offset);
            }
            else if (__type == Short.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getShort(__object, __offset);
            }
            else if (__type == Character.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getChar(__object, __offset);
            }
            else if (__type == Byte.TYPE)
            {
                __value = UnsafeAccess.UNSAFE.getByte(__object, __offset);
            }
        }
        else
        {
            __value = UnsafeAccess.UNSAFE.getObject(__object, __offset);
        }
        return __value;
    }

    /**
     * Gets the value of a field for a given object.
     *
     * @param object the object whose field is to be read
     * @param index the index of the field (between 0 and {@link #getCount()})
     * @return the value of the specified field which will be boxed if the field type is primitive
     */
    public long getRawPrimitive(Object __object, int __index)
    {
        long __offset = offsets[__index];
        Class<?> __type = types[__index];

        if (__type == Integer.TYPE)
        {
            return UnsafeAccess.UNSAFE.getInt(__object, __offset);
        }
        else if (__type == Long.TYPE)
        {
            return UnsafeAccess.UNSAFE.getLong(__object, __offset);
        }
        else if (__type == Boolean.TYPE)
        {
            return UnsafeAccess.UNSAFE.getBoolean(__object, __offset) ? 1 : 0;
        }
        else if (__type == Float.TYPE)
        {
            return Float.floatToRawIntBits(UnsafeAccess.UNSAFE.getFloat(__object, __offset));
        }
        else if (__type == Double.TYPE)
        {
            return Double.doubleToRawLongBits(UnsafeAccess.UNSAFE.getDouble(__object, __offset));
        }
        else if (__type == Short.TYPE)
        {
            return UnsafeAccess.UNSAFE.getShort(__object, __offset);
        }
        else if (__type == Character.TYPE)
        {
            return UnsafeAccess.UNSAFE.getChar(__object, __offset);
        }
        else if (__type == Byte.TYPE)
        {
            return UnsafeAccess.UNSAFE.getByte(__object, __offset);
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
    public boolean isSame(Fields __other, int __index)
    {
        return __other.offsets[__index] == offsets[__index];
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
    public String getName(int __index)
    {
        return names[__index];
    }

    /**
     * Gets the type of a field.
     *
     * @param index index of a field
     */
    public Class<?> getType(int __index)
    {
        return types[__index];
    }

    public Class<?> getDeclaringClass(int __index)
    {
        return declaringClasses[__index];
    }

    public void set(Object __object, int __index, Object __value)
    {
        long __offset = offsets[__index];
        Class<?> __type = types[__index];
        if (__type.isPrimitive())
        {
            if (__type == Integer.TYPE)
            {
                UnsafeAccess.UNSAFE.putInt(__object, __offset, (Integer) __value);
            }
            else if (__type == Long.TYPE)
            {
                UnsafeAccess.UNSAFE.putLong(__object, __offset, (Long) __value);
            }
            else if (__type == Boolean.TYPE)
            {
                UnsafeAccess.UNSAFE.putBoolean(__object, __offset, (Boolean) __value);
            }
            else if (__type == Float.TYPE)
            {
                UnsafeAccess.UNSAFE.putFloat(__object, __offset, (Float) __value);
            }
            else if (__type == Double.TYPE)
            {
                UnsafeAccess.UNSAFE.putDouble(__object, __offset, (Double) __value);
            }
            else if (__type == Short.TYPE)
            {
                UnsafeAccess.UNSAFE.putShort(__object, __offset, (Short) __value);
            }
            else if (__type == Character.TYPE)
            {
                UnsafeAccess.UNSAFE.putChar(__object, __offset, (Character) __value);
            }
            else if (__type == Byte.TYPE)
            {
                UnsafeAccess.UNSAFE.putByte(__object, __offset, (Byte) __value);
            }
        }
        else
        {
            UnsafeAccess.UNSAFE.putObject(__object, __offset, __value);
        }
    }

    public void setRawPrimitive(Object __object, int __index, long __value)
    {
        long __offset = offsets[__index];
        Class<?> __type = types[__index];
        if (__type == Integer.TYPE)
        {
            UnsafeAccess.UNSAFE.putInt(__object, __offset, (int) __value);
        }
        else if (__type == Long.TYPE)
        {
            UnsafeAccess.UNSAFE.putLong(__object, __offset, __value);
        }
        else if (__type == Boolean.TYPE)
        {
            UnsafeAccess.UNSAFE.putBoolean(__object, __offset, __value != 0);
        }
        else if (__type == Float.TYPE)
        {
            UnsafeAccess.UNSAFE.putFloat(__object, __offset, Float.intBitsToFloat((int) __value));
        }
        else if (__type == Double.TYPE)
        {
            UnsafeAccess.UNSAFE.putDouble(__object, __offset, Double.longBitsToDouble(__value));
        }
        else if (__type == Short.TYPE)
        {
            UnsafeAccess.UNSAFE.putShort(__object, __offset, (short) __value);
        }
        else if (__type == Character.TYPE)
        {
            UnsafeAccess.UNSAFE.putChar(__object, __offset, (char) __value);
        }
        else if (__type == Byte.TYPE)
        {
            UnsafeAccess.UNSAFE.putByte(__object, __offset, (byte) __value);
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
    }

    public boolean getBoolean(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getBoolean(__n, offsets[__i]);
    }

    public byte getByte(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getByte(__n, offsets[__i]);
    }

    public short getShort(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getShort(__n, offsets[__i]);
    }

    public char getChar(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getChar(__n, offsets[__i]);
    }

    public int getInt(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getInt(__n, offsets[__i]);
    }

    public long getLong(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getLong(__n, offsets[__i]);
    }

    public float getFloat(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getFloat(__n, offsets[__i]);
    }

    public double getDouble(Object __n, int __i)
    {
        return UnsafeAccess.UNSAFE.getDouble(__n, offsets[__i]);
    }

    public Object getObject(Object __object, int __i)
    {
        return UnsafeAccess.UNSAFE.getObject(__object, offsets[__i]);
    }

    public void putObject(Object __object, int __i, Object __value)
    {
        UnsafeAccess.UNSAFE.putObject(__object, offsets[__i], __value);
    }
}
