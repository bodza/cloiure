package giraaff.lir.stackslotalloc;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.ValueKind;

import giraaff.lir.VirtualStackSlot;

// @class StackInterval
public final class StackInterval
{
    // @def
    private static final int INVALID_START = Integer.MAX_VALUE;
    // @def
    private static final int INVALID_END = Integer.MIN_VALUE;
    // @field
    private final VirtualStackSlot ___operand;
    // @field
    private StackInterval ___hint;
    // @field
    private final ValueKind<?> ___kind;
    // @field
    private int ___from = INVALID_START;
    // @field
    private int ___to = INVALID_END;
    // @field
    private StackSlot ___location;

    // @cons
    public StackInterval(VirtualStackSlot __operand, ValueKind<?> __kind)
    {
        super();
        this.___operand = __operand;
        this.___kind = __kind;
    }

    public VirtualStackSlot getOperand()
    {
        return this.___operand;
    }

    public void addTo(int __opId)
    {
        if (__opId >= this.___to)
        {
            this.___to = __opId;
        }
    }

    protected void addFrom(int __opId)
    {
        if (this.___from > __opId)
        {
            this.___from = __opId;
            // set opId also as to if it has not yet been set
            if (this.___to == INVALID_END)
            {
                this.___to = __opId;
            }
        }
    }

    public ValueKind<?> kind()
    {
        return this.___kind;
    }

    public StackSlot location()
    {
        return this.___location;
    }

    public void setLocation(StackSlot __location)
    {
        this.___location = __location;
    }

    public int from()
    {
        return this.___from;
    }

    public int to()
    {
        return this.___to;
    }

    public void fixFrom()
    {
        if (this.___from == INVALID_START)
        {
            this.___from = 0;
        }
    }

    public boolean isFixed()
    {
        return this.___from == 0;
    }

    public void setLocationHint(StackInterval __locationHint)
    {
        this.___hint = __locationHint;
    }

    public StackInterval locationHint()
    {
        return this.___hint;
    }
}
