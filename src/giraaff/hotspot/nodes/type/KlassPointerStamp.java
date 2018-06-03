package giraaff.hotspot.nodes.type;

import java.util.Objects;

import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotMemoryAccessProvider;
import jdk.vm.ci.hotspot.HotSpotMetaspaceConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.CompressEncoding;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.Stamp;

// @class KlassPointerStamp
public final class KlassPointerStamp extends MetaspacePointerStamp
{
    // @def
    private static final KlassPointerStamp KLASS = new KlassPointerStamp(false, false);

    // @def
    private static final KlassPointerStamp KLASS_NON_NULL = new KlassPointerStamp(true, false);

    // @def
    private static final KlassPointerStamp KLASS_ALWAYS_NULL = new KlassPointerStamp(false, true);

    // @field
    private final CompressEncoding encoding;

    public static KlassPointerStamp klass()
    {
        return KLASS;
    }

    public static KlassPointerStamp klassNonNull()
    {
        return KLASS_NON_NULL;
    }

    public static KlassPointerStamp klassAlwaysNull()
    {
        return KLASS_ALWAYS_NULL;
    }

    // @cons
    private KlassPointerStamp(boolean __nonNull, boolean __alwaysNull)
    {
        this(__nonNull, __alwaysNull, null);
    }

    // @cons
    private KlassPointerStamp(boolean __nonNull, boolean __alwaysNull, CompressEncoding __encoding)
    {
        super(__nonNull, __alwaysNull);
        this.encoding = __encoding;
    }

    @Override
    protected AbstractPointerStamp copyWith(boolean __newNonNull, boolean __newAlwaysNull)
    {
        return new KlassPointerStamp(__newNonNull, __newAlwaysNull, encoding);
    }

    @Override
    public boolean isCompatible(Stamp __otherStamp)
    {
        if (this == __otherStamp)
        {
            return true;
        }
        if (__otherStamp instanceof KlassPointerStamp)
        {
            KlassPointerStamp __other = (KlassPointerStamp) __otherStamp;
            return Objects.equals(this.encoding, __other.encoding);
        }
        return false;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        if (__constant instanceof HotSpotMetaspaceConstant)
        {
            return ((HotSpotMetaspaceConstant) __constant).asResolvedJavaType() != null;
        }
        else
        {
            return super.isCompatible(__constant);
        }
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        if (isCompressed())
        {
            if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(__c))
            {
                return new KlassPointerStamp(false, true, encoding);
            }
        }
        else
        {
            if (JavaConstant.NULL_POINTER.equals(__c))
            {
                return KLASS_ALWAYS_NULL;
            }
        }

        if (nonNull())
        {
            return this;
        }
        if (isCompressed())
        {
            return new KlassPointerStamp(true, false, encoding);
        }
        else
        {
            return KLASS_NON_NULL;
        }
    }

    @Override
    public Constant asConstant()
    {
        if (alwaysNull() && isCompressed())
        {
            return HotSpotCompressedNullConstant.COMPRESSED_NULL;
        }
        else
        {
            return super.asConstant();
        }
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        if (isCompressed())
        {
            return __tool.getNarrowPointerKind();
        }
        else
        {
            return super.getLIRKind(__tool);
        }
    }

    public boolean isCompressed()
    {
        return encoding != null;
    }

    public CompressEncoding getEncoding()
    {
        return encoding;
    }

    public KlassPointerStamp compressed(CompressEncoding __newEncoding)
    {
        return new KlassPointerStamp(nonNull(), alwaysNull(), __newEncoding);
    }

    public KlassPointerStamp uncompressed()
    {
        return new KlassPointerStamp(nonNull(), alwaysNull());
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        HotSpotMemoryAccessProvider __hsProvider = (HotSpotMemoryAccessProvider) __provider;
        if (isCompressed())
        {
            return __hsProvider.readNarrowKlassPointerConstant(__base, __displacement);
        }
        else
        {
            return __hsProvider.readKlassPointerConstant(__base, __displacement);
        }
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        return __prime * super.hashCode() + ((encoding != null) ? encoding.hashCode() : 0);
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (!super.equals(__obj))
        {
            return false;
        }
        if (!(__obj instanceof KlassPointerStamp))
        {
            return false;
        }
        KlassPointerStamp __other = (KlassPointerStamp) __obj;
        return Objects.equals(this.encoding, __other.encoding);
    }
}
