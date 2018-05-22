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

public abstract class NarrowOopStamp extends AbstractObjectStamp
{
    private final CompressEncoding encoding;

    protected NarrowOopStamp(ResolvedJavaType type, boolean exactType, boolean nonNull, boolean alwaysNull, CompressEncoding encoding)
    {
        super(type, exactType, nonNull, alwaysNull);
        this.encoding = encoding;
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
    public LIRKind getLIRKind(LIRKindTool tool)
    {
        return tool.getNarrowOopKind();
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append('n');
        appendString(str);
        return str.toString();
    }

    @Override
    public boolean isCompatible(Stamp other)
    {
        if (this == other)
        {
            return true;
        }
        if (other instanceof NarrowOopStamp)
        {
            NarrowOopStamp narrow = (NarrowOopStamp) other;
            return encoding.equals(narrow.encoding);
        }
        return false;
    }

    @Override
    public abstract Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement);

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + encoding.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }
        NarrowOopStamp other = (NarrowOopStamp) obj;
        if (!encoding.equals(other.encoding))
        {
            return false;
        }
        return super.equals(other);
    }

    @Override
    public abstract JavaConstant asConstant();

    @Override
    public abstract boolean isCompatible(Constant other);
}
