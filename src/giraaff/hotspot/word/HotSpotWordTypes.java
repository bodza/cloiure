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

///
// Extends {@link WordTypes} with information about HotSpot metaspace pointer types.
///
// @class HotSpotWordTypes
public final class HotSpotWordTypes extends WordTypes
{
    ///
    // Resolved type for {@link MetaspacePointer}.
    ///
    // @field
    private final ResolvedJavaType ___metaspacePointerType;

    ///
    // Resolved type for {@link KlassPointer}.
    ///
    // @field
    private final ResolvedJavaType ___klassPointerType;

    ///
    // Resolved type for {@link MethodPointer}.
    ///
    // @field
    private final ResolvedJavaType ___methodPointerType;

    ///
    // Resolved type for {@link MethodCountersPointer}.
    ///
    // @field
    private final ResolvedJavaType ___methodCountersPointerType;

    // @cons HotSpotWordTypes
    public HotSpotWordTypes(MetaAccessProvider __metaAccess, JavaKind __wordKind)
    {
        super(__metaAccess, __wordKind);
        this.___metaspacePointerType = __metaAccess.lookupJavaType(MetaspacePointer.class);
        this.___klassPointerType = __metaAccess.lookupJavaType(KlassPointer.class);
        this.___methodPointerType = __metaAccess.lookupJavaType(MethodPointer.class);
        this.___methodCountersPointerType = __metaAccess.lookupJavaType(MethodCountersPointer.class);
    }

    @Override
    public boolean isWord(JavaType __type)
    {
        if (__type instanceof ResolvedJavaType && this.___metaspacePointerType.isAssignableFrom((ResolvedJavaType) __type))
        {
            return true;
        }
        return super.isWord(__type);
    }

    @Override
    public JavaKind asKind(JavaType __type)
    {
        if (this.___klassPointerType.equals(__type) || this.___methodPointerType.equals(__type))
        {
            return getWordKind();
        }
        return super.asKind(__type);
    }

    @Override
    public Stamp getWordStamp(ResolvedJavaType __type)
    {
        if (__type.equals(this.___klassPointerType))
        {
            return KlassPointerStamp.klass();
        }
        else if (__type.equals(this.___methodPointerType))
        {
            return MethodPointerStamp.method();
        }
        else if (__type.equals(this.___methodCountersPointerType))
        {
            return MethodCountersPointerStamp.methodCounters();
        }
        return super.getWordStamp(__type);
    }
}
