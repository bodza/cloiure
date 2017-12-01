package cloiure.lang;

import java.util.Comparator;

public class PersistentTreeSet extends APersistentSet implements IObj, Reversible, Sorted
{
    static public final PersistentTreeSet EMPTY = new PersistentTreeSet(null, PersistentTreeMap.EMPTY);

    final IPersistentMap _meta;

    static public PersistentTreeSet create(ISeq items)
    {
        PersistentTreeSet ret = EMPTY;
        for ( ; items != null; items = items.next())
        {
            ret = (PersistentTreeSet) ret.cons(items.first());
        }
        return ret;
    }

    static public PersistentTreeSet create(Comparator comp, ISeq items)
    {
        PersistentTreeSet ret = new PersistentTreeSet(null, new PersistentTreeMap(null, comp));
        for ( ; items != null; items = items.next())
        {
            ret = (PersistentTreeSet) ret.cons(items.first());
        }
        return ret;
    }

    PersistentTreeSet(IPersistentMap meta, IPersistentMap impl)
    {
        super(impl);
        this._meta = meta;
    }

    public boolean equals(Object obj)
    {
        try
        {
            return super.equals(obj);
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }

    public boolean equiv(Object obj)
    {
        try
        {
            return super.equiv(obj);
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }

    public IPersistentSet disjoin(Object key)
    {
        if (contains(key))
        {
            return new PersistentTreeSet(meta(), impl.without(key));
        }
        return this;
    }

    public IPersistentSet cons(Object o)
    {
        if (contains(o))
        {
            return this;
        }
        return new PersistentTreeSet(meta(), impl.assoc(o, o));
    }

    public IPersistentCollection empty()
    {
        return new PersistentTreeSet(meta(), (PersistentTreeMap)impl.empty());
    }

    public ISeq rseq()
    {
        return APersistentMap.KeySeq.create(((Reversible) impl).rseq());
    }

    public PersistentTreeSet withMeta(IPersistentMap meta)
    {
        return new PersistentTreeSet(meta, impl);
    }

    public Comparator comparator()
    {
        return ((Sorted)impl).comparator();
    }

    public Object entryKey(Object entry)
    {
        return entry;
    }

    public ISeq seq(boolean ascending)
    {
        PersistentTreeMap m = (PersistentTreeMap) impl;
        return RT.keys(m.seq(ascending));
    }

    public ISeq seqFrom(Object key, boolean ascending)
    {
        PersistentTreeMap m = (PersistentTreeMap) impl;
        return RT.keys(m.seqFrom(key, ascending));
    }

    public IPersistentMap meta()
    {
        return _meta;
    }
}
