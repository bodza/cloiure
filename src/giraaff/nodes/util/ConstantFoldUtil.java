package giraaff.nodes.util;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.spi.ConstantFieldProvider.ConstantFieldTool;
import giraaff.nodes.ConstantNode;

// @class ConstantFoldUtil
public final class ConstantFoldUtil
{
    // @cons
    private ConstantFoldUtil()
    {
        super();
    }

    public static ConstantNode tryConstantFold(ConstantFieldProvider __fieldProvider, ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, ResolvedJavaField __field, JavaConstant __receiver)
    {
        if (!__field.isStatic())
        {
            if (__receiver == null || __receiver.isNull())
            {
                return null;
            }
        }

        // @closure
        return __fieldProvider.readConstantField(__field, new ConstantFieldTool<ConstantNode>()
        {
            @Override
            public JavaConstant readValue()
            {
                return __constantReflection.readFieldValue(__field, __receiver);
            }

            @Override
            public JavaConstant getReceiver()
            {
                return __receiver;
            }

            @Override
            public ConstantNode foldConstant(JavaConstant __ret)
            {
                if (__ret != null)
                {
                    return ConstantNode.forConstant(__ret, __metaAccess);
                }
                else
                {
                    return null;
                }
            }

            @Override
            public ConstantNode foldStableArray(JavaConstant __ret, int __stableDimensions, boolean __isDefaultStable)
            {
                if (__ret != null)
                {
                    return ConstantNode.forConstant(__ret, __stableDimensions, __isDefaultStable, __metaAccess);
                }
                else
                {
                    return null;
                }
            }
        });
    }
}
