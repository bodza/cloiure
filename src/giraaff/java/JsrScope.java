package giraaff.java;

import jdk.vm.ci.code.BailoutException;

// @class JsrScope
public final class JsrScope
{
    public static final JsrScope EMPTY_SCOPE = new JsrScope();

    private final long scope;

    // @cons
    private JsrScope(long scope)
    {
        super();
        this.scope = scope;
    }

    // @cons
    public JsrScope()
    {
        super();
        this.scope = 0;
    }

    public int nextReturnAddress()
    {
        return (int) (scope & 0xffff);
    }

    public JsrScope push(int jsrReturnBci)
    {
        if ((scope & 0xffff000000000000L) != 0)
        {
            throw new BailoutException("only 4 jsr nesting levels are supported");
        }
        return new JsrScope((scope << 16) | jsrReturnBci);
    }

    public boolean isEmpty()
    {
        return scope == 0;
    }

    public boolean isPrefixOf(JsrScope other)
    {
        return (scope & other.scope) == scope;
    }

    public JsrScope pop()
    {
        return new JsrScope(scope >>> 16);
    }

    @Override
    public int hashCode()
    {
        return (int) (scope ^ (scope >>> 32));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        return obj != null && getClass() == obj.getClass() && scope == ((JsrScope) obj).scope;
    }
}
