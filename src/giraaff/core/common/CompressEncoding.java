package giraaff.core.common;

/**
 * A compact representation of the different encoding strategies for Objects and metadata.
 */
// @class CompressEncoding
public final class CompressEncoding
{
    private final long base;
    private final int shift;

    // @cons
    public CompressEncoding(long base, int shift)
    {
        super();
        this.base = base;
        this.shift = shift;
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
    public String toString()
    {
        return "base: " + base + " shift: " + shift;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (base ^ (base >>> 32));
        result = prime * result + shift;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof CompressEncoding)
        {
            CompressEncoding other = (CompressEncoding) obj;
            return base == other.base && shift == other.shift;
        }
        return false;
    }
}
