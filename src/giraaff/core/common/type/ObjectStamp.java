package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;

public class ObjectStamp extends AbstractObjectStamp
{
    public ObjectStamp(ResolvedJavaType type, boolean exactType, boolean nonNull, boolean alwaysNull)
    {
        super(type, exactType, nonNull, alwaysNull);
    }

    @Override
    protected ObjectStamp copyWith(ResolvedJavaType type, boolean exactType, boolean nonNull, boolean alwaysNull)
    {
        return new ObjectStamp(type, exactType, nonNull, alwaysNull);
    }

    @Override
    public Stamp unrestricted()
    {
        return StampFactory.object();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('a');
        appendString(sb);
        return sb.toString();
    }

    @Override
    public boolean isCompatible(Stamp other)
    {
        if (this == other)
        {
            return true;
        }
        if (other instanceof ObjectStamp)
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isCompatible(Constant constant)
    {
        if (constant instanceof JavaConstant)
        {
            return ((JavaConstant) constant).getJavaKind().isObject();
        }
        return false;
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool tool)
    {
        return tool.getObjectKind();
    }

    @Override
    public Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement)
    {
        try
        {
            return provider.readObjectConstant(base, displacement);
        }
        catch (IllegalArgumentException e)
        {
            // It's possible that the base and displacement aren't valid together so simply return null.
            return null;
        }
    }
}
