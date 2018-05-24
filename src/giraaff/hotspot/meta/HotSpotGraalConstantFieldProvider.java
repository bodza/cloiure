package giraaff.hotspot.meta;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.GraalOptions;
import giraaff.graph.NodeClass;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetCounter;
import giraaff.util.GraalError;

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
