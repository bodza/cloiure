package cloiure.lang;

public final class KeywordLookupSite implements ILookupSite, ILookupThunk
{
    final Keyword k;

    public KeywordLookupSite(Keyword k)
    {
        this.k = k;
    }

    public ILookupThunk fault(Object target)
    {
        if (target instanceof IKeywordLookup)
        {
            return install(target);
        }
        else if (target instanceof ILookup)
        {
            return ilookupThunk(target.getClass());
        }
        return this;
    }

    public Object get(Object target)
    {
        if (target instanceof IKeywordLookup || target instanceof ILookup)
            return this;
        return RT.get(target, k);
    }

    private ILookupThunk ilookupThunk(final Class c)
    {
        return new ILookupThunk()
        {
            public Object get(Object target)
            {
                if (target != null && target.getClass() == c)
                    return ((ILookup) target).valAt(k);
                return this;
            }
        };
    }

    private ILookupThunk install(Object target)
    {
        ILookupThunk t = ((IKeywordLookup)target).getLookupThunk(k);
        if (t != null)
            return t;
        return ilookupThunk(target.getClass());
    }
}
