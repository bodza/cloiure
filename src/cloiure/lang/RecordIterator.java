package cloiure.lang;

import java.util.Iterator;

public final class RecordIterator implements Iterator
{
    int i = 0;
    final int basecnt;
    final ILookup rec;
    final IPersistentVector basefields;
    final Iterator extmap;

    public RecordIterator (ILookup rec, IPersistentVector basefields, Iterator extmap)
    {
        this.rec = rec;
        this.basefields = basefields;
        this.basecnt = basefields.count();
        this.extmap = extmap;
    }

    public boolean hasNext()
    {
        if (i < basecnt)
        {
            return true;
        }
        else
        {
            return extmap.hasNext();
        }
    }

    public Object next()
    {
        if (i < basecnt)
        {
            Object k = basefields.nth(i);
            i++;
            return MapEntry.create(k, rec.valAt(k));
        }
        else
        {
            return extmap.next();
        }
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
