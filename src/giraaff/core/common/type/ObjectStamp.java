package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;

// @class ObjectStamp
public class ObjectStamp extends AbstractObjectStamp
{
    // @cons
    public ObjectStamp(ResolvedJavaType __type, boolean __exactType, boolean __nonNull, boolean __alwaysNull)
    {
        super(__type, __exactType, __nonNull, __alwaysNull);
    }

    @Override
    protected ObjectStamp copyWith(ResolvedJavaType __type, boolean __exactType, boolean __nonNull, boolean __alwaysNull)
    {
        return new ObjectStamp(__type, __exactType, __nonNull, __alwaysNull);
    }

    @Override
    public Stamp unrestricted()
    {
        return StampFactory.object();
    }

    @Override
    public boolean isCompatible(Stamp __other)
    {
        if (this == __other)
        {
            return true;
        }
        if (__other instanceof ObjectStamp)
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        if (__constant instanceof JavaConstant)
        {
            return ((JavaConstant) __constant).getJavaKind().isObject();
        }
        return false;
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        return __tool.getObjectKind();
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        try
        {
            return __provider.readObjectConstant(__base, __displacement);
        }
        catch (IllegalArgumentException __e)
        {
            // It's possible that the base and displacement aren't valid together so simply return null.
            return null;
        }
    }
}
