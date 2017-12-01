package cloiure.lang;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * conses onto rear, peeks/pops from front
 * See Okasaki's Batched Queues
 * This differs in that it uses a PersistentVector as the rear, which is in-order,
 * so no reversing or suspensions required for persistent use
 */

public class PersistentQueue extends Obj implements IPersistentList, Collection, Counted, IHashEq
{
    final public static PersistentQueue EMPTY = new PersistentQueue(null, 0, null, null);

    final int cnt;
    final ISeq f;
    final PersistentVector r;
 // static final int INITIAL_REAR_SIZE = 4;
    int _hash;
    int _hasheq;

    PersistentQueue(IPersistentMap meta, int cnt, ISeq f, PersistentVector r)
    {
        super(meta);
        this.cnt = cnt;
        this.f = f;
        this.r = r;
    }

    public boolean equiv(Object obj)
    {
        if (!(obj instanceof Sequential))
        {
            return false;
        }
        ISeq ms = RT.seq(obj);
        for (ISeq s = seq(); s != null; s = s.next(), ms = ms.next())
        {
            if (ms == null || !Util.equiv(s.first(), ms.first()))
            {
                return false;
            }
        }
        return (ms == null);
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Sequential))
        {
            return false;
        }
        ISeq ms = RT.seq(obj);
        for (ISeq s = seq(); s != null; s = s.next(), ms = ms.next())
        {
            if (ms == null || !Util.equals(s.first(), ms.first()))
            {
                return false;
            }
        }
        return (ms == null);
    }

    public int hashCode()
    {
        int hash = this._hash;
        if (hash == 0)
        {
            hash = 1;
            for (ISeq s = seq(); s != null; s = s.next())
            {
                hash = 31 * hash + ((s.first() == null) ? 0 : s.first().hashCode());
            }
            this._hash = hash;
        }
        return hash;
    }

    public int hasheq()
    {
        int cached = this._hasheq;
        if (cached == 0)
        {
         // int hash = 1;
         // for (ISeq s = seq(); s != null; s = s.next())
         // {
         //     hash = 31 * hash + Util.hasheq(s.first());
         // }
         // this._hasheq = hash;
            this._hasheq  = cached = Murmur3.hashOrdered(this);
        }
        return cached;
    }

    public Object peek()
    {
        return RT.first(f);
    }

    public PersistentQueue pop()
    {
        if (f == null)  // hmmm... pop of empty queue -> empty queue?
        {
            return this;
        }
         // throw new IllegalStateException("popping empty queue");
        ISeq f1 = f.next();
        PersistentVector r1 = r;
        if (f1 == null)
        {
            f1 = RT.seq(r);
            r1 = null;
        }
        return new PersistentQueue(meta(), cnt - 1, f1, r1);
    }

    public int count()
    {
        return cnt;
    }

    public ISeq seq()
    {
        if (f == null)
        {
            return null;
        }
        return new Seq(f, RT.seq(r));
    }

    public PersistentQueue cons(Object o)
    {
        if (f == null)     // empty
        {
            return new PersistentQueue(meta(), cnt + 1, RT.list(o), null);
        }
        else
        {
            return new PersistentQueue(meta(), cnt + 1, f, ((r != null) ? r : PersistentVector.EMPTY).cons(o));
        }
    }

    public IPersistentCollection empty()
    {
        return EMPTY.withMeta(meta());
    }

    public PersistentQueue withMeta(IPersistentMap meta)
    {
        return new PersistentQueue(meta, cnt, f, r);
    }

    static class Seq extends ASeq
    {
        final ISeq f;
        final ISeq rseq;

        Seq(ISeq f, ISeq rseq)
        {
            this.f = f;
            this.rseq = rseq;
        }

        Seq(IPersistentMap meta, ISeq f, ISeq rseq)
        {
            super(meta);
            this.f = f;
            this.rseq = rseq;
        }

        public Object first()
        {
            return f.first();
        }

        public ISeq next()
        {
            ISeq f1 = f.next();
            ISeq r1 = rseq;
            if (f1 == null)
            {
                if (rseq == null)
                {
                    return null;
                }
                f1 = rseq;
                r1 = null;
            }
            return new Seq(f1, r1);
        }

        public int count()
        {
            return RT.count(f) + RT.count(rseq);
        }

        public Seq withMeta(IPersistentMap meta)
        {
            return new Seq(meta, f, rseq);
        }
    }

    // java.util.Collection implementation

    public Object[] toArray()
    {
        return RT.seqToArray(seq());
    }

    public boolean add(Object o)
    {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection c)
    {
        for (Object o : c)
        {
            if (contains(o))
            {
                return true;
            }
        }
        return false;
    }

    public Object[] toArray(Object[] a)
    {
        return RT.seqToPassedArray(seq(), a);
    }

    public int size()
    {
        return count();
    }

    public boolean isEmpty()
    {
        return (count() == 0);
    }

    public boolean contains(Object o)
    {
        for (ISeq s = seq(); s != null; s = s.next())
        {
            if (Util.equiv(s.first(), o))
            {
                return true;
            }
        }
        return false;
    }

    public Iterator iterator()
    {
        return new Iterator()
        {
            private ISeq fseq = f;
            private final Iterator riter = (r != null) ? r.iterator() : null;

            public boolean hasNext()
            {
                return ((fseq != null && fseq.seq() != null) || (riter != null && riter.hasNext()));
            }

            public Object next()
            {
                if (fseq != null)
                {
                    Object ret = fseq.first();
                    fseq = fseq.next();
                    return ret;
                }
                else if (riter != null && riter.hasNext())
                {
                    return riter.next();
                }
                else
                {
                    throw new NoSuchElementException();
                }
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
