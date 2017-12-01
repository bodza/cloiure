package cloiure.lang;

import java.util.Map;

public final class MethodImplCache
{
    static public class Entry
    {
        final public Class c;
        final public IFn fn;

        public Entry(Class c, IFn fn)
        {
            this.c = c;
            this.fn = fn;
        }
    }

    public final IPersistentMap protocol;
    public final Keyword methodk;
    public final int shift;
    public final int mask;
    public final Object[] table;    // [class, entry. class, entry ...]
    public final Map map;

    Entry mre = null;

    public MethodImplCache(IPersistentMap protocol, Keyword methodk)
    {
        this(protocol, methodk, 0, 0, RT.EMPTY_ARRAY);
    }

    public MethodImplCache(IPersistentMap protocol, Keyword methodk, int shift, int mask, Object[] table)
    {
        this.protocol = protocol;
        this.methodk = methodk;
        this.shift = shift;
        this.mask = mask;
        this.table = table;
        this.map = null;
    }

    public MethodImplCache(IPersistentMap protocol, Keyword methodk, Map map)
    {
        this.protocol = protocol;
        this.methodk = methodk;
        this.shift = 0;
        this.mask = 0;
        this.table = null;
        this.map = map;
    }

    public IFn fnFor(Class c)
    {
        Entry last = mre;
        if (last != null && last.c == c)
        {
            return last.fn;
        }
        return findFnFor(c);
    }

    IFn findFnFor(Class c)
    {
        if (map != null)
        {
            Entry e = (Entry) map.get(c);
            mre = e;
            return (e != null) ? e.fn : null;
        }
        else
        {
            int idx = ((Util.hash(c) >> shift) & mask) << 1;
            if (idx < table.length && table[idx] == c)
            {
                Entry e = ((Entry) table[idx + 1]);
                mre = e;
                return (e != null) ? e.fn : null;
            }
            return null;
        }
    }
}
