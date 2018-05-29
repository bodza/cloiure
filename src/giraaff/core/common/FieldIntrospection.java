package giraaff.core.common;

// @class FieldIntrospection
public abstract class FieldIntrospection<T>
{
    private final Class<T> clazz;

    /**
     * The set of fields in {@link #clazz} that do long belong to a more specific category.
     */
    protected Fields data;

    // @cons
    public FieldIntrospection(Class<T> clazz)
    {
        super();
        this.clazz = clazz;
    }

    public Class<T> getClazz()
    {
        return clazz;
    }

    /**
     * Gets the fields in {@link #getClazz()} that do long belong to specific category.
     */
    public Fields getData()
    {
        return data;
    }

    public abstract Fields[] getAllFields();
}
