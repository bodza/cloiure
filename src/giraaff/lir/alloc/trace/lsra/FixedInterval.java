package giraaff.lir.alloc.trace.lsra;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction;

/**
 * Represents a fixed interval.
 */
final class FixedInterval extends IntervalHint
{
    /**
     * The fixed operand of this interval.
     */
    public final AllocatableValue operand;

    /**
     * The head of the list of ranges describing this interval. This list is sorted by
     * {@linkplain LIRInstruction#id instruction ids}.
     */
    private FixedRange first;

    /**
     * Iterator used to traverse the ranges of an interval.
     */
    private FixedRange current;

    /**
     * Link to next interval in a sorted list of intervals that ends with {@link #EndMarker}.
     */
    FixedInterval next;

    private int cachedTo; // cached value: to of last range (-1: not cached)

    public FixedRange first()
    {
        return first;
    }

    @Override
    public int from()
    {
        return first.from;
    }

    public int to()
    {
        if (cachedTo == -1)
        {
            cachedTo = calcTo();
        }
        return cachedTo;
    }

    // test intersection
    boolean intersects(TraceInterval i)
    {
        return first.intersects(i);
    }

    int intersectsAt(TraceInterval i)
    {
        return first.intersectsAt(i);
    }

    // range iteration
    void rewindRange()
    {
        current = first;
    }

    void nextRange()
    {
        current = current.next;
    }

    int currentFrom()
    {
        return current.from;
    }

    int currentTo()
    {
        return current.to;
    }

    boolean currentAtEnd()
    {
        return current == FixedRange.EndMarker;
    }

    boolean currentIntersects(TraceInterval it)
    {
        return current.intersects(it);
    }

    int currentIntersectsAt(TraceInterval it)
    {
        return current.intersectsAt(it);
    }

    // range creation
    public void setFrom(int from)
    {
        first().from = from;
    }

    private boolean isEmpty()
    {
        return first() == FixedRange.EndMarker;
    }

    public void addRange(int from, int to)
    {
        if (isEmpty())
        {
            first = new FixedRange(from, to, first());
            return;
        }
        if (to <= to() && from >= from())
        {
            return;
        }
        if (from() == to)
        {
            first().from = from;
        }
        else
        {
            first = new FixedRange(from, to, first());
        }
    }

    @Override
    public AllocatableValue location()
    {
        return operand;
    }

    /**
     * Sentinel interval to denote the end of an interval list.
     */
    static final FixedInterval EndMarker = new FixedInterval(Value.ILLEGAL);

    FixedInterval(AllocatableValue operand)
    {
        this.operand = operand;
        this.first = FixedRange.EndMarker;
        this.current = FixedRange.EndMarker;
        this.next = FixedInterval.EndMarker;
        this.cachedTo = -1;
    }

    int calcTo()
    {
        FixedRange r = first;
        while (r.next != FixedRange.EndMarker)
        {
            r = r.next;
        }
        return r.to;
    }

    // returns true if the opId is inside the interval
    boolean covers(int opId, LIRInstruction.OperandMode mode)
    {
        FixedRange cur = first;

        while (cur != FixedRange.EndMarker && cur.to < opId)
        {
            cur = cur.next;
        }
        if (cur != FixedRange.EndMarker)
        {
            if (mode == LIRInstruction.OperandMode.DEF)
            {
                return cur.from <= opId && opId < cur.to;
            }
            else
            {
                return cur.from <= opId && opId <= cur.to;
            }
        }
        return false;
    }

    // returns true if the interval has any hole between holeFrom and holeTo
    // (even if the hole has only the length 1)
    boolean hasHoleBetween(int holeFrom, int holeTo)
    {
        FixedRange cur = first;
        while (cur != FixedRange.EndMarker)
        {
            // hole-range starts before this range . hole
            if (holeFrom < cur.from)
            {
                return true;

                // hole-range completely inside this range . no hole
            }
            else
            {
                if (holeTo <= cur.to)
                {
                    return false;

                    // overlapping of hole-range with this range . hole
                }
                else
                {
                    if (holeFrom <= cur.to)
                    {
                        return true;
                    }
                }
            }

            cur = cur.next;
        }

        return false;
    }

    @Override
    public String toString()
    {
        if (this == EndMarker)
        {
            return "EndMarker [?,?]";
        }
        String from = "?";
        String to = "?";
        if (first != null && first != FixedRange.EndMarker)
        {
            from = String.valueOf(from());
            // to() may cache a computed value, modifying the current object, which is a bad idea
            // for a printing function. Compute it directly instead.
            to = String.valueOf(calcTo());
        }
        String locationString = "@" + this.operand;
        return ValueUtil.asRegister(operand).number + ":" + operand + (ValueUtil.isRegister(operand) ? "" : locationString) + "[" + from + "," + to + "]";
    }

    /**
     * Gets a single line string for logging the details of this interval to a log stream.
     */
    @Override
    public String logString()
    {
        StringBuilder buf = new StringBuilder(100);
        buf.append("fix ").append(ValueUtil.asRegister(operand).number).append(':').append(operand).append(' ');

        buf.append(" ranges{");

        // print ranges
        FixedRange cur = first;
        while (cur != FixedRange.EndMarker)
        {
            if (cur != first)
            {
                buf.append(", ");
            }
            buf.append(cur);
            cur = cur.next;
        }
        buf.append("}");
        return buf.toString();
    }
}
