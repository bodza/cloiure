package cloiure.lang;

/**
 * Exception that carries data (a map) as additional payload. Cloiure programs that need
 * richer semantics for exceptions should use this in lieu of defining project-specific
 * exception classes.
 */
public class ExceptionInfo extends RuntimeException implements IExceptionInfo
{
    public final IPersistentMap data;

    public ExceptionInfo(String s, IPersistentMap data)
    {
        this(s, data, null);
    }

    public ExceptionInfo(String s, IPersistentMap data, Throwable throwable)
    {
        // null cause is equivalent to not passing a cause
        super(s, throwable);
        if (data != null)
        {
            this.data = data;
        }
        else
        {
            throw new IllegalArgumentException("Additional data must be non-nil.");
        }
    }

    public IPersistentMap getData()
    {
        return data;
    }

    public String toString()
    {
        return "cloiure.lang.ExceptionInfo: " + getMessage() + " " + data.toString();
    }
}
