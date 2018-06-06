package giraaff.core.common;

///
// A compact representation of the different encoding strategies for Objects and metadata.
///
// @class CompressEncoding
public final class CompressEncoding
{
    // @field
    private final long ___base;
    // @field
    private final int ___shift;

    // @cons CompressEncoding
    public CompressEncoding(long __base, int __shift)
    {
        super();
        this.___base = __base;
        this.___shift = __shift;
    }

    public boolean hasBase()
    {
        return this.___base != 0;
    }

    public boolean hasShift()
    {
        return this.___shift != 0;
    }

    public long getBase()
    {
        return this.___base;
    }

    public int getShift()
    {
        return this.___shift;
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + (int) (this.___base ^ (this.___base >>> 32));
        __result = __prime * __result + this.___shift;
        return __result;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof CompressEncoding)
        {
            CompressEncoding __other = (CompressEncoding) __obj;
            return this.___base == __other.___base && this.___shift == __other.___shift;
        }
        return false;
    }
}
