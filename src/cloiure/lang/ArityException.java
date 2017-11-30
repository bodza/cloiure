package cloiure.lang;

/**
 * @since 1.3
 */
public class ArityException extends IllegalArgumentException
{
    final public int actual;

    final public String name;

    public ArityException(int actual, String name)
    {
        this(actual, name, null);
    }

    public ArityException(int actual, String name, Throwable cause)
    {
        super("Wrong number of args (" + actual + ") passed to: " + name, cause);
        this.actual = actual;
        this.name = name;
    }
}
