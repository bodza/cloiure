package giraaff.lir;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

///
// Represents an inlined {@link Constant} value.
///
// @class ConstantValue
public final class ConstantValue extends Value
{
    // @field
    private final Constant ___constant;

    // @cons ConstantValue
    public ConstantValue(ValueKind<?> __kind, Constant __constant)
    {
        super(__kind);
        this.___constant = __constant;
    }

    public Constant getConstant()
    {
        return this.___constant;
    }

    public boolean isJavaConstant()
    {
        return this.___constant instanceof JavaConstant;
    }

    public JavaConstant getJavaConstant()
    {
        return (JavaConstant) this.___constant;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (__obj instanceof ConstantValue)
        {
            ConstantValue __other = (ConstantValue) __obj;
            return super.equals(__other) && this.___constant.equals(__other.___constant);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return this.___constant.hashCode() + super.hashCode();
    }
}
