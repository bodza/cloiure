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
    // @defs
    private static final MethodPointerStamp
        METHOD             = new MethodPointerStamp(false, false),
        METHOD_NON_NULL    = new MethodPointerStamp(true, false),
        METHOD_ALWAYS_NULL = new MethodPointerStamp(false, true);

    public static MethodPointerStamp method()
    {
        return METHOD;
    }

    public static MethodPointerStamp methodNonNull()
    {
        return METHOD_NON_NULL;
    }

    // @cons MethodPointerStamp
    private MethodPointerStamp(boolean __nonNull, boolean __alwaysNull)
    {
        super(__nonNull, __alwaysNull);
    }

    @Override
    protected AbstractPointerStamp copyWith(boolean __newNonNull, boolean __newAlwaysNull)
    {
        if (__newNonNull)
        {
            return METHOD_NON_NULL;
        }
        else if (__newAlwaysNull)
        {
            return METHOD_ALWAYS_NULL;
        }
        else
        {
            return METHOD;
        }
    }

    @Override
    public boolean isCompatible(Stamp __otherStamp)
    {
        if (this == __otherStamp)
        {
            return true;
        }
        return __otherStamp instanceof MethodPointerStamp;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        if (__constant instanceof HotSpotMetaspaceConstant)
        {
            return ((HotSpotMetaspaceConstant) __constant).asResolvedJavaMethod() != null;
        }
        else
        {
            return super.isCompatible(__constant);
        }
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        if (JavaConstant.NULL_POINTER.equals(__c))
        {
            return METHOD_ALWAYS_NULL;
        }
        else
        {
            return METHOD_NON_NULL;
        }
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        HotSpotMemoryAccessProvider __hsProvider = (HotSpotMemoryAccessProvider) __provider;
        return __hsProvider.readMethodPointerConstant(__base, __displacement);
    }
}
