package graalvm.compiler.hotspot.nodes.type;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.spi.LIRKindTool;
import graalvm.compiler.core.common.type.AbstractPointerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.GraalError;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

public abstract class MetaspacePointerStamp extends AbstractPointerStamp
{
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

    protected void appendString(StringBuilder str)
    {
        str.append(nonNull() ? "!" : "").append(alwaysNull() ? " NULL" : "");
    }
}
