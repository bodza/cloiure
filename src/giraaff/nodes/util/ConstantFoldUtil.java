package giraaff.nodes.util;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.spi.ConstantFieldProvider.ConstantFieldTool;
import giraaff.nodes.ConstantNode;
import giraaff.options.OptionValues;

// @class ConstantFoldUtil
public final class ConstantFoldUtil
{
    public static ConstantNode tryConstantFold(ConstantFieldProvider fieldProvider, ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, ResolvedJavaField field, JavaConstant receiver, OptionValues options)
    {
        if (!field.isStatic())
        {
            if (receiver == null || receiver.isNull())
            {
                return null;
            }
        }

        return fieldProvider.readConstantField(field, new ConstantFieldTool<ConstantNode>()
        {
            @Override
            public JavaConstant readValue()
            {
                return constantReflection.readFieldValue(field, receiver);
            }

            @Override
            public JavaConstant getReceiver()
            {
                return receiver;
            }

            @Override
            public ConstantNode foldConstant(JavaConstant ret)
            {
                if (ret != null)
                {
                    return ConstantNode.forConstant(ret, metaAccess);
                }
                else
                {
                    return null;
                }
            }

            @Override
            public ConstantNode foldStableArray(JavaConstant ret, int stableDimensions, boolean isDefaultStable)
            {
                if (ret != null)
                {
                    return ConstantNode.forConstant(ret, stableDimensions, isDefaultStable, metaAccess);
                }
                else
                {
                    return null;
                }
            }

            @Override
            public OptionValues getOptions()
            {
                return options;
            }
        });
    }

    // @cons
    private ConstantFoldUtil()
    {
        super();
    }
}
