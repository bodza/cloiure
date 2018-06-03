package giraaff.hotspot.nodes.type;

import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotMemoryAccessProvider;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.CompressEncoding;
import giraaff.core.common.type.AbstractObjectStamp;
import giraaff.core.common.type.ObjectStamp;
import giraaff.core.common.type.Stamp;
import giraaff.nodes.CompressionNode.CompressionOp;
import giraaff.nodes.type.NarrowOopStamp;
import giraaff.util.GraalError;

// @class HotSpotNarrowOopStamp
public final class HotSpotNarrowOopStamp extends NarrowOopStamp
{
    // @cons
    private HotSpotNarrowOopStamp(ResolvedJavaType __type, boolean __exactType, boolean __nonNull, boolean __alwaysNull, CompressEncoding __encoding)
    {
        super(__type, __exactType, __nonNull, __alwaysNull, __encoding);
    }

    @Override
    protected AbstractObjectStamp copyWith(ResolvedJavaType __type, boolean __exactType, boolean __nonNull, boolean __alwaysNull)
    {
        return new HotSpotNarrowOopStamp(__type, __exactType, __nonNull, __alwaysNull, getEncoding());
    }

    public static Stamp compressed(AbstractObjectStamp __stamp, CompressEncoding __encoding)
    {
        return new HotSpotNarrowOopStamp(__stamp.type(), __stamp.isExactType(), __stamp.nonNull(), __stamp.alwaysNull(), __encoding);
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        try
        {
            HotSpotMemoryAccessProvider __hsProvider = (HotSpotMemoryAccessProvider) __provider;
            return __hsProvider.readNarrowOopConstant(__base, __displacement);
        }
        catch (IllegalArgumentException __e)
        {
            return null;
        }
    }

    @Override
    public JavaConstant asConstant()
    {
        if (alwaysNull())
        {
            return HotSpotCompressedNullConstant.COMPRESSED_NULL;
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean isCompatible(Constant __other)
    {
        if (__other instanceof HotSpotObjectConstant)
        {
            return ((HotSpotObjectConstant) __other).isCompressed();
        }
        return true;
    }

    public static Stamp mkStamp(CompressionOp __op, Stamp __input, CompressEncoding __encoding)
    {
        switch (__op)
        {
            case Compress:
                if (__input instanceof ObjectStamp)
                {
                    // compressed oop
                    return HotSpotNarrowOopStamp.compressed((ObjectStamp) __input, __encoding);
                }
                else if (__input instanceof KlassPointerStamp)
                {
                    // compressed klass pointer
                    return ((KlassPointerStamp) __input).compressed(__encoding);
                }
                break;
            case Uncompress:
                if (__input instanceof NarrowOopStamp)
                {
                    // oop
                    return ((NarrowOopStamp) __input).uncompressed();
                }
                else if (__input instanceof KlassPointerStamp)
                {
                    // metaspace pointer
                    return ((KlassPointerStamp) __input).uncompressed();
                }
                break;
        }
        throw GraalError.shouldNotReachHere("unexpected input stamp: " + __input);
    }
}
