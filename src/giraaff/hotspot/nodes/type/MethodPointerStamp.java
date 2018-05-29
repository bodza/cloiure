package giraaff.hotspot.nodes.type;

import jdk.vm.ci.hotspot.HotSpotMemoryAccessProvider;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.Stamp;

// @class MethodPointerStamp
public final class MethodPointerStamp extends MetaspacePointerStamp
{
    private static final MethodPointerStamp METHOD = new MethodPointerStamp(false, false);

    private static final MethodPointerStamp METHOD_NON_NULL = new MethodPointerStamp(true, false);

    private static final MethodPointerStamp METHOD_ALWAYS_NULL = new MethodPointerStamp(false, true);

    public static MethodPointerStamp method()
    {
        return METHOD;
    }

    public static MethodPointerStamp methodNonNull()
    {
        return METHOD_NON_NULL;
    }

    // @cons
    private MethodPointerStamp(boolean nonNull, boolean alwaysNull)
    {
        super(nonNull, alwaysNull);
    }

    @Override
    protected AbstractPointerStamp copyWith(boolean newNonNull, boolean newAlwaysNull)
    {
        if (newNonNull)
        {
            return METHOD_NON_NULL;
        }
        else if (newAlwaysNull)
        {
            return METHOD_ALWAYS_NULL;
        }
        else
        {
            return METHOD;
        }
    }

    @Override
    public boolean isCompatible(Stamp otherStamp)
    {
        if (this == otherStamp)
        {
            return true;
        }
        return otherStamp instanceof MethodPointerStamp;
    }

    @Override
    public boolean isCompatible(Constant constant)
    {
        if (constant instanceof HotSpotMetaspaceConstant)
        {
            return ((HotSpotMetaspaceConstant) constant).asResolvedJavaMethod() != null;
        }
        else
        {
            return super.isCompatible(constant);
        }
    }

    @Override
    public Stamp constant(Constant c, MetaAccessProvider meta)
    {
        if (JavaConstant.NULL_POINTER.equals(c))
        {
            return METHOD_ALWAYS_NULL;
        }
        else
        {
            return METHOD_NON_NULL;
        }
    }

    @Override
    public Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement)
    {
        HotSpotMemoryAccessProvider hsProvider = (HotSpotMemoryAccessProvider) provider;
        return hsProvider.readMethodPointerConstant(base, displacement);
    }

    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder("Method*");
        appendString(ret);
        return ret.toString();
    }
}
