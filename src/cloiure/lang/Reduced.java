package cloiure.lang;

public final class Reduced implements IDeref
{
    Object val;

    public Reduced(Object val)
    {
        this.val = val;
    }

    public Object deref()
    {
        return val;
    }
}
