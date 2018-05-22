package giraaff.hotspot.meta;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.GraalOptions;
import giraaff.debug.GraalError;
import giraaff.graph.NodeClass;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetCounter;

/**
 * Extends {@link HotSpotConstantFieldProvider} to override the implementation of
 * {@link #readConstantField} with Graal specific semantics.
 */
public class HotSpotGraalConstantFieldProvider extends HotSpotConstantFieldProvider
{
    public HotSpotGraalConstantFieldProvider(GraalHotSpotVMConfig config, MetaAccessProvider metaAccess)
    {
        super(config, metaAccess);
        this.metaAccess = metaAccess;
    }

    @Override
    protected boolean isStaticFieldConstant(ResolvedJavaField field, OptionValues options)
    {
        return super.isStaticFieldConstant(field, options) && (!GraalOptions.ImmutableCode.getValue(options) || isEmbeddableField(field));
    }

    /**
     * The set of fields whose values cannot be constant folded in ImmutableCode mode. This is
     * volatile to support double-checked locking lazy initialization.
     */
    private volatile List<ResolvedJavaField> nonEmbeddableFields;

    protected boolean isEmbeddableField(ResolvedJavaField field)
    {
        if (nonEmbeddableFields == null)
        {
            synchronized (this)
            {
                if (nonEmbeddableFields == null)
                {
                    List<ResolvedJavaField> fields = new ArrayList<>();
                    try
                    {
                        fields.add(metaAccess.lookupJavaField(Boolean.class.getDeclaredField("TRUE")));
                        fields.add(metaAccess.lookupJavaField(Boolean.class.getDeclaredField("FALSE")));

                        Class<?> characterCacheClass = Character.class.getDeclaredClasses()[0];
                        fields.add(metaAccess.lookupJavaField(characterCacheClass.getDeclaredField("cache")));

                        Class<?> byteCacheClass = Byte.class.getDeclaredClasses()[0];
                        fields.add(metaAccess.lookupJavaField(byteCacheClass.getDeclaredField("cache")));

                        Class<?> shortCacheClass = Short.class.getDeclaredClasses()[0];
                        fields.add(metaAccess.lookupJavaField(shortCacheClass.getDeclaredField("cache")));

                        Class<?> integerCacheClass = Integer.class.getDeclaredClasses()[0];
                        fields.add(metaAccess.lookupJavaField(integerCacheClass.getDeclaredField("cache")));

                        Class<?> longCacheClass = Long.class.getDeclaredClasses()[0];
                        fields.add(metaAccess.lookupJavaField(longCacheClass.getDeclaredField("cache")));

                        fields.add(metaAccess.lookupJavaField(Throwable.class.getDeclaredField("UNASSIGNED_STACK")));
                        fields.add(metaAccess.lookupJavaField(Throwable.class.getDeclaredField("SUPPRESSED_SENTINEL")));
                    }
                    catch (SecurityException | NoSuchFieldException e)
                    {
                        throw new GraalError(e);
                    }
                    nonEmbeddableFields = fields;
                }
            }
        }
        return !nonEmbeddableFields.contains(field);
    }

    @Override
    protected boolean isFinalFieldValueConstant(ResolvedJavaField field, JavaConstant value, ConstantFieldTool<?> tool)
    {
        if (super.isFinalFieldValueConstant(field, value, tool))
        {
            return true;
        }

        if (!field.isStatic())
        {
            JavaConstant receiver = tool.getReceiver();
            if (getSnippetCounterType().isInstance(receiver) || getNodeClassType().isInstance(receiver))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean isStableFieldValueConstant(ResolvedJavaField field, JavaConstant value, ConstantFieldTool<?> tool)
    {
        if (super.isStableFieldValueConstant(field, value, tool))
        {
            return true;
        }

        if (!field.isStatic())
        {
            JavaConstant receiver = tool.getReceiver();
            if (getHotSpotVMConfigType().isInstance(receiver))
            {
                return true;
            }
        }

        return false;
    }

    private final MetaAccessProvider metaAccess;

    private ResolvedJavaType cachedHotSpotVMConfigType;
    private ResolvedJavaType cachedSnippetCounterType;
    private ResolvedJavaType cachedNodeClassType;

    private ResolvedJavaType getHotSpotVMConfigType()
    {
        if (cachedHotSpotVMConfigType == null)
        {
            cachedHotSpotVMConfigType = metaAccess.lookupJavaType(GraalHotSpotVMConfig.class);
        }
        return cachedHotSpotVMConfigType;
    }

    private ResolvedJavaType getSnippetCounterType()
    {
        if (cachedSnippetCounterType == null)
        {
            cachedSnippetCounterType = metaAccess.lookupJavaType(SnippetCounter.class);
        }
        return cachedSnippetCounterType;
    }

    private ResolvedJavaType getNodeClassType()
    {
        if (cachedNodeClassType == null)
        {
            cachedNodeClassType = metaAccess.lookupJavaType(NodeClass.class);
        }
        return cachedNodeClassType;
    }
}
