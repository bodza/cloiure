package cloiure.lang;

public abstract class ATransientSet extends AFn implements ITransientSet
{
    volatile ITransientMap impl;

    ATransientSet(ITransientMap impl)
    {
        this.impl = impl;
    }

    public int count()
    {
        return impl.count();
    }

    public ITransientSet conj(Object val)
    {
        ITransientMap m = impl.assoc(val, val);
        if (m != impl)
            this.impl = m;
        return this;
    }

    public boolean contains(Object key)
    {
        return this != impl.valAt(key, this);
    }

    public ITransientSet disjoin(Object key)
    {
        ITransientMap m = impl.without(key);
        if (m != impl)
            this.impl = m;
        return this;
    }

    public Object get(Object key)
    {
        return impl.valAt(key);
    }

    public Object invoke(Object key, Object notFound)
    {
        return impl.valAt(key, notFound);
    }

    public Object invoke(Object key)
    {
        return impl.valAt(key);
    }
}
