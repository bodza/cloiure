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
    protected MetaspacePointerStamp(boolean nonNull, boolean alwaysNull)
    {
        super(nonNull, alwaysNull);
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool tool)
    {
        return tool.getWordKind();
    }

    @Override
    public Stamp empty()
    {
        // there is no empty pointer stamp
        return this;
    }

    @Override
    public boolean isCompatible(Constant constant)
    {
        return constant.isDefaultForKind();
    }

    @Override
    public boolean hasValues()
    {
        return true;
    }

    @Override
    public Stamp join(Stamp other)
    {
        return defaultPointerJoin(other);
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider metaAccess)
    {
        throw GraalError.shouldNotReachHere("metaspace pointer has no Java type");
    }
}
