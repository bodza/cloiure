package giraaff.lir;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.ValueKind;

/**
 * {@link VirtualStackSlot}s are stack slots that are not yet fixed to specific frame offset. They
 * are replaced by real {@link StackSlot}s with a fixed position in the frame before code emission.
 */
// @class VirtualStackSlot
public abstract class VirtualStackSlot extends AllocatableValue
{
    private final int id;

    // @cons
    public VirtualStackSlot(int id, ValueKind<?> kind)
    {
        super(kind);
        this.id = id;
    }

    public int getId()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        return prime * super.hashCode() + id;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!super.equals(obj))
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        VirtualStackSlot other = (VirtualStackSlot) obj;
        if (id != other.id)
        {
            return false;
        }
        return true;
    }
}
