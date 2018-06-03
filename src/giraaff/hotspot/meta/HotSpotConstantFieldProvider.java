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
    public HotSpotConstantFieldProvider(MetaAccessProvider __metaAccess)
    {
        super(__metaAccess);
    }

    @Override
    protected boolean isStableField(ResolvedJavaField __field, ConstantFieldTool<?> __tool)
    {
        if (!HotSpotRuntime.foldStableValues)
        {
            return false;
        }
        if (__field.isStatic() && !isStaticFieldConstant(__field))
        {
            return false;
        }

        if (((HotSpotResolvedJavaField) __field).isStable())
        {
            return true;
        }
        return super.isStableField(__field, __tool);
    }

    @Override
    protected boolean isFinalField(ResolvedJavaField __field, ConstantFieldTool<?> __tool)
    {
        if (__field.isStatic() && !isStaticFieldConstant(__field))
        {
            return false;
        }

        return super.isFinalField(__field, __tool);
    }

    protected boolean isStaticFieldConstant(ResolvedJavaField __field)
    {
        ResolvedJavaType __declaringClass = __field.getDeclaringClass();
        return __declaringClass.isInitialized() && !__declaringClass.getName().equals("Ljava/lang/System;");
    }
}
