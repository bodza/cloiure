package giraaff.lir;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

/**
 * Represents an inlined {@link Constant} value.
 */
// @class ConstantValue
public final class ConstantValue extends Value
{
    // @field
    private final Constant constant;

    // @cons
    public ConstantValue(ValueKind<?> __kind, Constant __constant)
    {
        super(__kind);
        this.constant = __constant;
    }

    public Constant getConstant()
    {
        return constant;
    }

    public boolean isJavaConstant()
    {
        return constant instanceof JavaConstant;
    }

    public JavaConstant getJavaConstant()
    {
        return (JavaConstant) constant;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof ConstantValue)
        {
            ConstantValue __other = (ConstantValue) __obj;
            return super.equals(__other) && this.constant.equals(__other.constant);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return constant.hashCode() + super.hashCode();
    }
}
