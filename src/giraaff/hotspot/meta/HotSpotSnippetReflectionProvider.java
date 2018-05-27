package giraaff.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.hotspot.HotSpotObjectConstant;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaType;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.hotspot.GraalHotSpotVMConfig;
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

    // lazily initialized
    private Class<?> wordTypesType;
    private Class<?> runtimeType;
    private Class<?> configType;

    @Override
    public <T> T getInjectedNodeIntrinsicParameter(Class<T> type)
    {
        // Need to test all fields since there is no guarantee under the JMM
        // about the order in which these fields are written.
        GraalHotSpotVMConfig config = runtime.getVMConfig();
        if (configType == null || wordTypesType == null || runtimeType == null)
        {
            wordTypesType = wordTypes.getClass();
            runtimeType = runtime.getClass();
            configType = config.getClass();
        }

        if (type.isAssignableFrom(wordTypesType))
        {
            return type.cast(wordTypes);
        }
        if (type.isAssignableFrom(runtimeType))
        {
            return type.cast(runtime);
        }
        if (type.isAssignableFrom(configType))
        {
            return type.cast(config);
        }
        return null;
    }

    @Override
    public Class<?> originalClass(ResolvedJavaType type)
    {
        return ((HotSpotResolvedJavaType) type).mirror();
    }
}
