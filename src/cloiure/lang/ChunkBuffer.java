package cloiure.lang;

final public class ChunkBuffer implements Counted
{
    Object[] buffer;
    int end;

    public ChunkBuffer(int capacity)
    {
        buffer = new Object[capacity];
        end = 0;
    }

    public void add(Object o)
    {
        buffer[end++] = o;
    }

    public IChunk chunk()
    {
        ArrayChunk ret = new ArrayChunk(buffer, 0, end);
        buffer = null;
        return ret;
    }

    public int count()
    {
        return end;
    }
}
