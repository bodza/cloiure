package cloiure.lang;

public class StringSeq extends ASeq implements IndexedSeq
{
    public final CharSequence s;
    public final int i;

    static public StringSeq create(CharSequence s)
    {
        if (s.length() == 0)
            return null;
        return new StringSeq(null, s, 0);
    }

    StringSeq(IPersistentMap meta, CharSequence s, int i)
    {
        super(meta);
        this.s = s;
        this.i = i;
    }

    public Obj withMeta(IPersistentMap meta)
    {
        if (meta == meta())
            return this;
        return new StringSeq(meta, s, i);
    }

    public Object first()
    {
        return Character.valueOf(s.charAt(i));
    }

    public ISeq next()
    {
        if (i + 1 < s.length())
            return new StringSeq(_meta, s, i + 1);
        return null;
    }

    public int index()
    {
        return i;
    }

    public int count()
    {
        return s.length() - i;
    }
}
