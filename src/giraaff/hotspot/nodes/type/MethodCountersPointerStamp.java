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
    private MethodCountersPointerStamp(boolean nonNull, boolean alwaysNull)
    {
        super(nonNull, alwaysNull);
    }

    @Override
    protected AbstractPointerStamp copyWith(boolean newNonNull, boolean newAlwaysNull)
    {
        if (newNonNull)
        {
            return METHOD_COUNTERS_NON_NULL;
        }
        else if (newAlwaysNull)
        {
            return METHOD_COUNTERS_ALWAYS_NULL;
        }
        else
        {
            return METHOD_COUNTERS;
        }
    }

    @Override
    public boolean isCompatible(Stamp otherStamp)
    {
        if (this == otherStamp)
        {
            return true;
        }
        return otherStamp instanceof MethodCountersPointerStamp;
    }

    @Override
    public Stamp constant(Constant c, MetaAccessProvider meta)
    {
        if (JavaConstant.NULL_POINTER.equals(c))
        {
            return METHOD_COUNTERS_ALWAYS_NULL;
        }
        else
        {
            return METHOD_COUNTERS_NON_NULL;
        }
    }

    @Override
    public Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement)
    {
        return null;
    }

    @Override
    public String toString()
    {
        StringBuilder ret = new StringBuilder("MethodCounters*");
        appendString(ret);
        return ret.toString();
    }
}
