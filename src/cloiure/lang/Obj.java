package cloiure.lang;

import java.io.Serializable;

public abstract class Obj implements IObj, Serializable
{
    final IPersistentMap _meta;

    public Obj(IPersistentMap meta)
    {
        this._meta = meta;
    }

    public Obj()
    {
        _meta = null;
    }

    final public IPersistentMap meta()
    {
        return _meta;
    }

    abstract public Obj withMeta(IPersistentMap meta);
}
