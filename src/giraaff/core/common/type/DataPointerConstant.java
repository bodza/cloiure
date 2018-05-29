package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.SerializableConstant;

/**
 * Base class for {@link Constant constants} that represent a pointer to the data section.
 */
// @class DataPointerConstant
public abstract class DataPointerConstant implements SerializableConstant
{
    private final int alignment;

    // @cons
    protected DataPointerConstant(int alignment)
    {
        super();
        this.alignment = alignment;
    }

    /**
     * Get the minimum alignment of the data in the data section.
     */
    public final int getAlignment()
    {
        return alignment;
    }

    @Override
    public boolean isDefaultForKind()
    {
        return false;
    }
}
