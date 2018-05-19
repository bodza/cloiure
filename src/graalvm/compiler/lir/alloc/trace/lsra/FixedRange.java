package graalvm.compiler.lir.alloc.trace.lsra;

/**
 * Represents a range of integers from a start (inclusive) to an end (exclusive).
 */
final class FixedRange
{
    public static final FixedRange EndMarker = new FixedRange(Integer.MAX_VALUE, Integer.MAX_VALUE, null);

    /**
     * The start of the range, inclusive.
     */
    public int from;

    /**
     * The end of the range, exclusive.
     */
    public int to;

    /**
     * A link to allow the range to be put into a singly linked list.
     */
    public FixedRange next;

    boolean intersects(TraceInterval i)
    {
        return intersectsAt(i) != -1;
    }

    /**
     * Creates a new range.
     *
     * @param from the start of the range, inclusive
     * @param to the end of the range, exclusive
     * @param next link to the next range in a linked list
     */
    FixedRange(int from, int to, FixedRange next)
    {
        this.from = from;
        this.to = to;
        this.next = next;
    }

    int intersectsAt(TraceInterval other)
    {
        FixedRange range = this;
        int intervalFrom = other.from();
        int intervalTo = other.to();

        do
        {
            if (range.from < intervalFrom)
            {
                if (range.to <= intervalFrom)
                {
                    range = range.next;
                    if (range == EndMarker)
                    {
                        return -1;
                    }
                }
                else
                {
                    return intervalFrom;
                }
            }
            else
            {
                if (intervalFrom < range.from)
                {
                    if (intervalTo <= range.from)
                    {
                        return -1;
                    }
                    return range.from;
                }
                else
                {
                    if (range.from == range.to)
                    {
                        range = range.next;
                        if (range == EndMarker)
                        {
                            return -1;
                        }
                    }
                    else
                    {
                        if (intervalFrom == intervalTo)
                        {
                            return -1;
                        }
                        return range.from;
                    }
                }
            }
        } while (true);
    }

    @Override
    public String toString()
    {
        return "[" + from + ", " + to + "]";
    }
}
