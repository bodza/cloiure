package giraaff.options;

/**
 * A key for an option. The value for an option is obtained from an {@link OptionValues} object.
 */
// @class OptionKey
public class OptionKey<T>
{
    private final T defaultValue;

    // @cons
    public OptionKey(T defaultValue)
    {
        super();
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
}
