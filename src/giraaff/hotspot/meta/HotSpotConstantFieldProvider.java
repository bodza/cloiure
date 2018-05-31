package giraaff.hotspot.meta;

import jdk.vm.ci.hotspot.HotSpotResolvedJavaField;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.spi.JavaConstantFieldProvider;
import giraaff.hotspot.HotSpotRuntime;

/**
 * Implements the default constant folding semantics for Java fields in the HotSpot VM.
 */
// @class HotSpotConstantFieldProvider
public class HotSpotConstantFieldProvider extends JavaConstantFieldProvider
{
    // @cons
    public HotSpotConstantFieldProvider(MetaAccessProvider metaAccess)
    {
        super(metaAccess);
    }

    @Override
    protected boolean isStableField(ResolvedJavaField field, ConstantFieldTool<?> tool)
    {
        if (!HotSpotRuntime.foldStableValues)
        {
            return false;
        }
        if (field.isStatic() && !isStaticFieldConstant(field))
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
        if (field.isStatic() && !isStaticFieldConstant(field))
        {
            return false;
        }

        return super.isFinalField(field, tool);
    }

    protected boolean isStaticFieldConstant(ResolvedJavaField field)
    {
        ResolvedJavaType declaringClass = field.getDeclaringClass();
        return declaringClass.isInitialized() && !declaringClass.getName().equals("Ljava/lang/System;");
    }
}
