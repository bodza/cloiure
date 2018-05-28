package giraaff.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.word.WordTypes;

public class HotSpotSnippetReflectionProvider implements SnippetReflectionProvider
{
    private final HotSpotGraalRuntimeProvider runtime;
    private final HotSpotConstantReflectionProvider constantReflection;
    private final WordTypes wordTypes;

    public HotSpotSnippetReflectionProvider(HotSpotGraalRuntimeProvider runtime, HotSpotConstantReflectionProvider constantReflection, WordTypes wordTypes)
    {
        this.runtime = runtime;
        this.constantReflection = constantReflection;
        this.wordTypes = wordTypes;
    }

    @Override
    public JavaConstant forObject(Object object)
    {
        return constantReflection.forObject(object);
    }

    @Override
    public Object asObject(ResolvedJavaType type, JavaConstant constant)
    {
        if (constant.isNull())
        {
            return null;
        }
        HotSpotObjectConstant hsConstant = (HotSpotObjectConstant) constant;
        return hsConstant.asObject(type);
    }

    @Override
    public <T> T asObject(Class<T> type, JavaConstant constant)
    {
        if (constant.isNull())
        {
            return null;
        }
        HotSpotObjectConstant hsConstant = (HotSpotObjectConstant) constant;
        return hsConstant.asObject(type);
    }

    @Override
    public JavaConstant forBoxed(JavaKind kind, Object value)
    {
        if (kind == JavaKind.Object)
        {
            return forObject(value);
        }
        else
        {
            return JavaConstant.forBoxedPrimitive(value);
        }
    }
}
