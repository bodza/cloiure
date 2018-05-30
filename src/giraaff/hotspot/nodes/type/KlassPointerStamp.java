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
    private static final KlassPointerStamp KLASS = new KlassPointerStamp(false, false);

    private static final KlassPointerStamp KLASS_NON_NULL = new KlassPointerStamp(true, false);

    private static final KlassPointerStamp KLASS_ALWAYS_NULL = new KlassPointerStamp(false, true);

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
    private KlassPointerStamp(boolean nonNull, boolean alwaysNull)
    {
        this(nonNull, alwaysNull, null);
    }

    // @cons
    private KlassPointerStamp(boolean nonNull, boolean alwaysNull, CompressEncoding encoding)
    {
        super(nonNull, alwaysNull);
        this.encoding = encoding;
    }

    @Override
    protected AbstractPointerStamp copyWith(boolean newNonNull, boolean newAlwaysNull)
    {
        return new KlassPointerStamp(newNonNull, newAlwaysNull, encoding);
    }

    @Override
    public boolean isCompatible(Stamp otherStamp)
    {
        if (this == otherStamp)
        {
            return true;
        }
        if (otherStamp instanceof KlassPointerStamp)
        {
            KlassPointerStamp other = (KlassPointerStamp) otherStamp;
            return Objects.equals(this.encoding, other.encoding);
        }
        return false;
    }

    @Override
    public boolean isCompatible(Constant constant)
    {
        if (constant instanceof HotSpotMetaspaceConstant)
        {
            return ((HotSpotMetaspaceConstant) constant).asResolvedJavaType() != null;
        }
        else
        {
            return super.isCompatible(constant);
        }
    }

    @Override
    public Stamp constant(Constant c, MetaAccessProvider meta)
    {
        if (isCompressed())
        {
            if (HotSpotCompressedNullConstant.COMPRESSED_NULL.equals(c))
            {
                return new KlassPointerStamp(false, true, encoding);
            }
        }
        else
        {
            if (JavaConstant.NULL_POINTER.equals(c))
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
    public LIRKind getLIRKind(LIRKindTool tool)
    {
        if (isCompressed())
        {
            return tool.getNarrowPointerKind();
        }
        else
        {
            return super.getLIRKind(tool);
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

    public KlassPointerStamp compressed(CompressEncoding newEncoding)
    {
        return new KlassPointerStamp(nonNull(), alwaysNull(), newEncoding);
    }

    public KlassPointerStamp uncompressed()
    {
        return new KlassPointerStamp(nonNull(), alwaysNull());
    }

    @Override
    public Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement)
    {
        HotSpotMemoryAccessProvider hsProvider = (HotSpotMemoryAccessProvider) provider;
        if (isCompressed())
        {
            return hsProvider.readNarrowKlassPointerConstant(base, displacement);
        }
        else
        {
            return hsProvider.readKlassPointerConstant(base, displacement);
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        return prime * super.hashCode() + ((encoding != null) ? encoding.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!super.equals(obj))
        {
            return false;
        }
        if (!(obj instanceof KlassPointerStamp))
        {
            return false;
        }
        KlassPointerStamp other = (KlassPointerStamp) obj;
        return Objects.equals(this.encoding, other.encoding);
    }
}
