package graalvm.compiler.options;

import java.util.EnumSet;

import org.graalvm.collections.EconomicMap;

public class EnumOptionKey<T extends Enum<T>> extends OptionKey<T>
{
    final Class<T> enumClass;

    @SuppressWarnings("unchecked")
    public EnumOptionKey(T value)
    {
        super(value);
        if (value == null)
        {
            throw new IllegalArgumentException("Value must not be null");
        }
        this.enumClass = (Class<T>) value.getClass();
    }

    /**
     * @return the set of possible values for this option.
     */
    public EnumSet<T> getAllValues()
    {
        return EnumSet.allOf(enumClass);
    }

    public Object valueOf(String name)
    {
        try
        {
            return Enum.valueOf(enumClass, name);
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("\"" + name + "\" is not a valid option for " + getName() + ". Valid values are " + getAllValues());
        }
    }

    @Override
    protected void onValueUpdate(EconomicMap<OptionKey<?>, Object> values, T oldValue, T newValue)
    {
        assert enumClass.isInstance(newValue) : newValue + " is not a valid value for " + getName();
    }
}
