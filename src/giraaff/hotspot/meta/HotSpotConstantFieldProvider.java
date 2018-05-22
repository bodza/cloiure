package giraaff.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaField;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.spi.JavaConstantFieldProvider;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.options.OptionValues;

/**
 * Implements the default constant folding semantics for Java fields in the HotSpot VM.
 */
public class HotSpotConstantFieldProvider extends JavaConstantFieldProvider
{
    private final GraalHotSpotVMConfig config;

    public HotSpotConstantFieldProvider(GraalHotSpotVMConfig config, MetaAccessProvider metaAccess)
    {
        super(metaAccess);
        this.config = config;
    }

    @Override
    protected boolean isStableField(ResolvedJavaField field, ConstantFieldTool<?> tool)
    {
        if (!config.foldStableValues)
        {
            return false;
        }
        if (field.isStatic() && !isStaticFieldConstant(field, tool.getOptions()))
        {
            return false;
        }

        if (((HotSpotResolvedJavaField) field).isStable())
        {
            return true;
        }
        return super.isStableField(field, tool);
    }

    @Override
    protected boolean isFinalField(ResolvedJavaField field, ConstantFieldTool<?> tool)
    {
        if (field.isStatic() && !isStaticFieldConstant(field, tool.getOptions()))
        {
            return false;
        }

        return super.isFinalField(field, tool);
    }

    private static final String SystemClassName = "Ljava/lang/System;";

    protected boolean isStaticFieldConstant(ResolvedJavaField field, @SuppressWarnings("unused") OptionValues options)
    {
        ResolvedJavaType declaringClass = field.getDeclaringClass();
        return declaringClass.isInitialized() && !declaringClass.getName().equals(SystemClassName);
    }
}
