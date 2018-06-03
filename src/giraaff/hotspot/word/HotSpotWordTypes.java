package giraaff.hotspot.word;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.Stamp;
import giraaff.hotspot.nodes.type.KlassPointerStamp;
import giraaff.hotspot.nodes.type.MethodCountersPointerStamp;
import giraaff.hotspot.nodes.type.MethodPointerStamp;
import giraaff.word.WordTypes;

/**
 * Extends {@link WordTypes} with information about HotSpot metaspace pointer types.
 */
// @class HotSpotWordTypes
public final class HotSpotWordTypes extends WordTypes
{
    /**
     * Resolved type for {@link MetaspacePointer}.
     */
    // @field
    private final ResolvedJavaType metaspacePointerType;

    /**
     * Resolved type for {@link KlassPointer}.
     */
    // @field
    private final ResolvedJavaType klassPointerType;

    /**
     * Resolved type for {@link MethodPointer}.
     */
    // @field
    private final ResolvedJavaType methodPointerType;

    /**
     * Resolved type for {@link MethodCountersPointer}.
     */
    // @field
    private final ResolvedJavaType methodCountersPointerType;

    // @cons
    public HotSpotWordTypes(MetaAccessProvider __metaAccess, JavaKind __wordKind)
    {
        super(__metaAccess, __wordKind);
        this.metaspacePointerType = __metaAccess.lookupJavaType(MetaspacePointer.class);
        this.klassPointerType = __metaAccess.lookupJavaType(KlassPointer.class);
        this.methodPointerType = __metaAccess.lookupJavaType(MethodPointer.class);
        this.methodCountersPointerType = __metaAccess.lookupJavaType(MethodCountersPointer.class);
    }

    @Override
    public boolean isWord(JavaType __type)
    {
        if (__type instanceof ResolvedJavaType && metaspacePointerType.isAssignableFrom((ResolvedJavaType) __type))
        {
            return true;
        }
        return super.isWord(__type);
    }

    @Override
    public JavaKind asKind(JavaType __type)
    {
        if (klassPointerType.equals(__type) || methodPointerType.equals(__type))
        {
            return getWordKind();
        }
        return super.asKind(__type);
    }

    @Override
    public Stamp getWordStamp(ResolvedJavaType __type)
    {
        if (__type.equals(klassPointerType))
        {
            return KlassPointerStamp.klass();
        }
        else if (__type.equals(methodPointerType))
        {
            return MethodPointerStamp.method();
        }
        else if (__type.equals(methodCountersPointerType))
        {
            return MethodCountersPointerStamp.methodCounters();
        }
        return super.getWordStamp(__type);
    }
}
