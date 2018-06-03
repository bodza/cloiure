package giraaff.lir;

import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.ValueKind;

/**
 * Represents a value that is yet to be bound to a machine location (such as a {@link RegisterValue}
 * or {@link StackSlot}) by a register allocator.
 */
// @class Variable
public final class Variable extends AllocatableValue
{
    /**
     * The identifier of the variable. This is a non-zero index in a contiguous 0-based name space.
     */
    // @field
    public final int index;

    // @field
    private String name;

    /**
     * Creates a new variable.
     */
    // @cons
    public Variable(ValueKind<?> __kind, int __index)
    {
        super(__kind);
        this.index = __index;
    }

    public void setName(String __name)
    {
        this.name = __name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return 71 * super.hashCode() + index;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof Variable)
        {
            Variable __other = (Variable) __obj;
            return super.equals(__other) && index == __other.index;
        }
        return false;
    }
}
