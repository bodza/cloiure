package cloiure.lang;

public class Repeat extends ASeq implements IReduce
{
    private static final long INFINITE = -1;

    private final long count;  // always INFINITE or >0
    private final Object val;
    private volatile ISeq _next;  // cached

    private Repeat(long count, Object val)
    {
        this.count = count;
        this.val = val;
    }

    private Repeat(IPersistentMap meta, long count, Object val)
    {
        super(meta);
        this.count = count;
        this.val = val;
    }

    public static Repeat create(Object val)
    {
        return new Repeat(INFINITE, val);
    }

    public static ISeq create(long count, Object val)
    {
        if (count <= 0)
            return PersistentList.EMPTY;
        return new Repeat(count, val);
    }

    public Object first()
    {
        return val;
    }

    public ISeq next()
    {
        if (_next == null)
        {
            if (count > 1)
                _next = new Repeat(count - 1, val);
            else if (count == INFINITE)
                _next = this;
        }
        return _next;
    }

    public Repeat withMeta(IPersistentMap meta)
    {
        return new Repeat(meta, count, val);
    }

    public Object reduce(IFn f)
    {
        Object ret = val;
        if (count == INFINITE)
        {
            while (true)
            {
                ret = f.invoke(ret, val);
                if (RT.isReduced(ret))
                    return ((IDeref)ret).deref();
            }
        }
        else
        {
            for (long i = 1; i < count; i++)
            {
                ret = f.invoke(ret, val);
                if (RT.isReduced(ret))
                    return ((IDeref)ret).deref();
            }
            return ret;
        }
    }

    public Object reduce(IFn f, Object start)
    {
        Object ret = start;
        if (count == INFINITE)
        {
            while (true)
            {
                ret = f.invoke(ret, val);
                if (RT.isReduced(ret))
                    return ((IDeref)ret).deref();
            }
        }
        else
        {
            for (long i = 0; i < count; i++)
            {
                ret = f.invoke(ret, val);
                if (RT.isReduced(ret))
                    return ((IDeref)ret).deref();
            }
            return ret;
        }
    }
}
