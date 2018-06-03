package giraaff.core.common.type;

import java.nio.ByteBuffer;

import jdk.vm.ci.meta.SerializableConstant;

/**
 * Type describing values that support arithmetic operations.
 */
// @class ArithmeticStamp
public abstract class ArithmeticStamp extends Stamp
{
    // @field
    private final ArithmeticOpTable ops;

    // @cons
    protected ArithmeticStamp(ArithmeticOpTable __ops)
    {
        super();
        this.ops = __ops;
    }

    public ArithmeticOpTable getOps()
    {
        return ops;
    }

    public abstract SerializableConstant deserialize(ByteBuffer buffer);

    @Override
    public Stamp improveWith(Stamp __other)
    {
        if (this.isCompatible(__other))
        {
            return this.join(__other);
        }
        // Cannot improve, because stamps are not compatible.
        return this;
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        return __prime * 1 + ops.hashCode();
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (!(__obj instanceof ArithmeticStamp))
        {
            return false;
        }
        return true;
    }
}
