package giraaff.lir.stackslotalloc;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.ValueKind;

import giraaff.lir.VirtualStackSlot;

public final class StackInterval
{
    private static final int INVALID_START = Integer.MAX_VALUE;
    private static final int INVALID_END = Integer.MIN_VALUE;
    private final VirtualStackSlot operand;
    private StackInterval hint;
    private final ValueKind<?> kind;
    private int from = INVALID_START;
    private int to = INVALID_END;
    private StackSlot location;

    public StackInterval(VirtualStackSlot operand, ValueKind<?> kind)
    {
        this.operand = operand;
        this.kind = kind;
    }

    public VirtualStackSlot getOperand()
    {
        return operand;
    }

    public void addTo(int opId)
    {
        if (opId >= to)
        {
            to = opId;
        }
    }

    protected void addFrom(int opId)
    {
        if (from > opId)
        {
            from = opId;
            // set opId also as to if it has not yet been set
            if (to == INVALID_END)
            {
                to = opId;
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

    public void setLocation(StackSlot location)
    {
        this.location = location;
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

    @Override
    public String toString()
    {
        return String.format("SI[%d-%d] k=%s o=%s l=%s h=%s", from, to, kind, operand, location, hint != null ? hint.getOperand() : "null");
    }

    public void setLocationHint(StackInterval locationHint)
    {
        hint = locationHint;
    }

    public StackInterval locationHint()
    {
        return hint;
    }
}
