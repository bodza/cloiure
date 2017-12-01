package cloiure.lang;

public class Delay implements IDeref, IPending
{
    volatile Object val;
    volatile Throwable exception;
    volatile IFn fn;

    public Delay(IFn fn)
    {
        this.fn = fn;
        this.val = null;
        this.exception = null;
    }

    static public Object force(Object x)
    {
        return (x instanceof Delay) ? ((Delay) x).deref() : x;
    }

    public Object deref()
    {
        if (fn != null)
        {
            synchronized (this)
            {
                // double check
                if (fn != null)
                {
                    try
                    {
                        val = fn.invoke();
                    }
                    catch (Throwable t)
                    {
                        exception = t;
                    }
                    fn = null;
                }
            }
        }
        if (exception != null)
        {
            throw Util.sneakyThrow(exception);
        }
        return val;
    }

    synchronized public boolean isRealized()
    {
        return (fn == null);
    }
}
