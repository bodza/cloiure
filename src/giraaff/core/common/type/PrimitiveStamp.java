package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MemoryAccessProvider;

///
// Type describing primitive values.
///
// @class PrimitiveStamp
public abstract class PrimitiveStamp extends ArithmeticStamp
{
    // @field
    private final int ___bits;

    // @cons PrimitiveStamp
    protected PrimitiveStamp(int __bits, ArithmeticOpTable __ops)
    {
        super(__ops);
        this.___bits = __bits;
    }

    ///
    // The width in bits of the value described by this stamp.
    ///
    public int getBits()
    {
        return this.___bits;
    }

    public static int getBits(Stamp __stamp)
    {
        if (__stamp instanceof PrimitiveStamp)
        {
            return ((PrimitiveStamp) __stamp).getBits();
        }
        else
        {
            return 0;
        }
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        try
        {
            return __provider.readPrimitiveConstant(getStackKind(), __base, __displacement, getBits());
        }
        catch (IllegalArgumentException __e)
        {
            // It's possible that the base and displacement aren't valid together so simply return null.
            return null;
        }
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        return __prime * super.hashCode() + this.___bits;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (!(__obj instanceof PrimitiveStamp))
        {
            return false;
        }
        PrimitiveStamp __other = (PrimitiveStamp) __obj;
        if (this.___bits != __other.___bits)
        {
            return false;
        }
        return super.equals(__obj);
    }
}
