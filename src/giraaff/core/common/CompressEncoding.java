package giraaff.core.common;

/**
 * A compact representation of the different encoding strategies for Objects and metadata.
 */
// @class CompressEncoding
public final class CompressEncoding
{
    // @field
    private final long base;
    // @field
    private final int shift;

    // @cons
    public CompressEncoding(long __base, int __shift)
    {
        super();
        this.base = __base;
        this.shift = __shift;
    }

    public boolean hasBase()
    {
        return base != 0;
    }

    public boolean hasShift()
    {
        return shift != 0;
    }

    public long getBase()
    {
        return base;
    }

    public int getShift()
    {
        return shift;
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + (int) (base ^ (base >>> 32));
        __result = __prime * __result + shift;
        return __result;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof CompressEncoding)
        {
            CompressEncoding __other = (CompressEncoding) __obj;
            return base == __other.base && shift == __other.shift;
        }
        return false;
    }
}
