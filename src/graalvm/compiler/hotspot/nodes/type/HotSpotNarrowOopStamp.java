package graalvm.compiler.hotspot.nodes.type;

import jdk.vm.ci.hotspot.HotSpotCompressedNullConstant;
import jdk.vm.ci.hotspot.HotSpotMemoryAccessProvider;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import graalvm.compiler.core.common.CompressEncoding;
import graalvm.compiler.core.common.type.AbstractObjectStamp;
import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.nodes.CompressionNode.CompressionOp;
import graalvm.compiler.nodes.type.NarrowOopStamp;

public final class HotSpotNarrowOopStamp extends NarrowOopStamp
{
    private HotSpotNarrowOopStamp(ResolvedJavaType type, boolean exactType, boolean nonNull, boolean alwaysNull, CompressEncoding encoding)
    {
        super(type, exactType, nonNull, alwaysNull, encoding);
    }

    @Override
    protected AbstractObjectStamp copyWith(ResolvedJavaType type, boolean exactType, boolean nonNull, boolean alwaysNull)
    {
        return new HotSpotNarrowOopStamp(type, exactType, nonNull, alwaysNull, getEncoding());
    }

    public static Stamp compressed(AbstractObjectStamp stamp, CompressEncoding encoding)
    {
        return new HotSpotNarrowOopStamp(stamp.type(), stamp.isExactType(), stamp.nonNull(), stamp.alwaysNull(), encoding);
    }

    @Override
    public Constant readConstant(MemoryAccessProvider provider, Constant base, long displacement)
    {
        try
        {
            HotSpotMemoryAccessProvider hsProvider = (HotSpotMemoryAccessProvider) provider;
            return hsProvider.readNarrowOopConstant(base, displacement);
        }
        catch (IllegalArgumentException e)
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
    public boolean isCompatible(Constant other)
    {
        if (other instanceof HotSpotObjectConstant)
        {
            return ((HotSpotObjectConstant) other).isCompressed();
        }
        return true;
    }

    public static Stamp mkStamp(CompressionOp op, Stamp input, CompressEncoding encoding)
    {
        switch (op)
        {
            case Compress:
                if (input instanceof ObjectStamp)
                {
                    // compressed oop
                    return HotSpotNarrowOopStamp.compressed((ObjectStamp) input, encoding);
                }
                else if (input instanceof KlassPointerStamp)
                {
                    // compressed klass pointer
                    return ((KlassPointerStamp) input).compressed(encoding);
                }
                break;
            case Uncompress:
                if (input instanceof NarrowOopStamp)
                {
                    // oop
                    return ((NarrowOopStamp) input).uncompressed();
                }
                else if (input instanceof KlassPointerStamp)
                {
                    // metaspace pointer
                    return ((KlassPointerStamp) input).uncompressed();
                }
                break;
        }
        throw GraalError.shouldNotReachHere(String.format("Unexpected input stamp %s", input));
    }
}
