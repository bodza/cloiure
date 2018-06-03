package giraaff.hotspot.meta;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotRuntime;

/**
 * Extends {@link HotSpotConstantFieldProvider} to override the implementation of
 * {@link #readConstantField} with Graal specific semantics.
 */
// @class HotSpotGraalConstantFieldProvider
public final class HotSpotGraalConstantFieldProvider extends HotSpotConstantFieldProvider
{
    // @cons
    public HotSpotGraalConstantFieldProvider(MetaAccessProvider __metaAccess)
    {
        super(__metaAccess);
        this.metaAccess = __metaAccess;
    }

    @Override
    protected boolean isFinalFieldValueConstant(ResolvedJavaField __field, JavaConstant __value, ConstantFieldTool<?> __tool)
    {
        if (super.isFinalFieldValueConstant(__field, __value, __tool))
        {
            return true;
        }

        if (!__field.isStatic())
        {
            JavaConstant __receiver = __tool.getReceiver();
            if (getNodeClassType().isInstance(__receiver))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean isStableFieldValueConstant(ResolvedJavaField __field, JavaConstant __value, ConstantFieldTool<?> __tool)
    {
        if (super.isStableFieldValueConstant(__field, __value, __tool))
        {
            return true;
        }

        if (!__field.isStatic())
        {
            JavaConstant __receiver = __tool.getReceiver();
            if (getHotSpotVMConfigType().isInstance(__receiver))
            {
                return true;
            }
        }

        return false;
    }

    // @field
    private final MetaAccessProvider metaAccess;

    // @field
    private ResolvedJavaType cachedHotSpotVMConfigType;
    // @field
    private ResolvedJavaType cachedNodeClassType;

    private ResolvedJavaType getHotSpotVMConfigType()
    {
        if (cachedHotSpotVMConfigType == null)
        {
            cachedHotSpotVMConfigType = metaAccess.lookupJavaType(HotSpotRuntime.class);
        }
        return cachedHotSpotVMConfigType;
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
