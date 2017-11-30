package cloiure.lang;

final public class Volatile implements IDeref
{
    volatile Object val;

    public Volatile(Object val)
    {
        this.val = val;
    }

    public Object deref()
    {
        return val;
    }

    public Object reset(Object newval)
    {
        return this.val = newval;
    }
}
