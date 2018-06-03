package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;

///
// Abstract base class of all pointer types.
///
// @class AbstractPointerStamp
public abstract class AbstractPointerStamp extends Stamp
{
    // @field
    private final boolean ___nonNull;
    // @field
    private final boolean ___alwaysNull;

    // @cons
    protected AbstractPointerStamp(boolean __nonNull, boolean __alwaysNull)
    {
        super();
        this.___nonNull = __nonNull;
        this.___alwaysNull = __alwaysNull;
    }

    public boolean nonNull()
    {
        return this.___nonNull;
    }

    public boolean alwaysNull()
    {
        return this.___alwaysNull;
    }

    protected abstract AbstractPointerStamp copyWith(boolean __newNonNull, boolean __newAlwaysNull);

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + (this.___alwaysNull ? 1231 : 1237);
        __result = __prime * __result + (this.___nonNull ? 1231 : 1237);
        return __result;
    }

    protected Stamp defaultPointerJoin(Stamp __stamp)
    {
        AbstractPointerStamp __other = (AbstractPointerStamp) __stamp;
        boolean __joinNonNull = this.___nonNull || __other.___nonNull;
        boolean __joinAlwaysNull = this.___alwaysNull || __other.___alwaysNull;
        if (__joinNonNull && __joinAlwaysNull)
        {
            return empty();
        }
        else
        {
            return copyWith(__joinNonNull, __joinAlwaysNull);
        }
    }

    @Override
    public Stamp improveWith(Stamp __other)
    {
        return join(__other);
    }

    @Override
    public Stamp meet(Stamp __stamp)
    {
        AbstractPointerStamp __other = (AbstractPointerStamp) __stamp;
        boolean __meetNonNull = this.___nonNull && __other.___nonNull;
        boolean __meetAlwaysNull = this.___alwaysNull && __other.___alwaysNull;
        return copyWith(__meetNonNull, __meetAlwaysNull);
    }

    @Override
    public Stamp unrestricted()
    {
        return copyWith(false, false);
    }

    public static Stamp pointerNonNull(Stamp __stamp)
    {
        AbstractPointerStamp __pointer = (AbstractPointerStamp) __stamp;
        return __pointer.asNonNull();
    }

    public static Stamp pointerMaybeNull(Stamp __stamp)
    {
        AbstractPointerStamp __pointer = (AbstractPointerStamp) __stamp;
        return __pointer.asMaybeNull();
    }

    public static Stamp pointerAlwaysNull(Stamp __stamp)
    {
        AbstractPointerStamp __pointer = (AbstractPointerStamp) __stamp;
        return __pointer.asAlwaysNull();
    }

    public Stamp asNonNull()
    {
        if (isEmpty())
        {
            return this;
        }
        return copyWith(true, false);
    }

    public Stamp asMaybeNull()
    {
        if (isEmpty())
        {
            return this;
        }
        return copyWith(false, false);
    }

    public Stamp asAlwaysNull()
    {
        if (isEmpty())
        {
            return this;
        }
        return copyWith(false, true);
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj == null || getClass() != __obj.getClass())
        {
            return false;
        }
        AbstractPointerStamp __other = (AbstractPointerStamp) __obj;
        return this.___alwaysNull == __other.___alwaysNull && this.___nonNull == __other.___nonNull;
    }

    @Override
    public Constant asConstant()
    {
        if (this.___alwaysNull)
        {
            return JavaConstant.NULL_POINTER;
        }
        else
        {
            return null;
        }
    }

    @Override
    public JavaKind getStackKind()
    {
        return JavaKind.Illegal;
    }
}
