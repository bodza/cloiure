package giraaff.hotspot.nodes.type;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.util.GraalError;

// @class MetaspacePointerStamp
public abstract class MetaspacePointerStamp extends AbstractPointerStamp
{
    // @cons
    protected MetaspacePointerStamp(boolean __nonNull, boolean __alwaysNull)
    {
        super(__nonNull, __alwaysNull);
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        return __tool.getWordKind();
    }

    @Override
    public Stamp empty()
    {
        // there is no empty pointer stamp
        return this;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        return __constant.isDefaultForKind();
    }

    @Override
    public boolean hasValues()
    {
        return true;
    }

    @Override
    public Stamp join(Stamp __other)
    {
        return defaultPointerJoin(__other);
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider __metaAccess)
    {
        throw GraalError.shouldNotReachHere("metaspace pointer has no Java type");
    }
}
