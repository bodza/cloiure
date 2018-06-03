package giraaff.lir.alloc.lsra;

///
// Represents a range of integers from a start (inclusive) to an end (exclusive.
///
// @class Range
public final class Range
{
    ///
    // The start of the range, inclusive.
    ///
    // @field
    public int ___from;

    ///
    // The end of the range, exclusive.
    ///
    // @field
    public int ___to;

    ///
    // A link to allow the range to be put into a singly linked list.
    ///
    // @field
    public Range ___next;

    boolean intersects(Range __r)
    {
        return intersectsAt(__r) != -1;
    }

    ///
    // Creates a new range.
    //
    // @param from the start of the range, inclusive
    // @param to the end of the range, exclusive
    // @param next link to the next range in a linked list
    ///
    // @cons
    Range(int __from, int __to, Range __next)
    {
        super();
        this.___from = __from;
        this.___to = __to;
        this.___next = __next;
    }

    public boolean isEndMarker()
    {
        return this.___from == Integer.MAX_VALUE;
    }

    int intersectsAt(Range __other)
    {
        Range __r1 = this;
        Range __r2 = __other;

        do
        {
            if (__r1.___from < __r2.___from)
            {
                if (__r1.___to <= __r2.___from)
                {
                    __r1 = __r1.___next;
                    if (__r1.isEndMarker())
                    {
                        return -1;
                    }
                }
                else
                {
                    return __r2.___from;
                }
            }
            else
            {
                if (__r2.___from < __r1.___from)
                {
                    if (__r2.___to <= __r1.___from)
                    {
                        __r2 = __r2.___next;
                        if (__r2.isEndMarker())
                        {
                            return -1;
                        }
                    }
                    else
                    {
                        return __r1.___from;
                    }
                }
                else // r1.from() == r2.from()
                {
                    if (__r1.___from == __r1.___to)
                    {
                        __r1 = __r1.___next;
                        if (__r1.isEndMarker())
                        {
                            return -1;
                        }
                    }
                    else
                    {
                        if (__r2.___from == __r2.___to)
                        {
                            __r2 = __r2.___next;
                            if (__r2.isEndMarker())
                            {
                                return -1;
                            }
                        }
                        else
                        {
                            return __r1.___from;
                        }
                    }
                }
            }
        } while (true);
    }
}
