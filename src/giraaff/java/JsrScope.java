package giraaff.java;

import jdk.vm.ci.code.BailoutException;

// @class JsrScope
public final class JsrScope
{
    // @def
    public static final JsrScope EMPTY_SCOPE = new JsrScope();

    // @field
    private final long scope;

    // @cons
    private JsrScope(long __scope)
    {
        super();
        this.scope = __scope;
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

    public JsrScope push(int __jsrReturnBci)
    {
        if ((scope & 0xffff000000000000L) != 0)
        {
            throw new BailoutException("only 4 jsr nesting levels are supported");
        }
        return new JsrScope((scope << 16) | __jsrReturnBci);
    }

    public boolean isEmpty()
    {
        return scope == 0;
    }

    public boolean isPrefixOf(JsrScope __other)
    {
        return (scope & __other.scope) == scope;
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
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        return __obj != null && getClass() == __obj.getClass() && scope == ((JsrScope) __obj).scope;
    }
}
