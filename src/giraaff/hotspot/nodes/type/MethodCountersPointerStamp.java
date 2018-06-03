package giraaff.hotspot.nodes.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.Stamp;

// @class MethodCountersPointerStamp
public final class MethodCountersPointerStamp extends MetaspacePointerStamp
{
    private static final MethodCountersPointerStamp
        METHOD_COUNTERS             = new MethodCountersPointerStamp(false, false),
        METHOD_COUNTERS_NON_NULL    = new MethodCountersPointerStamp(true, false),
        METHOD_COUNTERS_ALWAYS_NULL = new MethodCountersPointerStamp(false, true);

    public static MethodCountersPointerStamp methodCounters()
    {
        return METHOD_COUNTERS;
    }

    public static MethodCountersPointerStamp methodCountersNonNull()
    {
        return METHOD_COUNTERS_NON_NULL;
    }

    // @cons
    private MethodCountersPointerStamp(boolean __nonNull, boolean __alwaysNull)
    {
        super(__nonNull, __alwaysNull);
    }

    @Override
    protected AbstractPointerStamp copyWith(boolean __newNonNull, boolean __newAlwaysNull)
    {
        if (__newNonNull)
        {
            return METHOD_COUNTERS_NON_NULL;
        }
        else if (__newAlwaysNull)
        {
            return METHOD_COUNTERS_ALWAYS_NULL;
        }
        else
        {
            return METHOD_COUNTERS;
        }
    }

    @Override
    public boolean isCompatible(Stamp __otherStamp)
    {
        if (this == __otherStamp)
        {
            return true;
        }
        return __otherStamp instanceof MethodCountersPointerStamp;
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        if (JavaConstant.NULL_POINTER.equals(__c))
        {
            return METHOD_COUNTERS_ALWAYS_NULL;
        }
        else
        {
            return METHOD_COUNTERS_NON_NULL;
        }
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        return null;
    }
}
