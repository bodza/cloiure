package giraaff.java;

import jdk.vm.ci.code.BailoutException;

// @class JsrScope
public final class JsrScope
{
    // @def
    public static final JsrScope EMPTY_SCOPE = new JsrScope();

    // @field
    private final long ___scope;

    // @cons JsrScope
    private JsrScope(long __scope)
    {
        super();
        this.___scope = __scope;
    }

    // @cons JsrScope
    public JsrScope()
    {
        super();
        this.___scope = 0;
    }

    public int nextReturnAddress()
    {
        return (int) (this.___scope & 0xffff);
    }

    public JsrScope push(int __jsrReturnBci)
    {
        if ((this.___scope & 0xffff000000000000L) != 0)
        {
            throw new BailoutException("only 4 jsr nesting levels are supported");
        }
        return new JsrScope((this.___scope << 16) | __jsrReturnBci);
    }

    public boolean isEmpty()
    {
        return this.___scope == 0;
    }

    public boolean isPrefixOf(JsrScope __other)
    {
        return (this.___scope & __other.___scope) == this.___scope;
    }

    public JsrScope pop()
    {
        return new JsrScope(this.___scope >>> 16);
    }

    @Override
    public int hashCode()
    {
        return (int) (this.___scope ^ (this.___scope >>> 32));
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        return __obj != null && getClass() == __obj.getClass() && this.___scope == ((JsrScope) __obj).___scope;
    }
}
