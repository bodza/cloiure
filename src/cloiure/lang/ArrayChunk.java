package cloiure.lang;

import java.io.Serializable;

public final class ArrayChunk implements IChunk, Serializable
{
    final Object[] array;
    final int off;
    final int end;

    public ArrayChunk(Object[] array)
    {
        this(array, 0, array.length);
    }

    public ArrayChunk(Object[] array, int off)
    {
        this(array, off, array.length);
    }

    public ArrayChunk(Object[] array, int off, int end)
    {
        this.array = array;
        this.off = off;
        this.end = end;
    }

    public Object nth(int i)
    {
        return array[off + i];
    }

    public Object nth(int i, Object notFound)
    {
        if (i >= 0 && i < count())
            return nth(i);
        return notFound;
    }

    public int count()
    {
        return end - off;
    }

    public IChunk dropFirst()
    {
        if (off==end)
            throw new IllegalStateException("dropFirst of empty chunk");
        return new ArrayChunk(array, off + 1, end);
    }

    public Object reduce(IFn f, Object start)
    {
        Object ret = f.invoke(start, array[off]);
        if (RT.isReduced(ret))
            return ret;
        for (int x = off + 1; x < end; x++)
        {
            ret = f.invoke(ret, array[x]);
            if (RT.isReduced(ret))
                return ret;
        }
        return ret;
    }
}
