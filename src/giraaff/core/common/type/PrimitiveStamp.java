package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MemoryAccessProvider;

/**
 * Type describing primitive values.
 */
// @class PrimitiveStamp
public abstract class PrimitiveStamp extends ArithmeticStamp
{
    private final int bits;

    // @cons
    protected PrimitiveStamp(int bits, ArithmeticOpTable ops)
    {
        super(ops);
        this.bits = bits;
    }

    /**
     * The width in bits of the value described by this stamp.
     */
    public int getBits()
    {
        return bits;
    }

    public static int getBits(Stamp stamp)
    {
        if (stamp instanceof PrimitiveStamp)
        {
            return ((PrimitiveStamp) stamp).getBits();
        }
        else
        {
            return 0;
        }
    }

    @Override
    public Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement)
    {
        try
        {
            return provider.readPrimitiveConstant(getStackKind(), base, displacement, getBits());
        }
        catch (IllegalArgumentException e)
        {
            // It's possible that the base and displacement aren't valid together so simply return null.
            return null;
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        return prime * super.hashCode() + bits;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof PrimitiveStamp))
        {
            return false;
        }
        PrimitiveStamp other = (PrimitiveStamp) obj;
        if (bits != other.bits)
        {
            return false;
        }
        return super.equals(obj);
    }
}
