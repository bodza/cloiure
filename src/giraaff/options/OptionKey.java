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
     * Gets the value of this option in {@code values}.
     */
    public T getValue(OptionValues values)
    {
        return values.get(this);
    }
}
