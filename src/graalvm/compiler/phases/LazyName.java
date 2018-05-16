package graalvm.compiler.phases;

import graalvm.compiler.debug.DebugContext;

/**
 * A name whose {@link String} value is computed only when it is needed. This is useful in
 * combination with debugging facilities such as {@link DebugContext#scope(Object)} where the
 * {@link String} value of a name is only needed if debugging is enabled.
 */
public abstract class LazyName implements CharSequence
{
    private String value;

    @Override
    public int length()
    {
        return toString().length();
    }

    @Override
    public char charAt(int index)
    {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return toString().subSequence(start, end);
    }

    @Override
    public final String toString()
    {
        if (value == null)
        {
            value = createString();
        }
        return value;
    }

    /**
     * Creates the {@link String} value of this name.
     */
    protected abstract String createString();
}
