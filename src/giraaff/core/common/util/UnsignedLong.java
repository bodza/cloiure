package giraaff.core.common.util;

// @class UnsignedLong
public final class UnsignedLong
{
    // @field
    private final long ___value;

    // @cons
    public UnsignedLong(long __value)
    {
        super();
        this.___value = __value;
    }

    public long asLong()
    {
        return this.___value;
    }

    public boolean equals(long __unsignedValue)
    {
        return this.___value == __unsignedValue;
    }

    public boolean isLessThan(long __unsignedValue)
    {
        return Long.compareUnsigned(this.___value, __unsignedValue) < 0;
    }

    public boolean isLessOrEqualTo(long __unsignedValue)
    {
        return Long.compareUnsigned(this.___value, __unsignedValue) <= 0;
    }

    public UnsignedLong times(long __unsignedValue)
    {
        if (__unsignedValue != 0 && Long.compareUnsigned(this.___value, Long.divideUnsigned(0xffff_ffff_ffff_ffffL, __unsignedValue)) > 0)
        {
            throw new ArithmeticException();
        }
        return new UnsignedLong(this.___value * __unsignedValue);
    }

    public UnsignedLong minus(long __unsignedValue)
    {
        if (Long.compareUnsigned(this.___value, __unsignedValue) < 0)
        {
            throw new ArithmeticException();
        }
        return new UnsignedLong(this.___value - __unsignedValue);
    }

    public UnsignedLong plus(long __unsignedValue)
    {
        if (Long.compareUnsigned(0xffff_ffff_ffff_ffffL - __unsignedValue, this.___value) < 0)
        {
            throw new ArithmeticException();
        }
        return new UnsignedLong(this.___value + __unsignedValue);
    }

    public UnsignedLong wrappingPlus(long __unsignedValue)
    {
        return new UnsignedLong(this.___value + __unsignedValue);
    }

    public UnsignedLong wrappingTimes(long __unsignedValue)
    {
        return new UnsignedLong(this.___value * __unsignedValue);
    }

    @Override
    public boolean equals(Object __o)
    {
        if (this == __o)
        {
            return true;
        }
        if (__o == null || getClass() != __o.getClass())
        {
            return false;
        }
        UnsignedLong __that = (UnsignedLong) __o;
        return this.___value == __that.___value;
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(this.___value);
    }
}
