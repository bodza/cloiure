package giraaff.core.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import giraaff.util.UnsafeAccess;

/**
 * Scans the fields in a class hierarchy.
 */
// @class FieldsScanner
public class FieldsScanner
{
    /**
     * Determines the offset (in bytes) of a field.
     */
    // @iface FieldsScanner.CalcOffset
    public interface CalcOffset
    {
        long getOffset(Field field);
    }

    /**
     * Determines the offset (in bytes) of a field using {@link Unsafe#objectFieldOffset(Field)}.
     */
    // @class FieldsScanner.DefaultCalcOffset
    public static final class DefaultCalcOffset implements CalcOffset
    {
        @Override
        public long getOffset(Field __field)
        {
            return UnsafeAccess.UNSAFE.objectFieldOffset(__field);
        }
    }

    /**
     * Describes a field in a class during {@linkplain FieldsScanner scanning}.
     */
    // @class FieldsScanner.FieldInfo
    public static class FieldInfo implements Comparable<FieldInfo>
    {
        // @field
        public final long offset;
        // @field
        public final String name;
        // @field
        public final Class<?> type;
        // @field
        public final Class<?> declaringClass;

        // @cons
        public FieldInfo(long __offset, String __name, Class<?> __type, Class<?> __declaringClass)
        {
            super();
            this.offset = __offset;
            this.name = __name;
            this.type = __type;
            this.declaringClass = __declaringClass;
        }

        /**
         * Sorts fields in ascending order by their {@link #offset}s.
         */
        @Override
        public int compareTo(FieldInfo __o)
        {
            return offset < __o.offset ? -1 : (offset > __o.offset ? 1 : 0);
        }
    }

    // @field
    private final FieldsScanner.CalcOffset calc;

    /**
     * Fields not belonging to a more specific category defined by scanner subclasses are added to
     * this list.
     */
    // @field
    public final ArrayList<FieldsScanner.FieldInfo> data = new ArrayList<>();

    // @cons
    public FieldsScanner(FieldsScanner.CalcOffset __calc)
    {
        super();
        this.calc = __calc;
    }

    /**
     * Scans the fields in a class hierarchy.
     *
     * @param clazz the class at which to start scanning
     * @param endClazz scanning stops when this class is encountered (i.e. {@code endClazz} is not scanned)
     */
    public void scan(Class<?> __clazz, Class<?> __endClazz, boolean __includeTransient)
    {
        Class<?> __currentClazz = __clazz;
        while (__currentClazz != __endClazz)
        {
            for (Field __field : __currentClazz.getDeclaredFields())
            {
                if (Modifier.isStatic(__field.getModifiers()))
                {
                    continue;
                }
                if (!__includeTransient && Modifier.isTransient(__field.getModifiers()))
                {
                    continue;
                }
                long __offset = calc.getOffset(__field);
                scanField(__field, __offset);
            }
            __currentClazz = __currentClazz.getSuperclass();
        }
    }

    protected void scanField(Field __field, long __offset)
    {
        data.add(new FieldsScanner.FieldInfo(__offset, __field.getName(), __field.getType(), __field.getDeclaringClass()));
    }
}
