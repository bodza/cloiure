package giraaff.options;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import org.graalvm.collections.UnmodifiableEconomicMap;
import org.graalvm.collections.UnmodifiableMapCursor;

/**
 * A context for obtaining values for {@link OptionKey}s.
 */
public class OptionValues
{
    private final UnmodifiableEconomicMap<OptionKey<?>, Object> values;

    protected boolean containsKey(OptionKey<?> key)
    {
        return values.containsKey(key);
    }

    public OptionValues(OptionValues initialValues, UnmodifiableEconomicMap<OptionKey<?>, Object> extraPairs)
    {
        EconomicMap<OptionKey<?>, Object> map = newOptionMap();
        if (initialValues != null)
        {
            map.putAll(initialValues.getMap());
        }
        initMap(map, extraPairs);
        this.values = map;
    }

    public OptionValues(OptionValues initialValues, OptionKey<?> key1, Object value1, Object... extraPairs)
    {
        this(initialValues, asMap(key1, value1, extraPairs));
    }

    /**
     * Creates a new map suitable for using {@link OptionKey}s as keys.
     */
    public static EconomicMap<OptionKey<?>, Object> newOptionMap()
    {
        return EconomicMap.create(Equivalence.IDENTITY);
    }

    /**
     * Gets an immutable view of the key/value pairs in this object. Values read from this view
     * should be {@linkplain #decodeNull(Object) decoded} before being used.
     */
    public UnmodifiableEconomicMap<OptionKey<?>, Object> getMap()
    {
        return values;
    }

    /**
     * @param key1 first key in map
     * @param value1 first value in map
     * @param extraPairs key/value pairs of the form {@code [key1, value1, key2, value2, ...]}
     * @return a map containing the key/value pairs as entries
     */
    public static EconomicMap<OptionKey<?>, Object> asMap(OptionKey<?> key1, Object value1, Object... extraPairs)
    {
        EconomicMap<OptionKey<?>, Object> map = newOptionMap();
        map.put(key1, value1);
        for (int i = 0; i < extraPairs.length; i += 2)
        {
            OptionKey<?> key = (OptionKey<?>) extraPairs[i];
            Object value = extraPairs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    public OptionValues(UnmodifiableEconomicMap<OptionKey<?>, Object> values)
    {
        EconomicMap<OptionKey<?>, Object> map = newOptionMap();
        initMap(map, values);
        this.values = map;
    }

    protected static void initMap(EconomicMap<OptionKey<?>, Object> map, UnmodifiableEconomicMap<OptionKey<?>, Object> values)
    {
        UnmodifiableMapCursor<OptionKey<?>, Object> cursor = values.getEntries();
        while (cursor.advance())
        {
            map.put(cursor.getKey(), encodeNull(cursor.getValue()));
        }
    }

    protected <T> T get(OptionKey<T> key)
    {
        return get(values, key);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T get(UnmodifiableEconomicMap<OptionKey<?>, Object> values, OptionKey<T> key)
    {
        Object value = values.get(key);
        if (value == null)
        {
            return key.getDefaultValue();
        }
        return (T) decodeNull(value);
    }

    private static final Object NULL = new Object();

    protected static Object encodeNull(Object value)
    {
        return value == null ? NULL : value;
    }

    /**
     * Decodes a value that may be the sentinel value for {@code null} in a map.
     */
    public static Object decodeNull(Object value)
    {
        return value == NULL ? null : value;
    }

    @Override
    public String toString()
    {
        return toString(getMap());
    }

    public static String toString(UnmodifiableEconomicMap<OptionKey<?>, Object> values)
    {
        Comparator<OptionKey<?>> comparator = new Comparator<OptionKey<?>>()
        {
            @Override
            public int compare(OptionKey<?> o1, OptionKey<?> o2)
            {
                return o1.toString().compareTo(o2.toString());
            }
        };
        SortedMap<OptionKey<?>, Object> sorted = new TreeMap<>(comparator);
        UnmodifiableMapCursor<OptionKey<?>, Object> cursor = values.getEntries();
        while (cursor.advance())
        {
            sorted.put(cursor.getKey(), decodeNull(cursor.getValue()));
        }
        return sorted.toString();
    }
}
