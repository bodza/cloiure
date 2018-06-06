package giraaff.hotspot.meta;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.graph.NodeClass;
import giraaff.hotspot.HotSpotRuntime;

///
// Extends {@link HotSpotConstantFieldProvider} to override the implementation of
// {@link #readConstantField} with Graal specific semantics.
///
// @class HotSpotGraalConstantFieldProvider
public final class HotSpotGraalConstantFieldProvider extends HotSpotConstantFieldProvider
{
    // @cons HotSpotGraalConstantFieldProvider
    public HotSpotGraalConstantFieldProvider(MetaAccessProvider __metaAccess)
    {
        super(__metaAccess);
        this.___metaAccess = __metaAccess;
    }

    @Override
    protected boolean isFinalFieldValueConstant(ResolvedJavaField __field, JavaConstant __value, ConstantFieldProvider.ConstantFieldTool<?> __tool)
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
    protected boolean isStableFieldValueConstant(ResolvedJavaField __field, JavaConstant __value, ConstantFieldProvider.ConstantFieldTool<?> __tool)
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
    private final MetaAccessProvider ___metaAccess;

    // @field
    private ResolvedJavaType ___cachedHotSpotVMConfigType;
    // @field
    private ResolvedJavaType ___cachedNodeClassType;

    private ResolvedJavaType getHotSpotVMConfigType()
    {
        if (this.___cachedHotSpotVMConfigType == null)
        {
            this.___cachedHotSpotVMConfigType = this.___metaAccess.lookupJavaType(HotSpotRuntime.class);
        }
        return this.___cachedHotSpotVMConfigType;
    }

    private ResolvedJavaType getNodeClassType()
    {
        if (this.___cachedNodeClassType == null)
        {
            this.___cachedNodeClassType = this.___metaAccess.lookupJavaType(NodeClass.class);
        }
        return this.___cachedNodeClassType;
    }
}
