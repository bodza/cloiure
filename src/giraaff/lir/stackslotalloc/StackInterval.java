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
    private final VirtualStackSlot operand;
    // @field
    private StackInterval hint;
    // @field
    private final ValueKind<?> kind;
    // @field
    private int from = INVALID_START;
    // @field
    private int to = INVALID_END;
    // @field
    private StackSlot location;

    // @cons
    public StackInterval(VirtualStackSlot __operand, ValueKind<?> __kind)
    {
        super();
        this.operand = __operand;
        this.kind = __kind;
    }

    public VirtualStackSlot getOperand()
    {
        return operand;
    }

    public void addTo(int __opId)
    {
        if (__opId >= to)
        {
            to = __opId;
        }
    }

    protected void addFrom(int __opId)
    {
        if (from > __opId)
        {
            from = __opId;
            // set opId also as to if it has not yet been set
            if (to == INVALID_END)
            {
                to = __opId;
            }
        }
    }

    public ValueKind<?> kind()
    {
        return kind;
    }

    public StackSlot location()
    {
        return location;
    }

    public void setLocation(StackSlot __location)
    {
        this.location = __location;
    }

    public int from()
    {
        return from;
    }

    public int to()
    {
        return to;
    }

    public void fixFrom()
    {
        if (from == INVALID_START)
        {
            from = 0;
        }
    }

    public boolean isFixed()
    {
        return from == 0;
    }

    public void setLocationHint(StackInterval __locationHint)
    {
        hint = __locationHint;
    }

    public StackInterval locationHint()
    {
        return hint;
    }
}
