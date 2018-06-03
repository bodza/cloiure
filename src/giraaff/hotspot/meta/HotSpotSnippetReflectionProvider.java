package giraaff.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.hotspot.HotSpotGraalRuntime;
import giraaff.word.WordTypes;

// @class HotSpotSnippetReflectionProvider
public final class HotSpotSnippetReflectionProvider implements SnippetReflectionProvider
{
    // @field
    private final HotSpotGraalRuntime runtime;
    // @field
    private final HotSpotConstantReflectionProvider constantReflection;
    // @field
    private final WordTypes wordTypes;

    // @cons
    public HotSpotSnippetReflectionProvider(HotSpotGraalRuntime __runtime, HotSpotConstantReflectionProvider __constantReflection, WordTypes __wordTypes)
    {
        super();
        this.runtime = __runtime;
        this.constantReflection = __constantReflection;
        this.wordTypes = __wordTypes;
    }

    @Override
    public JavaConstant forObject(Object __object)
    {
        return constantReflection.forObject(__object);
    }

    @Override
    public Object asObject(ResolvedJavaType __type, JavaConstant __constant)
    {
        if (__constant.isNull())
        {
            return null;
        }
        HotSpotObjectConstant __hsConstant = (HotSpotObjectConstant) __constant;
        return __hsConstant.asObject(__type);
    }

    @Override
    public <T> T asObject(Class<T> __type, JavaConstant __constant)
    {
        if (__constant.isNull())
        {
            return null;
        }
        HotSpotObjectConstant __hsConstant = (HotSpotObjectConstant) __constant;
        return __hsConstant.asObject(__type);
    }

    @Override
    public JavaConstant forBoxed(JavaKind __kind, Object __value)
    {
        if (__kind == JavaKind.Object)
        {
            return forObject(__value);
        }
        else
        {
            return JavaConstant.forBoxedPrimitive(__value);
        }
    }
}
