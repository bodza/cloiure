package cloiure.lang;

final public class ChunkedCons extends ASeq implements IChunkedSeq
{
    final IChunk chunk;
    final ISeq _more;

    ChunkedCons(IPersistentMap meta, IChunk chunk, ISeq more)
    {
        super(meta);
        this.chunk = chunk;
        this._more = more;
    }

    public ChunkedCons(IChunk chunk, ISeq more)
    {
        this(null, chunk, more);
    }

    public Obj withMeta(IPersistentMap meta)
    {
        if (meta != _meta)
        {
            return new ChunkedCons(meta, chunk, _more);
        }
        return this;
    }

    public Object first()
    {
        return chunk.nth(0);
    }

    public ISeq next()
    {
        if (chunk.count() > 1)
        {
            return new ChunkedCons(chunk.dropFirst(), _more);
        }
        return chunkedNext();
    }

    public ISeq more()
    {
        if (chunk.count() > 1)
        {
            return new ChunkedCons(chunk.dropFirst(), _more);
        }
        if (_more == null)
        {
            return PersistentList.EMPTY;
        }
        return _more;
    }

    public IChunk chunkedFirst()
    {
        return chunk;
    }

    public ISeq chunkedNext()
    {
        return chunkedMore().seq();
    }

    public ISeq chunkedMore()
    {
        if (_more == null)
        {
            return PersistentList.EMPTY;
        }
        return _more;
    }
}
