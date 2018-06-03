package giraaff.nodes.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.CompressEncoding;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.core.common.type.AbstractObjectStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;

// @class NarrowOopStamp
public abstract class NarrowOopStamp extends AbstractObjectStamp
{
    // @field
    private final CompressEncoding encoding;

    // @cons
    protected NarrowOopStamp(ResolvedJavaType __type, boolean __exactType, boolean __nonNull, boolean __alwaysNull, CompressEncoding __encoding)
    {
        super(__type, __exactType, __nonNull, __alwaysNull);
        this.encoding = __encoding;
    }

    @Override
    protected abstract AbstractObjectStamp copyWith(ResolvedJavaType type, boolean exactType, boolean nonNull, boolean alwaysNull);

    public Stamp uncompressed()
    {
        return new ObjectStamp(type(), isExactType(), nonNull(), alwaysNull());
    }

    public CompressEncoding getEncoding()
    {
        return encoding;
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        return __tool.getNarrowOopKind();
    }

    @Override
    public boolean isCompatible(Stamp __other)
    {
        if (this == __other)
        {
            return true;
        }
        if (__other instanceof NarrowOopStamp)
        {
            NarrowOopStamp __narrow = (NarrowOopStamp) __other;
            return encoding.equals(__narrow.encoding);
        }
        return false;
    }

    @Override
    public abstract Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement);

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        return __prime * super.hashCode() + encoding.hashCode();
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
        NarrowOopStamp __other = (NarrowOopStamp) __obj;
        if (!encoding.equals(__other.encoding))
        {
            return false;
        }
        return super.equals(__other);
    }

    @Override
    public abstract JavaConstant asConstant();

    @Override
    public abstract boolean isCompatible(Constant other);
}
