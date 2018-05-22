package giraaff.core.common.type;

import java.nio.ByteBuffer;

import jdk.vm.ci.meta.SerializableConstant;

/**
 * Type describing values that support arithmetic operations.
 */
public abstract class ArithmeticStamp extends Stamp
{
    private final ArithmeticOpTable ops;

    protected ArithmeticStamp(ArithmeticOpTable ops)
    {
        this.ops = ops;
    }

    public ArithmeticOpTable getOps()
    {
        return ops;
    }

    public abstract SerializableConstant deserialize(ByteBuffer buffer);

    @Override
    public Stamp improveWith(Stamp other)
    {
        if (this.isCompatible(other))
        {
            return this.join(other);
        }
        // Cannot improve, because stamps are not compatible.
        return this;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ops.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof ArithmeticStamp))
        {
            return false;
        }
        return true;
    }
}
