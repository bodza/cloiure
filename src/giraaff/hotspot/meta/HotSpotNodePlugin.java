package giraaff.hotspot.meta;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.JavaTypeProfile;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampPair;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.GraphBuilderTool;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin;
import giraaff.nodes.graphbuilderconf.NodePlugin;
import giraaff.nodes.graphbuilderconf.TypePlugin;
import giraaff.nodes.util.ConstantFoldUtil;
import giraaff.word.Word;
import giraaff.word.WordOperationPlugin;

/**
 * This plugin handles the HotSpot-specific customizations of bytecode parsing:
 *
 * {@link Word}-type rewriting for {@link GraphBuilderContext#parsingIntrinsic intrinsic} functions
 * (snippets and method substitutions), by forwarding to the {@link WordOperationPlugin}. Note that
 * we forward the {@link NodePlugin} and {@link TypePlugin} methods, but not the
 * {@link InlineInvokePlugin} methods implemented by {@link WordOperationPlugin}. The latter is not
 * necessary because HotSpot only uses the {@link Word} type in methods that are force-inlined,
 * i.e., there are never non-inlined invokes that involve the {@link Word} type.
 *
 * Constant folding of field loads.
 */
public final class HotSpotNodePlugin implements NodePlugin, TypePlugin
{
    protected final WordOperationPlugin wordOperationPlugin;

    public HotSpotNodePlugin(WordOperationPlugin wordOperationPlugin)
    {
        this.wordOperationPlugin = wordOperationPlugin;
    }

    @Override
    public boolean canChangeStackKind(GraphBuilderContext b)
    {
        if (b.parsingIntrinsic())
        {
            return wordOperationPlugin.canChangeStackKind(b);
        }
        return false;
    }

    @Override
    public StampPair interceptType(GraphBuilderTool b, JavaType declaredType, boolean nonNull)
    {
        if (b.parsingIntrinsic())
        {
            return wordOperationPlugin.interceptType(b, declaredType, nonNull);
        }
        return null;
    }

    @Override
    public boolean handleInvoke(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode[] args)
    {
        if (b.parsingIntrinsic() && wordOperationPlugin.handleInvoke(b, method, args))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadField(GraphBuilderContext b, ValueNode object, ResolvedJavaField field)
    {
        if (object.isConstant())
        {
            JavaConstant asJavaConstant = object.asJavaConstant();
            if (tryReadField(b, field, asJavaConstant))
            {
                return true;
            }
        }
        if (b.parsingIntrinsic() && wordOperationPlugin.handleLoadField(b, object, field))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadStaticField(GraphBuilderContext b, ResolvedJavaField field)
    {
        if (tryReadField(b, field, null))
        {
            return true;
        }
        if (b.parsingIntrinsic() && wordOperationPlugin.handleLoadStaticField(b, field))
        {
            return true;
        }
        return false;
    }

    private static boolean tryReadField(GraphBuilderContext b, ResolvedJavaField field, JavaConstant object)
    {
        return tryConstantFold(b, field, object);
    }

    private static boolean tryConstantFold(GraphBuilderContext b, ResolvedJavaField field, JavaConstant object)
    {
        ConstantNode result = ConstantFoldUtil.tryConstantFold(b.getConstantFieldProvider(), b.getConstantReflection(), b.getMetaAccess(), field, object, b.getOptions());
        if (result != null)
        {
            result = b.getGraph().unique(result);
            b.push(field.getJavaKind(), result);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleStoreField(GraphBuilderContext b, ValueNode object, ResolvedJavaField field, ValueNode value)
    {
        if (b.parsingIntrinsic() && wordOperationPlugin.handleStoreField(b, object, field, value))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleStoreStaticField(GraphBuilderContext b, ResolvedJavaField field, ValueNode value)
    {
        if (b.parsingIntrinsic() && wordOperationPlugin.handleStoreStaticField(b, field, value))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleLoadIndexed(GraphBuilderContext b, ValueNode array, ValueNode index, JavaKind elementKind)
    {
        if (b.parsingIntrinsic() && wordOperationPlugin.handleLoadIndexed(b, array, index, elementKind))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleStoreIndexed(GraphBuilderContext b, ValueNode array, ValueNode index, JavaKind elementKind, ValueNode value)
    {
        if (b.parsingIntrinsic() && wordOperationPlugin.handleStoreIndexed(b, array, index, elementKind, value))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleCheckCast(GraphBuilderContext b, ValueNode object, ResolvedJavaType type, JavaTypeProfile profile)
    {
        if (b.parsingIntrinsic() && wordOperationPlugin.handleCheckCast(b, object, type, profile))
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean handleInstanceOf(GraphBuilderContext b, ValueNode object, ResolvedJavaType type, JavaTypeProfile profile)
    {
        if (b.parsingIntrinsic() && wordOperationPlugin.handleInstanceOf(b, object, type, profile))
        {
            return true;
        }
        return false;
    }
}
