package cloiure.lang;

public class AReference implements IReference
{
    private IPersistentMap _meta;

    public AReference()
    {
        this(null);
    }

    public AReference(IPersistentMap meta)
    {
        _meta = meta;
    }

    synchronized public IPersistentMap meta()
    {
        return _meta;
    }

    synchronized public IPersistentMap alterMeta(IFn alter, ISeq args)
    {
        _meta = (IPersistentMap) alter.applyTo(new Cons(_meta, args));
        return _meta;
    }

    synchronized public IPersistentMap resetMeta(IPersistentMap m)
    {
        _meta = m;
        return m;
    }
}
