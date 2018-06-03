package giraaff.core.common;

// @class FieldIntrospection
public abstract class FieldIntrospection<T>
{
    // @field
    private final Class<T> clazz;

    /**
     * The set of fields in {@link #clazz} that do long belong to a more specific category.
     */
    // @field
    protected Fields data;

    // @cons
    public FieldIntrospection(Class<T> __clazz)
    {
        super();
        this.clazz = __clazz;
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
