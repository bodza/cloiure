package giraaff.core.common.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.util.GraalError;

/**
 * Type describing pointers to raw memory. This stamp is used for example for direct pointers to
 * fields or array elements.
 */
// @class RawPointerStamp
public final class RawPointerStamp extends AbstractPointerStamp
{
    // @cons
    protected RawPointerStamp()
    {
        super(false, false);
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        return __tool.getWordKind();
    }

    @Override
    protected AbstractPointerStamp copyWith(boolean __newNonNull, boolean __newAlwaysNull)
    {
        // RawPointerStamp is a singleton
        return this;
    }

    @Override
    public Stamp meet(Stamp __other)
    {
        return this;
    }

    @Override
    public Stamp improveWith(Stamp __other)
    {
        return this;
    }

    @Override
    public Stamp join(Stamp __other)
    {
        return this;
    }

    @Override
    public Stamp unrestricted()
    {
        return this;
    }

    @Override
    public Stamp empty()
    {
        // there is no empty pointer stamp
        return this;
    }

    @Override
    public boolean hasValues()
    {
        return true;
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider __metaAccess)
    {
        throw GraalError.shouldNotReachHere("pointer has no Java type");
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        return this;
    }

    @Override
    public boolean isCompatible(Stamp __other)
    {
        return __other instanceof RawPointerStamp;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        if (__constant instanceof PrimitiveConstant)
        {
            return ((PrimitiveConstant) __constant).getJavaKind().isNumericInteger();
        }
        else
        {
            return __constant instanceof DataPointerConstant;
        }
    }

    @Override
    public Constant readConstant(MemoryAccessProvider __provider, Constant __base, long __displacement)
    {
        throw GraalError.shouldNotReachHere("can't read raw pointer");
    }
}
