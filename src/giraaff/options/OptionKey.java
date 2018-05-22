package giraaff.options;

import org.graalvm.collections.EconomicMap;

/**
 * A key for an option. The value for an option is obtained from an {@link OptionValues} object.
 */
public class OptionKey<T>
{
    private final T defaultValue;

    public OptionKey(T defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * The initial value specified in source code.
     */
    public final T getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Returns true if the option has been set in any way. Note that this doesn't mean that the
     * current value is different than the default.
     */
    public boolean hasBeenSet(OptionValues values)
    {
        return values.containsKey(this);
    }

    /**
     * Gets the value of this option in {@code values}.
     */
    public T getValue(OptionValues values)
    {
        return values.get(this);
    }

    /**
     * Sets the value of this option in a given map. The
     * {@link #onValueUpdate(EconomicMap, Object, Object)} method is called once the value is set.
     *
     * @param values map of option values
     * @param v the value to set for this key in {@code map}
     */
    @SuppressWarnings("unchecked")
    public void update(EconomicMap<OptionKey<?>, Object> values, Object v)
    {
        T oldValue = (T) values.put(this, v);
        onValueUpdate(values, oldValue, (T) v);
    }

    /**
     * Sets the value of this option in a given map if it doesn't already have a value. The
     * {@link #onValueUpdate(EconomicMap, Object, Object)} method is called once the value is set.
     *
     * @param values map of option values
     * @param v the value to set for this key in {@code map}
     */
    @SuppressWarnings("unchecked")
    public void putIfAbsent(EconomicMap<OptionKey<?>, Object> values, Object v)
    {
        if (!values.containsKey(this))
        {
            T oldValue = (T) values.put(this, v);
            onValueUpdate(values, oldValue, (T) v);
        }
    }

    /**
     * Notifies this object when a value associated with this key is set or updated in {@code values}.
     */
    protected void onValueUpdate(EconomicMap<OptionKey<?>, Object> values, T oldValue, T newValue)
    {
    }
}
