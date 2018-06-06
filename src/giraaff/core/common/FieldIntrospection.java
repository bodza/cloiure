package giraaff.core.common;

// @class FieldIntrospection
public abstract class FieldIntrospection<T>
{
    // @field
    private final Class<T> ___clazz;

    ///
    // The set of fields in {@link #clazz} that do long belong to a more specific category.
    ///
    // @field
    protected Fields ___data;

    // @cons FieldIntrospection
    public FieldIntrospection(Class<T> __clazz)
    {
        super();
        this.___clazz = __clazz;
    }

    public Class<T> getClazz()
    {
        return this.___clazz;
    }

    ///
    // Gets the fields in {@link #getClazz()} that do long belong to specific category.
    ///
    public Fields getData()
    {
        return this.___data;
    }

    public abstract Fields[] getAllFields();
}
