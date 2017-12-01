package cloiure.lang;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SeqIterator implements Iterator
{
    static final Object START = new Object();

    Object seq;
    Object next;

    public SeqIterator(Object o)
    {
        seq = START;
        next = o;
    }

    // preserved for binary compatibility
    public SeqIterator(ISeq o)
    {
        seq = START;
        next = o;
    }

    public boolean hasNext()
    {
        if (seq == START)
        {
            seq = null;
            next = RT.seq(next);
        }
        else if (seq == next)
        {
            next = RT.next(seq);
        }
        return (next != null);
    }

    public Object next() throws NoSuchElementException
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        seq = next;
        return RT.first(next);
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
