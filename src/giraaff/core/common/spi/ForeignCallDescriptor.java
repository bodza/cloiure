package giraaff.core.common.spi;

import java.util.Arrays;

/**
 * The name and signature of a foreign call. A foreign call differs from a normal compiled Java call
 * in at least one of these aspects:
 *
 * <li>The call is to C/C++/assembler code.</li>
 * <li>The call uses different conventions for passing parameters or returning values.</li>
 * <li>The callee has different register saving semantics. For example, the callee may save all
 * registers (apart from some specified temporaries) in which case the register allocator doesn't
 * not need to spill all live registers around the call site.</li>
 * <li>The call does not occur at an INVOKE* bytecode. Such a call could be transformed into a
 * standard Java call if the foreign routine is a normal Java method and the runtime supports
 * linking Java calls at arbitrary bytecodes.</li>
 */
// @class ForeignCallDescriptor
public final class ForeignCallDescriptor
{
    // @field
    private final String name;
    // @field
    private final Class<?> resultType;
    // @field
    private final Class<?>[] argumentTypes;

    // @cons
    public ForeignCallDescriptor(String __name, Class<?> __resultType, Class<?>... __argumentTypes)
    {
        super();
        this.name = __name;
        this.resultType = __resultType;
        this.argumentTypes = __argumentTypes;
    }

    /**
     * Gets the name of this foreign call.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the return type of this foreign call.
     */
    public Class<?> getResultType()
    {
        return resultType;
    }

    /**
     * Gets the argument types of this foreign call.
     */
    public Class<?>[] getArgumentTypes()
    {
        return argumentTypes.clone();
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof ForeignCallDescriptor)
        {
            ForeignCallDescriptor __other = (ForeignCallDescriptor) __obj;
            return __other.name.equals(name) && __other.resultType.equals(resultType) && Arrays.equals(__other.argumentTypes, argumentTypes);
        }
        return false;
    }
}
