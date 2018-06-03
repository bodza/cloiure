package giraaff.lir;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.ValueKind;

///
// {@link VirtualStackSlot}s are stack slots that are not yet fixed to specific frame offset. They
// are replaced by real {@link StackSlot}s with a fixed position in the frame before code emission.
///
// @class VirtualStackSlot
public abstract class VirtualStackSlot extends AllocatableValue
{
    // @field
    private final int ___id;

    // @cons
    public VirtualStackSlot(int __id, ValueKind<?> __kind)
    {
        super(__kind);
        this.___id = __id;
    }

    public int getId()
    {
        return this.___id;
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        return __prime * super.hashCode() + this.___id;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (!super.equals(__obj))
        {
            return false;
        }
        if (getClass() != __obj.getClass())
        {
            return false;
        }
        VirtualStackSlot __other = (VirtualStackSlot) __obj;
        if (this.___id != __other.___id)
        {
            return false;
        }
        return true;
    }
}
